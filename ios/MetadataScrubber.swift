import Foundation
import ImageIO
import UniformTypeIdentifiers

enum MetadataScrubber {
  /// Rewrites the container without its identifying metadata, leaving the compressed image data
  /// exactly as it was. Returns `nil` when the container cannot be rewritten that way — the caller
  /// then re-encodes instead, because a half-stripped file is not an acceptable answer to
  /// `stripMetadata: true`.
  static func scrubbed(data: Data) -> Data? {
    // The exclusion keys below reach only what ImageIO chooses to rewrite. XMP is a second
    // representation that can carry `exif:GPSLatitude`, `tiff:Make` and `tiff:Model`, and a source
    // already carrying a packet was measured to keep it — so such a source is declined outright
    // and re-encoded by the caller instead. This gate reads raw bytes and depends on nothing
    // ImageIO does, which is why it comes first.
    guard !XMPPacket.isPresent(in: data) else { return nil }

    // JPEG only. `CGImageDestinationCopyImageSource` is the one ImageIO call documented to copy
    // the image data unmodified, and it was measured to strip completely only for JPEG: on PNG it
    // leaves every `tEXt` credit in place and *adds* an XMP packet rebuilt from them, and on HEIC
    // it keeps Artist, Copyright, DateTime, Software, the IPTC by-line and the XMP. Those two go
    // to the caller's re-encode, which was measured clean.
    guard let source = CGImageSourceCreateWithData(data as CFData, nil),
      let type = CGImageSourceGetType(source),
      UTType(type as String)?.conforms(to: .jpeg) == true
    else { return nil }

    let output = NSMutableData()
    guard let destination = CGImageDestinationCreateWithData(output, type, 1, nil) else {
      return nil
    }

    // The orientation must survive: dropping it would render a quarter-turned photo sideways, and
    // the reported `width`/`height` assume it intact. `kCGImageDestinationMetadata` *replaces*
    // every EXIF/IPTC/XMP tag unless `kCGImageDestinationMergeMetadata` is set, so an
    // orientation-only object — and no merge flag — is what strips the rest. Merging is what an
    // earlier implementation did through a partial TIFF dictionary, and it left Make, Model,
    // Software, DateTime, Artist and Copyright in the file.
    let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any] ?? [:]
    let metadata = CGImageMetadataCreateMutable()
    if let orientation = properties[kCGImagePropertyOrientation] {
      guard
        CGImageMetadataSetValueMatchingImageProperty(
          metadata,
          kCGImagePropertyTIFFDictionary,
          kCGImagePropertyTIFFOrientation,
          orientation as CFTypeRef
        )
      else { return nil }
    }

    let options: [CFString: Any] = [
      kCGImageDestinationMetadata: metadata,
      kCGImageMetadataShouldExcludeXMP: kCFBooleanTrue as Any,
      kCGImageMetadataShouldExcludeGPS: kCFBooleanTrue as Any,
    ]

    guard CGImageDestinationCopyImageSource(destination, source, options as CFDictionary, nil)
    else { return nil }
    return output as Data
  }
}
