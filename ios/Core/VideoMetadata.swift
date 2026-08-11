import CoreGraphics

enum VideoMetadata {
  static func displayedSize(
    naturalSize: CGSize,
    preferredTransform: CGAffineTransform
  ) -> (width: Int, height: Int) {
    let rect = CGRect(origin: .zero, size: naturalSize).applying(preferredTransform)
    return (Int(abs(rect.width).rounded()), Int(abs(rect.height).rounded()))
  }
}
