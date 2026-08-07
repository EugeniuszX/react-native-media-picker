import Foundation

struct ImageOrientation: Equatable {
  let rotationDegrees: Int
  /// Flip is applied after the rotation; the opposite order swaps EXIF 5 and 7.
  let isMirrored: Bool

  var swapsAxes: Bool {
    rotationDegrees == 90 || rotationDegrees == 270
  }

  static let upright = ImageOrientation(rotationDegrees: 0, isMirrored: false)

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
