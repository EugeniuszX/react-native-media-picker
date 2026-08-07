import Foundation

enum PickerError: String {
  case permission
  case cameraUnavailable = "camera_unavailable"
  case others

  var code: String { rawValue }
}
