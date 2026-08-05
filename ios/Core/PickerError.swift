import Foundation

/// The closed set of failure codes the JS side can receive. Mirrors the
/// `ErrorCode` union in `src/NativeReactNativeMediaPicker.ts`.
enum PickerError: String {
  case permission
  case cameraUnavailable = "camera_unavailable"
  case others

  var code: String { rawValue }
}
