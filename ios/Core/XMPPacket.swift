import Foundation

enum XMPPacket {
  /// XMP is stored as uncompressed XML text — a JPEG `APP1` segment, a HEIF `mime` item — so a
  /// packet is literally present in the container's bytes and can be found without a parser.
  ///
  /// Three markers rather than one: a packet can be written without the `x:xmpmeta` wrapper, so
  /// the fixed packet id and the XMP namespace URI are checked too.
  private static let markers = [
    "<x:xmpmeta",
    "W5M0MpCehiHzreSzNTczkc9d",
    "ns.adobe.com/xap",
  ]

  static func isPresent(in data: Data) -> Bool {
    markers.contains { marker in
      guard let bytes = marker.data(using: .ascii) else { return false }
      return data.range(of: bytes) != nil
    }
  }
}
