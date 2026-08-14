import Foundation

/// Builds the `fileName` an asset is reported with.
///
/// The name the system hands over for a picked item is a label, not a path — the bytes always live
/// in a temp file under a generated name — so it is sanitized before it leaves the module, and its
/// extension is replaced with that of the file actually written. `fallback` (the temp file's own
/// name) is used whenever nothing usable survives sanitizing.
enum AssetFileName {
  static let maxBaseLength = 100

  static func resolve(suggested: String?, fallback: String, fileExtension: String) -> String {
    guard let base = sanitizedBase(of: suggested) else { return fallback }
    return fileExtension.isEmpty ? base : "\(base).\(fileExtension)"
  }

  private static let trimmed = CharacterSet.whitespaces.union(CharacterSet(charactersIn: "."))

  private static func sanitizedBase(of suggested: String?) -> String? {
    guard let suggested else { return nil }
    var name = lastPathComponent(of: suggested)
    name = name.components(separatedBy: .controlCharacters).joined()
    name = name.trimmingCharacters(in: .whitespaces)
    name = strippingExtension(from: name)
    name = name.trimmingCharacters(in: trimmed)
    if name.count > maxBaseLength {
      name = String(name.prefix(maxBaseLength)).trimmingCharacters(in: trimmed)
    }
    return name.isEmpty ? nil : name
  }

  private static func lastPathComponent(of value: String) -> String {
    value.split(whereSeparator: { $0 == "/" || $0 == "\\" }).last.map(String.init) ?? ""
  }

  /// Drops a trailing `.ext` only when it looks like a file extension: at most five ASCII
  /// alphanumerics. Keeps names that merely contain dots, like `report.final`.
  private static func strippingExtension(from name: String) -> String {
    guard let dot = name.lastIndex(of: ".") else { return name }
    let ext = name[name.index(after: dot)...]
    guard !ext.isEmpty, ext.count <= 5,
      ext.allSatisfy({ $0.isASCII && ($0.isLetter || $0.isNumber) })
    else {
      return name
    }
    return String(name[name.startIndex..<dot])
  }
}
