import Foundation

enum GPSCoordinate {
  /// ImageIO reports latitude and longitude as unsigned decimal degrees plus a hemisphere ref.
  static func signed(_ magnitude: Double?, ref: String?) -> Double? {
    guard let magnitude else { return nil }
    let hemisphere = ref?.trimmingCharacters(in: .whitespaces).uppercased()
    let negative = hemisphere == "S" || hemisphere == "W"
    return negative ? -magnitude : magnitude
  }

  /// `GPSAltitudeRef` is `1` when the altitude is below sea level, `0` (or absent) above it.
  static func altitude(_ value: Double?, ref: Int?) -> Double? {
    guard let value else { return nil }
    return ref == 1 ? -value : value
  }
}
