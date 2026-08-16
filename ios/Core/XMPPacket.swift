import Foundation

enum XMPPacket {
  /// XMP is stored as uncompressed XML text — a JPEG `APP1` segment, a HEIF `mime` item — so a
  /// packet is literally present in the container's bytes and can be found without a parser.
  ///
  /// Four markers rather than one: a packet can be written without the `x:xmpmeta` wrapper, so
  /// the fixed packet id and the XMP namespace URI are checked too. `XML:com.adobe.xmp` is the
  /// PNG `iTXt` keyword — a keyword is stored uncompressed even when its payload is deflated, so
  /// it is the only one of the four a compression-flagged chunk leaves in plain bytes. Android's
  /// `MetadataResidue` matches the same four.
  private static let markers = [
    "<x:xmpmeta",
    "W5M0MpCehiHzreSzNTczkc9d",
    "ns.adobe.com/xap",
    "XML:com.adobe.xmp",
  ]

  static func isPresent(in data: Data) -> Bool {
    markers.contains { marker in
      guard let bytes = marker.data(using: .ascii) else { return false }
      return data.range(of: bytes) != nil
    }
  }
}
