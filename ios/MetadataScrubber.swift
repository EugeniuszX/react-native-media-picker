import Foundation
import ImageIO
import UniformTypeIdentifiers

enum MetadataScrubber {
  /// Rewrites the container without its identifying metadata, leaving the compressed image data
  /// exactly as it was. Returns `nil` when the container cannot be rewritten that way — the caller
  /// then re-encodes instead, because a half-stripped file is not an acceptable answer to
  /// `stripMetadata: true`.
  static func scrubbed(data: Data) -> Data? {
    guard !XMPPacket.isPresent(in: data) else { return nil }

    guard let source = CGImageSourceCreateWithData(data as CFData, nil),
      let type = CGImageSourceGetType(source),
      UTType(type as String)?.conforms(to: .jpeg) == true
    else { return nil }

    let output = NSMutableData()
    guard let destination = CGImageDestinationCreateWithData(output, type, 1, nil) else {
      return nil
    }

    let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any] ?? [:]
    let metadata = CGImageMetadataCreateMutable()
    let orientation = properties[kCGImagePropertyOrientation] ?? 1
    guard
      CGImageMetadataSetValueMatchingImageProperty(
        metadata,
        kCGImagePropertyTIFFDictionary,
        kCGImagePropertyTIFFOrientation,
        orientation as CFTypeRef
      )
    else { return nil }

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
