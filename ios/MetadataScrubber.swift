import Foundation
import ImageIO

enum MetadataScrubber {
  /// The PNG keys that describe how the image should be rendered rather than who made it: gAMA,
  /// cHRM, sRGB, pHYs and tRNS. They share a dictionary with the `tEXt`/`iTXt` credits, so they
  /// have to be carried across by hand when that dictionary is replaced.
  private static let renderingPNGKeys: [CFString] = [
    kCGImagePropertyPNGGamma,
    kCGImagePropertyPNGChromaticities,
    kCGImagePropertyPNGsRGBIntent,
    kCGImagePropertyPNGXPixelsPerMeter,
    kCGImagePropertyPNGYPixelsPerMeter,
    kCGImagePropertyPNGTransparency,
  ]

  /// Rewrites the container without its identifying metadata. `AddImageFromSource` copies the
  /// already-compressed image data, so the pixels are untouched and no quality is lost.
  /// Returns `nil` when the container cannot be rewritten — the caller then re-encodes instead,
  /// because a half-stripped file is not an acceptable answer to `stripMetadata: true`.
  static func scrubbed(data: Data) -> Data? {
    // The overrides below reach only the `kCGImageProperty…` channel. XMP is a second
    // representation that can carry `exif:GPSLatitude`, `tiff:Make` and `tiff:Model`, and the two
    // exclusion keys further down are documented for `CGImageDestinationCopyImageSource` rather
    // than for the `AddImageFromSource` call used here — so a source carrying a packet is
    // declined outright and re-encoded by the caller instead. The re-encode is the deliberate
    // price of a guarantee that does not rest on undocumented behaviour, and it fires only for
    // the minority of assets that went through an XMP-writing pipeline.
    guard !XMPPacket.isPresent(in: data) else { return nil }

    guard let source = CGImageSourceCreateWithData(data as CFData, nil),
      let type = CGImageSourceGetType(source)
    else { return nil }

    let output = NSMutableData()
    guard let destination = CGImageDestinationCreateWithData(output, type, 1, nil) else {
      return nil
    }

    let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any] ?? [:]

    // The orientation lives in the TIFF dictionary and must survive: dropping it would render
    // a quarter-turned photo sideways. So TIFF is replaced by an orientation-only dictionary
    // rather than removed, while Make and Model go with the rest.
    var tiff: [CFString: Any] = [:]
    if let orientation = properties[kCGImagePropertyOrientation] {
      tiff[kCGImagePropertyTIFFOrientation] = orientation
    }

    // The same treatment for PNG: nulling that dictionary wholesale would take gAMA and cHRM with
    // it, shifting the colours of a PNG that carries no ICC profile, and could drop an indexed
    // image's tRNS alpha. Only the rendering keys are carried over; Author, Comment, Software and
    // the other `tEXt` credits are left behind.
    var png: [CFString: Any] = [:]
    if let sourcePNG = properties[kCGImagePropertyPNGDictionary] as? [CFString: Any] {
      for key in renderingPNGKeys {
        if let value = sourcePNG[key] {
          png[key] = value
        }
      }
    }

    var overrides: [CFString: Any] = [
      kCGImagePropertyExifDictionary: kCFNull,
      kCGImagePropertyGPSDictionary: kCFNull,
      kCGImagePropertyIPTCDictionary: kCFNull,
      kCGImagePropertyExifAuxDictionary: kCFNull,
      kCGImagePropertyMakerAppleDictionary: kCFNull,
      kCGImagePropertyTIFFDictionary: tiff.isEmpty ? kCFNull : tiff,
      kCGImagePropertyPNGDictionary: png.isEmpty ? kCFNull : png,
    ]

    // Belt and braces alongside the packet gate above: if ImageIO honours these on this call the
    // XMP never reaches the destination, and if it ignores them nothing is lost. They can only
    // ever subtract metadata. An empty `kCGImageDestinationMetadata` is deliberately not used —
    // it is documented to replace all EXIF and TIFF tags, which would take the orientation above
    // with it.
    overrides[kCGImageMetadataShouldExcludeXMP] = kCFBooleanTrue
    overrides[kCGImageMetadataShouldExcludeGPS] = kCFBooleanTrue

    CGImageDestinationAddImageFromSource(destination, source, 0, overrides as CFDictionary)
    guard CGImageDestinationFinalize(destination) else { return nil }
    return output as Data
  }
}
