import Foundation

struct TempFileStore {
  static let directoryName = "rn-media-picker"
  static let autoSweepAge: TimeInterval = 24 * 60 * 60

  private let root: URL
  private let fileManager: FileManager

  init(
    parent: URL = FileManager.default.temporaryDirectory,
    fileManager: FileManager = .default
  ) {
    self.root = parent.appendingPathComponent(Self.directoryName, isDirectory: true)
    self.fileManager = fileManager
  }

  func makeFileURL(fileExtension: String) throws -> URL {
    try fileManager.createDirectory(at: root, withIntermediateDirectories: true)
    return root.appendingPathComponent("media_picker_\(UUID().uuidString).\(fileExtension)")
  }

  @discardableResult
  func removeAll() -> Int {
    remove { _ in true }
  }

  @discardableResult
  func remove(uris: [String]) -> Int {
    let names = Set(uris.compactMap(Self.fileName(forURI:)))
    guard !names.isEmpty else { return 0 }
    return remove { names.contains($0.lastPathComponent) }
  }

  static func fileName(forURI uri: String) -> String? {
    guard let url = URL(string: uri), url.isFileURL else { return nil }
    let name = url.lastPathComponent
    return name.isEmpty || name == "/" ? nil : name
  }

  @discardableResult
  func removeFiles(olderThan age: TimeInterval, now: Date = Date()) -> Int {
    remove { url in
      let values = try? url.resourceValues(forKeys: [.contentModificationDateKey])
      guard let modified = values?.contentModificationDate else { return false }
      return now.timeIntervalSince(modified) > age
    }
  }

  private func remove(where shouldRemove: (URL) -> Bool) -> Int {
    guard
      let entries = try? fileManager.contentsOfDirectory(
        at: root,
        includingPropertiesForKeys: [.contentModificationDateKey]
      )
    else {
      return 0
    }
    var removed = 0
    for url in entries where shouldRemove(url) {
      do {
        try fileManager.removeItem(at: url)
        removed += 1
      } catch {
        NSLog(
          "[ReactNativeMediaPicker] failed to remove %@: %@", url.lastPathComponent,
          error.localizedDescription)
      }
    }
    return removed
  }
}
