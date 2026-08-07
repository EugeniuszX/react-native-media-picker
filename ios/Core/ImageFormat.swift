import Foundation
import UniformTypeIdentifiers

enum ImageFormat: String {
  case jpeg = "image/jpeg"
  case png = "image/png"
  case heic = "image/heic"
  case gif = "image/gif"
  case webp = "image/webp"

  var mime: String { rawValue }

  var fileExtension: String {
    switch self {
    case .jpeg: return "jpg"
    case .png: return "png"
    case .heic: return "heic"
    case .gif: return "gif"
    case .webp: return "webp"
    }
  }

  var isPotentiallyAnimated: Bool {
    self == .gif || self == .webp
  }

  var reencodeFormat: ImageFormat {
    switch self {
    case .png: return .png
    case .heic: return .heic
    case .jpeg, .gif, .webp: return .jpeg
    }
  }

  static func from(uti: String) -> ImageFormat {
    switch uti {
    case UTType.png.identifier: return .png
    case UTType.heic.identifier, UTType.heif.identifier: return .heic
    case UTType.gif.identifier: return .gif
    case UTType.webP.identifier: return .webp
    default: return .jpeg
    }
  }

  static func isAnimatedWebP(header: Data) -> Bool {
    guard header.count >= 21 else { return false }
    let bytes = [UInt8](header.prefix(21))
    let riff: [UInt8] = [0x52, 0x49, 0x46, 0x46]
    let webp: [UInt8] = [0x57, 0x45, 0x42, 0x50]
    let vp8x: [UInt8] = [0x56, 0x50, 0x38, 0x58]
    guard Array(bytes[0...3]) == riff,
      Array(bytes[8...11]) == webp,
      Array(bytes[12...15]) == vp8x
    else { return false }
    return (bytes[20] & 0x02) != 0
  }
}
