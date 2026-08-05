import Foundation

enum CameraFacing: String {
  case back
  case front

  /// Unknown values fall back to `.back`, matching the JS-side normalization.
  static func from(rawValue: String) -> CameraFacing {
    CameraFacing(rawValue: rawValue) ?? .back
  }
}

/// Immutable per-request options. Built once at the module boundary and passed
/// down the pipeline — never stored on a long-lived object.
struct LibraryOptions: Equatable {
  /// 0 means unlimited.
  let selectionLimit: Int
  /// 0 means "no bound on this axis".
  let maxWidth: Int
  let maxHeight: Int
  /// 0...1, already clamped by the JS wrapper.
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
