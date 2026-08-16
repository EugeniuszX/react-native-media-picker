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
    if willTransform { return .skip }
    if canScrub { return .scrub }
    if preserveSource { return .skip }
    return .forceReencode
  }

  /// Whether a source must not be re-encoded, which is what `resolve`'s `preserveSource`
  /// expects. Two independent reasons: an animated source would lose its frames, and a GIF
  /// re-encodes to JPEG — flattening any transparency onto black to remove metadata a GIF never
  /// carries in the first place. So a GIF is preserved whether animated or not.
  ///
  /// This does not stop a scrub, which rewrites the container without touching the pixels.
  static func preservesSource(format: ImageFormat, preserveAnimation: Bool) -> Bool {
    preserveAnimation || format == .gif
  }

  /// The containers this library rewrites in place: JPEG, and only JPEG.
  /// `CGImageDestinationCopyImageSource` is the one ImageIO call that leaves the compressed data
  /// alone, and it was measured to strip completely only for JPEG — on PNG it keeps every `tEXt`
  /// credit and adds an XMP packet rebuilt from them, and on HEIC it keeps Artist, Copyright,
  /// DateTime, Software and the XMP. PNG and HEIC therefore re-encode, which is measured clean.
  /// WebP is excluded because iOS ships no WebP encoder at all; GIF is excluded by policy rather
  /// than capability — `CGImageDestination` can write one, but a rewrite risks the animation, so
  /// GIFs are passed through untouched.
  static func canScrub(_ format: ImageFormat) -> Bool {
    switch format {
    case .jpeg: return true
    case .png, .heic, .gif, .webp: return false
    }
  }
}
