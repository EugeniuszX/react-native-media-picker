import Foundation
import UniformTypeIdentifiers

enum VideoFormat: String {
  case mp4 = "video/mp4"
  case quickTime = "video/quicktime"

  var mime: String { rawValue }

  var fileExtension: String {
    switch self {
    case .mp4: return "mp4"
    case .quickTime: return "mov"
    }
  }

  static func from(uti: String) -> VideoFormat {
    switch uti {
    case UTType.quickTimeMovie.identifier: return .quickTime
    default: return .mp4
    }
  }
}
