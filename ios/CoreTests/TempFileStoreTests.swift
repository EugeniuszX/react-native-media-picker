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

  /// removeAll empties the directory but must not delete the directory itself —
  /// the module sweeps this directory at init and then writes into it. Mirrors
  /// `removeAll keeps the directory itself` in the Kotlin store's tests.
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

  /// Mirrors `age based sweep keeps files whose timestamp cannot be read` in the
  /// Kotlin store's tests. Same semantics, reached by different code — which is
  /// the whole reason the two suites are name-symmetric. Kotlin needs an explicit
  /// `modified > 0L` guard because `File.lastModified()` returns 0 both for the
  /// epoch and for a failed read; Swift's resource value is optional, so the
  /// `guard let` keeps the file for free.
  ///
  /// The *fixtures* have to differ too. Kotlin's `setLastModified(0)` has no
  /// Swift equivalent: epoch is a perfectly readable date here and would be
  /// swept. Nor does any on-disk entry work — verified that a dangling symlink
  /// still reports its own `lstat` date, because a URL resource value describes
  /// the directory entry, not what it resolves to. The only way the read fails is
  /// for the entry to be gone by the time it is stat'd, i.e. the TOCTOU window
  /// between `contentsOfDirectory` and the per-entry `resourceValues` — which a
  /// concurrent `cleanTempFiles` can genuinely open. So the listing is stubbed to
  /// include one entry that is not there.
  ///
  /// The observation has to differ as well: the return count cannot tell the two
  /// branches apart, because a ghost the sweep *did* try to delete would fail to
  /// delete and go uncounted anyway. So the assertion is on which entries
  /// `removeItem` was asked to delete, which does flip if the guard flips.
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

    // Asserted, not assumed: if the ghost had a readable timestamp this test
    // would pass for the wrong reason and never fail again.
    let values = try? ghost.resourceValues(forKeys: [.contentModificationDateKey])
    XCTAssertNil(values?.contentModificationDate)

    XCTAssertEqual(store.removeFiles(olderThan: TempFileStore.autoSweepAge, now: now), 1)
    // The genuinely old file proves the sweep ran; the ghost was never touched.
    // Compared by name: the listing hands back /private-resolved paths.
    XCTAssertEqual(fileManager.removeAttempts.map(\.lastPathComponent), [old.lastPathComponent])
  }
}

/// Appends entries to every directory listing and records every deletion the
/// store attempts. Lets a test describe a directory the real filesystem cannot
/// produce — see `testAgeBasedSweepKeepsFilesWhoseTimestampCannotBeRead`.
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
