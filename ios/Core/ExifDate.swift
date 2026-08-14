import Foundation

enum ExifDate {
  /// Converts an EXIF timestamp (`'2026:08:14 15:29:03'`) to ISO-8601 without a timezone.
  /// EXIF carries no offset, so none is invented. Anything malformed yields `nil` rather than
  /// a half-parsed value.
  static func iso8601(from raw: String?) -> String? {
    guard let raw else { return nil }
    let parts = raw.trimmingCharacters(in: .whitespaces).split(separator: " ")
    guard parts.count == 2 else { return nil }

    let date = parts[0].split(separator: ":")
    let time = parts[1].split(separator: ":")
    guard date.count == 3, time.count == 3 else { return nil }

    let widths = [4, 2, 2, 2, 2, 2]
    let fields = date + time
    for (field, width) in zip(fields, widths) {
      // ASCII-only: `isNumber` alone also accepts Arabic-Indic digits and other Unicode number
      // categories, which the Kotlin unit's `\d` rejects and ISO-8601 has no place for.
      guard field.count == width, field.allSatisfy({ $0.isASCII && $0.isNumber }) else {
        return nil
      }
    }
    // Cameras write an all-zero placeholder when the clock was never set.
    guard date[0] != "0000" else { return nil }

    return "\(date[0])-\(date[1])-\(date[2])T\(time[0]):\(time[1]):\(time[2])"
  }
}
