import Foundation
import ImageIO

enum ExifReader {
  static func read(from data: Data) -> ExifPayload? {
    guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
    return read(properties: CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any])
  }

  /// Also serves camera captures, whose `UIImagePickerController.InfoKey.mediaMetadata`
  /// dictionary uses the very same `kCGImageProperty…` keys.
  static func read(properties: [CFString: Any]?) -> ExifPayload? {
    guard let properties else { return nil }
    let exif = properties[kCGImagePropertyExifDictionary] as? [CFString: Any] ?? [:]
    let gps = properties[kCGImagePropertyGPSDictionary] as? [CFString: Any] ?? [:]
    let tiff = properties[kCGImagePropertyTIFFDictionary] as? [CFString: Any] ?? [:]

    var payload = ExifPayload()
    payload.dateTimeOriginal = ExifDate.iso8601(
      from: exif[kCGImagePropertyExifDateTimeOriginal] as? String)
    payload.latitude = GPSCoordinate.signed(
      (gps[kCGImagePropertyGPSLatitude] as? NSNumber)?.doubleValue,
      ref: gps[kCGImagePropertyGPSLatitudeRef] as? String)
    payload.longitude = GPSCoordinate.signed(
      (gps[kCGImagePropertyGPSLongitude] as? NSNumber)?.doubleValue,
      ref: gps[kCGImagePropertyGPSLongitudeRef] as? String)
    payload.altitude = GPSCoordinate.altitude(
      (gps[kCGImagePropertyGPSAltitude] as? NSNumber)?.doubleValue,
      ref: (gps[kCGImagePropertyGPSAltitudeRef] as? NSNumber)?.intValue)
    payload.make = tiff[kCGImagePropertyTIFFMake] as? String
    payload.model = tiff[kCGImagePropertyTIFFModel] as? String
    payload.orientation = (properties[kCGImagePropertyOrientation] as? NSNumber)?.intValue
    payload.iso = (exif[kCGImagePropertyExifISOSpeedRatings] as? [NSNumber])?.first?.intValue
    payload.fNumber = (exif[kCGImagePropertyExifFNumber] as? NSNumber)?.doubleValue
    payload.exposureTime = (exif[kCGImagePropertyExifExposureTime] as? NSNumber)?.doubleValue
    payload.focalLength = (exif[kCGImagePropertyExifFocalLength] as? NSNumber)?.doubleValue

    return payload.isEmpty ? nil : payload
  }
}
