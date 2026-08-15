import Foundation
import ImageIO

enum MetadataScrubber {
  /// Rewrites the container without its identifying metadata. `AddImageFromSource` copies the
  /// already-compressed image data, so the pixels are untouched and no quality is lost.
  /// Returns `nil` when the container cannot be rewritten — the caller then re-encodes instead,
  /// because a half-stripped file is not an acceptable answer to `stripMetadata: true`.
  static func scrubbed(data: Data) -> Data? {
    guard let source = CGImageSourceCreateWithData(data as CFData, nil),
      let type = CGImageSourceGetType(source)
    else { return nil }

    let output = NSMutableData()
    guard let destination = CGImageDestinationCreateWithData(output, type, 1, nil) else {
      return nil
    }

    // The orientation lives in the TIFF dictionary and must survive: dropping it would render
    // a quarter-turned photo sideways. So TIFF is replaced by an orientation-only dictionary
    // rather than removed, while Make and Model go with the rest.
    var tiff: [CFString: Any] = [:]
    if let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
      let orientation = properties[kCGImagePropertyOrientation]
    {
      tiff[kCGImagePropertyTIFFOrientation] = orientation
    }

    let overrides: [CFString: Any] = [
      kCGImagePropertyExifDictionary: kCFNull,
      kCGImagePropertyGPSDictionary: kCFNull,
      kCGImagePropertyIPTCDictionary: kCFNull,
      kCGImagePropertyExifAuxDictionary: kCFNull,
      kCGImagePropertyMakerAppleDictionary: kCFNull,
      kCGImagePropertyTIFFDictionary: tiff.isEmpty ? kCFNull : tiff,
    ]

    CGImageDestinationAddImageFromSource(destination, source, 0, overrides as CFDictionary)
    guard CGImageDestinationFinalize(destination) else { return nil }
    return output as Data
  }
}
