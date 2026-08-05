import Foundation

/// One picked item. `dictionary` is the only place asset keys are spelled out —
/// it must stay in sync with the `Asset` type in
/// `src/NativeReactNativeMediaPicker.ts`.
struct AssetPayload: Equatable {
  let uri: String
  let mime: String
  let fileName: String
  let fileSize: Int
  let width: Int
  let height: Int
  let base64: String?

  var dictionary: [String: Any] {
    var dict: [String: Any] = [
      "uri": uri,
      "type": mime,
      "fileName": fileName,
      "fileSize": fileSize,
      "width": width,
      "height": height,
    ]
    if let base64 {
      dict["base64"] = base64
    }
    return dict
  }
}
