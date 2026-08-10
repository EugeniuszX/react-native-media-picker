import Foundation

enum CameraFacing: String {
  case back
  case front

  static func from(rawValue: String) -> CameraFacing {
    CameraFacing(rawValue: rawValue) ?? .back
  }
}

struct LibraryOptions: Equatable {
  let selectionLimit: Int
  let maxWidth: Int
  let maxHeight: Int
  let quality: Double
  let includeBase64: Bool
}

struct CameraOptions: Equatable {
  let facing: CameraFacing
  let maxWidth: Int
  let maxHeight: Int
  let quality: Double
  let includeBase64: Bool
}

enum RequestedFormat: String {
  case original
  case jpeg
  case png

  static func from(rawValue: String) -> RequestedFormat {
    RequestedFormat(rawValue: rawValue) ?? .original
  }
}
