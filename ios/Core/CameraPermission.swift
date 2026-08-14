import Foundation

/// The camera authorization states this library distinguishes, mirroring `AVAuthorizationStatus`
/// without depending on AVFoundation.
enum CameraAuthorization {
  case notDetermined
  case authorized
  case denied
  case restricted
  case unknown
}

/// The camera permission status reported to JS.
///
/// Two more values exist in the JS union and are Android-only: `not_required` (the app does not
/// declare `android.permission.CAMERA`) and `denied` (refused, but re-askable). iOS grants an app a
/// single chance to ask, so a refusal there is `blocked` — only Settings can undo it.
enum CameraPermission: String {
  case granted
  case notDetermined = "not_determined"
  case blocked
  case unavailable

  static func resolve(hasCamera: Bool, authorization: CameraAuthorization) -> CameraPermission {
    guard hasCamera else { return .unavailable }
    switch authorization {
    case .authorized: return .granted
    case .notDetermined: return .notDetermined
    case .denied, .restricted, .unknown: return .blocked
    }
  }
}
