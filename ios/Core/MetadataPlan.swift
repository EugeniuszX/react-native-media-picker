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
    preserveSource: Bool,
    canScrub: Bool
  ) -> MetadataAction {
    guard stripMetadata else { return .skip }
    // A re-encode decodes to a bitmap and writes fresh bytes, so no metadata survives it.
    if willTransform { return .skip }
    // Some sources must come back byte-for-byte: an animated one would lose its frames to a
    // re-encode, and a GIF would be flattened into a JPEG to remove metadata it never had.
    if preserveSource { return .skip }
    return canScrub ? .scrub : .forceReencode
  }

  /// Whether a source must come back byte-for-byte, which is what `resolve`'s `preserveSource`
  /// expects. Two independent reasons: an animated source would lose its frames to a re-encode,
  /// and a GIF re-encodes to JPEG — flattening any transparency onto black to remove metadata
  /// a GIF never carries in the first place. So a GIF is preserved whether animated or not.
  static func preservesSource(format: ImageFormat, preserveAnimation: Bool) -> Bool {
    preserveAnimation || format == .gif
  }

  /// The containers this library rewrites in place. WebP is excluded because iOS ships no WebP
  /// encoder at all; GIF is excluded by policy rather than capability — `CGImageDestination` can
  /// write one, but a rewrite risks the animation, so GIFs are passed through untouched.
  static func canScrub(_ format: ImageFormat) -> Bool {
    switch format {
    case .jpeg, .png, .heic: return true
    case .gif, .webp: return false
    }
  }
}
