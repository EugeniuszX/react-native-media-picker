import Foundation

/// Which representation PHPicker should hand over for a picked photo.
///
/// The system is willing to transcode an asset while exporting it — a HEIC photo can arrive as
/// JPEG. That costs time and contradicts `format: 'original'`, which promises the source format,
/// so the stored representation is requested whenever the source format is preserved. When the
/// output is re-encoded anyway (`format: 'jpeg'` / `'png'`), the system is left to pick whichever
/// representation it can produce fastest.
enum AssetRepresentationPlan {
  case current
  case automatic

  static func resolve(requested: RequestedFormat) -> AssetRepresentationPlan {
    switch requested {
    case .original: return .current
    case .jpeg, .png: return .automatic
    }
  }
}
