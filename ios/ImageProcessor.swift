import CoreGraphics
import ImageIO
import UIKit
import UniformTypeIdentifiers

/// Turns encoded bytes into an `AssetPayload`. Stateless — every call carries
/// its own options, so concurrent calls cannot interfere.
struct ImageProcessor {
  private let tempFiles: TempFileStore

  init(tempFiles: TempFileStore) {
    self.tempFiles = tempFiles
  }

  /// Library path: metadata is read first so untouched images never get decoded.
  func process(
    data: Data,
    format: ImageFormat,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    includeBase64: Bool
  ) -> AssetPayload? {
    let metadata = readMetadata(from: data)
    let isAnimated =
      format == .gif
      || (format == .webp && ImageFormat.isAnimatedWebP(header: data))
    let plan = DecodePlan.compute(
      pixelWidth: metadata.pixelWidth,
      pixelHeight: metadata.pixelHeight,
      orientation: metadata.orientation,
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      isAnimated: isAnimated
    )

    guard plan.needsTransform else {
      return write(
        data: data,
        format: format,
        width: plan.displayWidth,
        height: plan.displayHeight,
        includeBase64: includeBase64
      )
    }

    // UIImage applies the EXIF orientation for us, so drawing into a
    // target-sized renderer yields upright, correctly mirrored pixels.
    guard let image = UIImage(data: data) else { return nil }
    let resized = redraw(image, width: plan.targetWidth, height: plan.targetHeight)
    return encodeAndWrite(
      resized,
      format: format.reencodeFormat,
      quality: quality,
      includeBase64: includeBase64
    )
  }

  /// Camera path. A capture arrives already decoded, so there are no original
  /// bytes to pass through — the output is always a freshly encoded JPEG.
  func process(
    capturedImage image: UIImage,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    includeBase64: Bool
  ) -> AssetPayload? {
    let pixelWidth = image.cgImage?.width ?? Int(image.size.width * image.scale)
    let pixelHeight = image.cgImage?.height ?? Int(image.size.height * image.scale)
    let plan = DecodePlan.compute(
      pixelWidth: pixelWidth,
      pixelHeight: pixelHeight,
      orientation: orientation(of: image),
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      isAnimated: false
    )
    // Both branches redraw: even without a resize the capture has to be baked
    // into an upright, scale-1 buffer before it can be encoded.
    let output =
      plan.needsTransform
      ? redraw(image, width: plan.targetWidth, height: plan.targetHeight)
      : redraw(image, width: plan.displayWidth, height: plan.displayHeight)
    return encodeAndWrite(
      output,
      format: .jpeg,
      quality: quality,
      includeBase64: includeBase64
    )
  }

  // MARK: - Metadata

  private struct Metadata {
    let pixelWidth: Int
    let pixelHeight: Int
    let orientation: ImageOrientation
  }

  private func readMetadata(from data: Data) -> Metadata {
    guard let source = CGImageSourceCreateWithData(data as CFData, nil),
      let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any]
    else {
      NSLog("[ReactNativeMediaPicker] warning: could not read image metadata")
      return Metadata(pixelWidth: 0, pixelHeight: 0, orientation: .upright)
    }
    let width = (props[kCGImagePropertyPixelWidth] as? Int) ?? 0
    let height = (props[kCGImagePropertyPixelHeight] as? Int) ?? 0
    let exif = (props[kCGImagePropertyOrientation] as? NSNumber)?.intValue ?? 1
    return Metadata(
      pixelWidth: width,
      pixelHeight: height,
      orientation: ImageOrientation.from(exifValue: exif)
    )
  }

  /// `UIImage.imageOrientation` expressed as the shared core type. A decoded
  /// image reports the transform still owed to its buffer.
  private func orientation(of image: UIImage) -> ImageOrientation {
    switch image.imageOrientation {
    case .up: return .upright
    case .upMirrored: return ImageOrientation.from(exifValue: 2)
    case .down: return ImageOrientation.from(exifValue: 3)
    case .downMirrored: return ImageOrientation.from(exifValue: 4)
    case .leftMirrored: return ImageOrientation.from(exifValue: 5)
    case .right: return ImageOrientation.from(exifValue: 6)
    case .rightMirrored: return ImageOrientation.from(exifValue: 7)
    case .left: return ImageOrientation.from(exifValue: 8)
    @unknown default: return .upright
    }
  }

  // MARK: - Pixels

  private func redraw(_ image: UIImage, width: Int, height: Int) -> UIImage {
    let size = CGSize(width: max(1, width), height: max(1, height))
    let format = UIGraphicsImageRendererFormat.default()
    format.scale = 1
    format.opaque = false
    return UIGraphicsImageRenderer(size: size, format: format).image { _ in
      image.draw(in: CGRect(origin: .zero, size: size))
    }
  }

  private func encode(
    _ image: UIImage,
    format: ImageFormat,
    quality: Double
  ) -> (data: Data, format: ImageFormat)? {
    switch format {
    case .png:
      guard let data = image.pngData() else { return nil }
      return (data, .png)
    case .heic:
      if let data = heicData(from: image, quality: quality) {
        return (data, .heic)
      }
      // No HEIC encoder (Simulator, pre-A10). Mirrors the Android fallback.
      guard let data = image.jpegData(compressionQuality: CGFloat(quality)) else { return nil }
      return (data, .jpeg)
    case .jpeg, .gif, .webp:
      guard let data = image.jpegData(compressionQuality: CGFloat(quality)) else { return nil }
      return (data, .jpeg)
    }
  }

  private func heicData(from image: UIImage, quality: Double) -> Data? {
    guard let cgImage = image.cgImage else { return nil }
    let output = NSMutableData()
    guard
      let destination = CGImageDestinationCreateWithData(
        output, UTType.heic.identifier as CFString, 1, nil
      )
    else { return nil }
    CGImageDestinationAddImage(
      destination,
      cgImage,
      [kCGImageDestinationLossyCompressionQuality: quality] as CFDictionary
    )
    guard CGImageDestinationFinalize(destination) else { return nil }
    return output as Data
  }

  // MARK: - Output

  private func encodeAndWrite(
    _ image: UIImage,
    format: ImageFormat,
    quality: Double,
    includeBase64: Bool
  ) -> AssetPayload? {
    guard let encoded = encode(image, format: format, quality: quality) else { return nil }
    // `redraw` always produces an upright, scale-1 image, so its buffer
    // dimensions already are the display dimensions — no axis swap needed here.
    let cgImage = image.cgImage
    let pixelWidth = cgImage?.width ?? Int(image.size.width)
    let pixelHeight = cgImage?.height ?? Int(image.size.height)
    return write(
      data: encoded.data,
      format: encoded.format,
      width: pixelWidth,
      height: pixelHeight,
      includeBase64: includeBase64
    )
  }

  private func write(
    data: Data,
    format: ImageFormat,
    width: Int,
    height: Int,
    includeBase64: Bool
  ) -> AssetPayload? {
    do {
      let url = try tempFiles.makeFileURL(fileExtension: format.fileExtension)
      try data.write(to: url)
      return AssetPayload(
        uri: url.absoluteString,
        mime: format.mime,
        fileName: url.lastPathComponent,
        fileSize: data.count,
        width: width,
        height: height,
        base64: includeBase64 ? data.base64EncodedString() : nil
      )
    } catch {
      NSLog("[ReactNativeMediaPicker] failed to write asset: %@", error.localizedDescription)
      return nil
    }
  }
}
