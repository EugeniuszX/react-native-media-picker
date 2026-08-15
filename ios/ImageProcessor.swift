import CoreGraphics
import ImageIO
import UIKit
import UniformTypeIdentifiers

struct ImageProcessor {
  private let tempFiles: TempFileStore

  init(tempFiles: TempFileStore) {
    self.tempFiles = tempFiles
  }

  func process(
    data: Data,
    format: ImageFormat,
    requested: RequestedFormat,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    includeBase64: Bool,
    stripMetadata: Bool = false,
    includeExif: Bool = false,
    suggestedName: String? = nil
  ) -> AssetPayload? {
    let metadata = readMetadata(from: data)
    let exif = includeExif ? ExifReader.read(from: data) : nil
    let isAnimated =
      format == .gif
      || (format == .webp && ImageFormat.isAnimatedWebP(header: data))
    let output = OutputPlan.resolve(
      source: format,
      requested: requested,
      isAnimatedSource: isAnimated
    )
    let plan = DecodePlan.compute(
      pixelWidth: metadata.pixelWidth,
      pixelHeight: metadata.pixelHeight,
      orientation: metadata.orientation,
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      isAnimated: output.preserveAnimation
    )

    let willTransform = plan.needsTransform || output.forceReencode
    let action = MetadataPlan.resolve(
      stripMetadata: stripMetadata,
      willTransform: willTransform,
      preserveSource: output.preserveAnimation || format == .gif,
      canScrub: MetadataPlan.canScrub(format)
    )

    if !willTransform, action != .forceReencode {
      // `.scrub` falls through to the transform path when ImageIO refuses the rewrite, so a
      // strip request is never silently ignored.
      let payloadData = action == .scrub ? MetadataScrubber.scrubbed(data: data) : data
      if let payloadData {
        return write(
          data: payloadData,
          format: format,
          width: plan.displayWidth,
          height: plan.displayHeight,
          includeBase64: includeBase64,
          suggestedName: suggestedName,
          exif: exif
        )
      }
    }

    guard let image = UIImage(data: data) else { return nil }
    let targetWidth = plan.targetWidth > 0 ? plan.targetWidth : Int(image.size.width * image.scale)
    let targetHeight =
      plan.targetHeight > 0 ? plan.targetHeight : Int(image.size.height * image.scale)
    let resized = redraw(image, width: targetWidth, height: targetHeight)
    return encodeAndWrite(
      resized,
      format: output.target,
      quality: quality,
      includeBase64: includeBase64,
      suggestedName: suggestedName,
      exif: exif
    )
  }

  func process(
    capturedImage image: UIImage,
    requested: RequestedFormat,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    includeBase64: Bool,
    exif: ExifPayload? = nil
  ) -> AssetPayload? {
    let pixelWidth = image.cgImage?.width ?? Int(image.size.width * image.scale)
    let pixelHeight = image.cgImage?.height ?? Int(image.size.height * image.scale)
    let output = OutputPlan.resolve(
      source: .jpeg,
      requested: requested,
      isAnimatedSource: false
    )
    let plan = DecodePlan.compute(
      pixelWidth: pixelWidth,
      pixelHeight: pixelHeight,
      orientation: orientation(of: image),
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      isAnimated: false
    )
    let resized =
      plan.needsTransform
      ? redraw(image, width: plan.targetWidth, height: plan.targetHeight)
      : redraw(image, width: plan.displayWidth, height: plan.displayHeight)
    return encodeAndWrite(
      resized,
      format: output.target,
      quality: quality,
      includeBase64: includeBase64,
      exif: exif
    )
  }

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

  private func encodeAndWrite(
    _ image: UIImage,
    format: ImageFormat,
    quality: Double,
    includeBase64: Bool,
    suggestedName: String? = nil,
    exif: ExifPayload?
  ) -> AssetPayload? {
    guard let encoded = encode(image, format: format, quality: quality) else { return nil }
    let cgImage = image.cgImage
    let pixelWidth = cgImage?.width ?? Int(image.size.width)
    let pixelHeight = cgImage?.height ?? Int(image.size.height)
    return write(
      data: encoded.data,
      format: encoded.format,
      width: pixelWidth,
      height: pixelHeight,
      includeBase64: includeBase64,
      suggestedName: suggestedName,
      exif: exif
    )
  }

  private func write(
    data: Data,
    format: ImageFormat,
    width: Int,
    height: Int,
    includeBase64: Bool,
    suggestedName: String?,
    exif: ExifPayload?
  ) -> AssetPayload? {
    do {
      let url = try tempFiles.makeFileURL(fileExtension: format.fileExtension)
      try data.write(to: url)
      return AssetPayload(
        uri: url.absoluteString,
        mime: format.mime,
        fileName: AssetFileName.resolve(
          suggested: suggestedName,
          fallback: url.lastPathComponent,
          fileExtension: url.pathExtension
        ),
        fileSize: data.count,
        width: width,
        height: height,
        base64: includeBase64 ? data.base64EncodedString() : nil,
        exif: exif
      )
    } catch {
      NSLog("[ReactNativeMediaPicker] failed to write asset: %@", error.localizedDescription)
      return nil
    }
  }
}
