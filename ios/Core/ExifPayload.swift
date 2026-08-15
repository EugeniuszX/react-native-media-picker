import Foundation

/// The normalised metadata subset returned as `Asset.exif`. Every field is optional: a source
/// without a GPS block simply has no coordinates.
struct ExifPayload: Equatable {
  var dateTimeOriginal: String?
  var latitude: Double?
  var longitude: Double?
  var altitude: Double?
  var make: String?
  var model: String?
  var orientation: Int?
  var iso: Int?
  var fNumber: Double?
  var exposureTime: Double?
  var focalLength: Double?

  var isEmpty: Bool {
    dateTimeOriginal == nil && latitude == nil && longitude == nil && altitude == nil
      && make == nil && model == nil && orientation == nil && iso == nil && fNumber == nil
      && exposureTime == nil && focalLength == nil
  }

  var dictionary: [String: Any] {
    var dict: [String: Any] = [:]
    if let dateTimeOriginal { dict["dateTimeOriginal"] = dateTimeOriginal }
    if let latitude { dict["latitude"] = latitude }
    if let longitude { dict["longitude"] = longitude }
    if let altitude { dict["altitude"] = altitude }
    if let make { dict["make"] = make }
    if let model { dict["model"] = model }
    if let orientation { dict["orientation"] = orientation }
    if let iso { dict["iso"] = iso }
    if let fNumber { dict["fNumber"] = fNumber }
    if let exposureTime { dict["exposureTime"] = exposureTime }
    if let focalLength { dict["focalLength"] = focalLength }
    return dict
  }
}
