import XCTest

@testable import MediaPickerCore

final class TempFileStoreTests: XCTestCase {
  private var parent: URL!

  override func setUpWithError() throws {
    parent = FileManager.default.temporaryDirectory
      .appendingPathComponent("TempFileStoreTests-\(UUID().uuidString)")
    try FileManager.default.createDirectory(at: parent, withIntermediateDirectories: true)
  }

  override func tearDownWithError() throws {
    try? FileManager.default.removeItem(at: parent)
  }

  func testFilesLandInADedicatedSubdirectory() throws {
    let store = TempFileStore(parent: parent)
    let url = try store.makeFileURL(fileExtension: "jpg")
    XCTAssertEqual(url.deletingLastPathComponent().lastPathComponent, "rn-media-picker")
    XCTAssertTrue(url.lastPathComponent.hasPrefix("media_picker_"))
    XCTAssertEqual(url.pathExtension, "jpg")
  }

  func testNamesAreUnique() throws {
    let store = TempFileStore(parent: parent)
    let names = try (0..<50).map { _ in
      try store.makeFileURL(fileExtension: "png").lastPathComponent
    }
    XCTAssertEqual(Set(names).count, 50)
  }

  func testRemoveAllDeletesEveryFileItProduced() throws {
    let store = TempFileStore(parent: parent)
    for _ in 0..<3 {
      let url = try store.makeFileURL(fileExtension: "jpg")
      try Data([0x01]).write(to: url)
    }
    XCTAssertEqual(store.removeAll(), 3)
    XCTAssertEqual(store.removeAll(), 0)
  }

  func testRemoveAllKeepsTheDirectoryItself() throws {
    let store = TempFileStore(parent: parent)
    let url = try store.makeFileURL(fileExtension: "jpg")
    try Data([0x01]).write(to: url)

    XCTAssertEqual(store.removeAll(), 1)

    let root = parent.appendingPathComponent(TempFileStore.directoryName, isDirectory: true)
    var isDirectory: ObjCBool = false
    XCTAssertTrue(FileManager.default.fileExists(atPath: root.path, isDirectory: &isDirectory))
    XCTAssertTrue(isDirectory.boolValue)
  }

  func testRemoveAllOnAMissingDirectoryIsANoOp() {
    let store = TempFileStore(parent: parent)
    XCTAssertEqual(store.removeAll(), 0)
  }

  func testRemoveByURIDeletesOnlyTheNamedFiles() throws {
    let store = TempFileStore(parent: parent)
    let released = try store.makeFileURL(fileExtension: "mp4")
    try Data([0x01]).write(to: released)
    let kept = try store.makeFileURL(fileExtension: "jpg")
    try Data([0x01]).write(to: kept)

    XCTAssertEqual(store.remove(uris: [released.absoluteString]), 1)
    XCTAssertFalse(FileManager.default.fileExists(atPath: released.path))
    XCTAssertTrue(FileManager.default.fileExists(atPath: kept.path))
  }

  func testRemoveByURIIgnoresFilesOutsideThePickerDirectory() throws {
    let store = TempFileStore(parent: parent)
    let outsider = parent.appendingPathComponent("not-ours.jpg")
    try Data([0x01]).write(to: outsider)

    XCTAssertEqual(store.remove(uris: [outsider.absoluteString]), 0)
    XCTAssertTrue(FileManager.default.fileExists(atPath: outsider.path))
  }

  func testRemoveByURIIgnoresTraversalAndNonFileURIs() throws {
    let store = TempFileStore(parent: parent)
    let kept = try store.makeFileURL(fileExtension: "jpg")
    try Data([0x01]).write(to: kept)

    let outsider = parent.appendingPathComponent("not-ours.jpg")
    try Data([0x01]).write(to: outsider)

    let traversal = "file://\(parent.path)/rn-media-picker/../not-ours.jpg"
    XCTAssertEqual(
      store.remove(uris: [traversal, "https://example.com/a.jpg", "", "not a uri at all"]),
      0
    )
    XCTAssertTrue(FileManager.default.fileExists(atPath: outsider.path))
    XCTAssertTrue(FileManager.default.fileExists(atPath: kept.path))
  }

  func testRemoveByURIWithNoUsableEntriesIsANoOp() {
    let store = TempFileStore(parent: parent)
    XCTAssertEqual(store.remove(uris: []), 0)
  }

  func testFileNameForURIDecodesPercentEscapes() {
    XCTAssertEqual(
      TempFileStore.fileName(forURI: "file:///tmp/media%20picker.jpg"),
      "media picker.jpg"
    )
    XCTAssertNil(TempFileStore.fileName(forURI: "file:///"))
    XCTAssertNil(TempFileStore.fileName(forURI: "content://media/external/1"))
  }

  func testAgeBasedSweepKeepsRecentFiles() throws {
    let store = TempFileStore(parent: parent)
    let old = try store.makeFileURL(fileExtension: "jpg")
    try Data([0x01]).write(to: old)
    let recent = try store.makeFileURL(fileExtension: "jpg")
    try Data([0x01]).write(to: recent)

    let now = Date()
    try FileManager.default.setAttributes(
      [.modificationDate: now.addingTimeInterval(-48 * 60 * 60)],
      ofItemAtPath: old.path
    )

    XCTAssertEqual(store.removeFiles(olderThan: TempFileStore.autoSweepAge, now: now), 1)
    XCTAssertFalse(FileManager.default.fileExists(atPath: old.path))
    XCTAssertTrue(FileManager.default.fileExists(atPath: recent.path))
  }

  func testAgeBasedSweepKeepsFilesWhoseTimestampCannotBeRead() throws {
    let fileManager = StubbedListingFileManager()
    let store = TempFileStore(parent: parent, fileManager: fileManager)

    let old = try store.makeFileURL(fileExtension: "jpg")
    try Data([0x01]).write(to: old)
    let now = Date()
    try FileManager.default.setAttributes(
      [.modificationDate: now.addingTimeInterval(-48 * 60 * 60)],
      ofItemAtPath: old.path
    )

    let ghost = old.deletingLastPathComponent()
      .appendingPathComponent("media_picker_already_deleted.jpg")
    fileManager.extraEntries = [ghost]

    let values = try? ghost.resourceValues(forKeys: [.contentModificationDateKey])
    XCTAssertNil(values?.contentModificationDate)

    XCTAssertEqual(store.removeFiles(olderThan: TempFileStore.autoSweepAge, now: now), 1)
    XCTAssertEqual(fileManager.removeAttempts.map(\.lastPathComponent), [old.lastPathComponent])
  }
}

private final class StubbedListingFileManager: FileManager {
  var extraEntries: [URL] = []
  private(set) var removeAttempts: [URL] = []

  override func contentsOfDirectory(
    at url: URL,
    includingPropertiesForKeys keys: [URLResourceKey]?,
    options mask: FileManager.DirectoryEnumerationOptions = []
  ) throws -> [URL] {
    try super.contentsOfDirectory(at: url, includingPropertiesForKeys: keys, options: mask)
      + extraEntries
  }

  override func removeItem(at url: URL) throws {
    removeAttempts.append(url)
    try super.removeItem(at: url)
  }
}
