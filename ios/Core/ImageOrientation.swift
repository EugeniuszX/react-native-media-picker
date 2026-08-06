import Foundation

/// An EXIF / `CGImagePropertyOrientation` tag decomposed into the two operations
/// we care about. Having one mapping for both the passthrough and the transform
/// path is what keeps reported dimensions and rendered pixels consistent.
struct ImageOrientation: Equatable {
  /// Clockwise rotation, in degrees, needed to display the stored buffer upright.
  let rotationDegrees: Int
  /// Whether the buffer must also be flipped horizontally. The flip is applied
  /// **after** the rotation, in the rotated coordinate space — with the opposite
  /// order, EXIF 5 and EXIF 7 would silently swap meanings.
  let isMirrored: Bool

  /// True when the rotation transposes the buffer's width and height.
  var swapsAxes: Bool {
    rotationDegrees == 90 || rotationDegrees == 270
  }

  static let upright = ImageOrientation(rotationDegrees: 0, isMirrored: false)

  /// Maps a raw EXIF orientation tag (1...8). Anything outside that range is
  /// treated as upright, which is how decoders behave on malformed metadata.
  static func from(exifValue: Int) -> ImageOrientation {
    switch exifValue {
    case 2: return ImageOrientation(rotationDegrees: 0, isMirrored: true)
    case 3: return ImageOrientation(rotationDegrees: 180, isMirrored: false)
    case 4: return ImageOrientation(rotationDegrees: 180, isMirrored: true)
    case 5: return ImageOrientation(rotationDegrees: 90, isMirrored: true)
    case 6: return ImageOrientation(rotationDegrees: 90, isMirrored: false)
    case 7: return ImageOrientation(rotationDegrees: 270, isMirrored: true)
    case 8: return ImageOrientation(rotationDegrees: 270, isMirrored: false)
    default: return .upright
    }
  }
}
