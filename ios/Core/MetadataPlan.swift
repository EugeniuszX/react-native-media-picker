import Foundation

/// What to do with the metadata of an asset that is otherwise passed through untouched.
enum MetadataAction: Equatable {
  /// Leave the bytes alone.
  case skip
  /// Rewrite the container without its metadata; the pixel data is copied, not re-encoded.
  case scrub
  /// The container cannot be rewritten on this platform, so re-encode to guarantee the strip.
  case forceReencode
}

enum MetadataPlan {
  static func resolve(
    stripMetadata: Bool,
    willTransform: Bool,
    isAnimated: Bool,
    canScrub: Bool
  ) -> MetadataAction {
    guard stripMetadata else { return .skip }
    // A re-encode decodes to a bitmap and writes fresh bytes, so no metadata survives it.
    if willTransform { return .skip }
    // Animated sources are never modified — re-encoding them would lose the animation.
    if isAnimated { return .skip }
    return canScrub ? .scrub : .forceReencode
  }

  /// The containers `CGImageDestination` can rewrite in place. GIF and WebP are excluded:
  /// iOS has no WebP encoder, and a GIF is animation-bearing by nature.
  static func canScrub(_ format: ImageFormat) -> Bool {
    switch format {
    case .jpeg, .png, .heic: return true
    case .gif, .webp: return false
    }
  }
}
