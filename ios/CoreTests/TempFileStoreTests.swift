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

  func testRemoveAllOnAMissingDirectoryIsANoOp() {
    let store = TempFileStore(parent: parent)
    XCTAssertEqual(store.removeAll(), 0)
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
}
