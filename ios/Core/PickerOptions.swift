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
