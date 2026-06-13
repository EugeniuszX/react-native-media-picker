import AVFoundation
import Foundation
import ImageIO
import PhotosUI
import UIKit
import UniformTypeIdentifiers

@objc public class MediaPickerImpl: NSObject, PHPickerViewControllerDelegate,
  UIImagePickerControllerDelegate, UINavigationControllerDelegate {

  private var resolve: (([[String: Any]]?, Bool, String?, String?) -> Void)?
  private var maxWidth: CGFloat = 0
  private var maxHeight: CGFloat = 0
  private var quality: CGFloat = 1
  private var includeBase64: Bool = false

  // completion: (assets, didCancel, errorCode, errorMessage)
  @objc public func launchImageLibrary(
    _ selectionLimit: Int,
    maxWidth: Double,
    maxHeight: Double,
    quality: Double,
    includeBase64: Bool,
    completion: @escaping ([[String: Any]]?, Bool, String?, String?) -> Void
  ) {
    if resolve != nil {
      completion(nil, false, "others", "Already waiting for an image pick.")
      return
    }

    self.resolve = completion
    self.maxWidth = CGFloat(maxWidth)
    self.maxHeight = CGFloat(maxHeight)
    self.quality = CGFloat(quality)
    self.includeBase64 = includeBase64

    var config = PHPickerConfiguration()
    config.filter = .images
    config.selectionLimit = selectionLimit // 0 = unlimited

    DispatchQueue.main.async {
      let picker = PHPickerViewController(configuration: config)
      picker.delegate = self
      guard let presenter = Self.topViewController() else {
        self.finish(nil, false, "others", "No view controller to present from")
        return
      }
      presenter.present(picker, animated: true)
    }
  }

  @objc public func launchCamera(
    _ cameraType: String,
    maxWidth: Double,
    maxHeight: Double,
    quality: Double,
    includeBase64: Bool,
    completion: @escaping ([[String: Any]]?, Bool, String?, String?) -> Void
  ) {
    if resolve != nil {
      completion(nil, false, "others", "Already waiting for a pick.")
      return
    }
    guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
      completion(nil, false, "camera_unavailable", "Camera is not available on this device")
      return
    }

    self.resolve = completion
    self.maxWidth = CGFloat(maxWidth)
    self.maxHeight = CGFloat(maxHeight)
    self.quality = CGFloat(quality)
    self.includeBase64 = includeBase64

    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .authorized:
      presentCamera(cameraType: cameraType)
    case .notDetermined:
      AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
        guard let self else { return }
        if granted {
          self.presentCamera(cameraType: cameraType)
        } else {
          self.finish(nil, false, "permission", "Camera permission denied")
        }
      }
    case .denied, .restricted:
      finish(nil, false, "permission", "Camera permission denied")
    @unknown default:
      finish(nil, false, "permission", "Camera permission unavailable")
    }
  }

  private func presentCamera(cameraType: String) {
    DispatchQueue.main.async {
      let picker = UIImagePickerController()
      picker.sourceType = .camera
      picker.delegate = self
      if cameraType == "front", UIImagePickerController.isCameraDeviceAvailable(.front) {
        picker.cameraDevice = .front
      }
      guard let presenter = Self.topViewController() else {
        self.finish(nil, false, "others", "No view controller to present from")
        return
      }
      presenter.present(picker, animated: true)
    }
  }

  public func imagePickerController(
    _ picker: UIImagePickerController,
    didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
  ) {
    picker.dismiss(animated: true)
    guard let image = info[.originalImage] as? UIImage else {
      finish(nil, false, "others", "Failed to capture image")
      return
    }
    DispatchQueue.global(qos: .userInitiated).async { [weak self] in
      guard let self else { return }
      guard let asset = self.processImage(image) else {
        self.finish(nil, false, "others", "Failed to capture image")
        return
      }
      self.finish([asset], false, nil, nil)
    }
  }

  public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    picker.dismiss(animated: true)
    finish(nil, true, nil, nil)
  }

  public func picker(
    _ picker: PHPickerViewController,
    didFinishPicking results: [PHPickerResult]
  ) {
    picker.dismiss(animated: true)

    if results.isEmpty {
      finish(nil, true, nil, nil)
      return
    }

    let group = DispatchGroup()
    // Pre-sized, index-addressed slots preserve the user's selection order even
    // though load completions arrive out of order.
    var slots = [[String: Any]?](repeating: nil, count: results.count)
    let lock = NSLock()

    // TODO(phase-2): cap concurrency for large unlimited selections to bound peak memory.
    // NOTE(phase-2): loadDataRepresentation fires for every selected item with no
    // concurrency cap, and each item now holds original bytes + a decoded UIImage
    // (resize path) + encoded output at once — materially higher peak memory than
    // the old decoded-bitmap-only path. Cap concurrency for large unlimited picks.
    for (index, result) in results.enumerated() {
      let provider = result.itemProvider
      guard let uti = provider.registeredTypeIdentifiers.first(where: {
        UTType($0)?.conforms(to: .image) == true
      }) else {
        NSLog("[ReactNativeMediaPicker] skipping item at index \(index): no image UTI")
        continue
      }
      group.enter()
      provider.loadDataRepresentation(forTypeIdentifier: uti) { [weak self] data, error in
        defer { group.leave() }
        guard let self else { return }
        if let error {
          NSLog("[ReactNativeMediaPicker] failed to load image: %@", error.localizedDescription)
          return
        }
        guard let data, let asset = self.processData(data, uti: uti) else { return }
        lock.lock()
        slots[index] = asset
        lock.unlock()
      }
    }

    group.notify(queue: .main) { [weak self] in
      guard let self else { return }
      let assets = slots.compactMap { $0 }
      if assets.isEmpty {
        // Selection was non-empty but every item failed to load/process.
        self.finish(nil, false, "others", "Failed to load the selected image(s).")
      } else {
        self.finish(assets, false, nil, nil)
      }
    }
  }

  // MARK: - Format helpers

  private func mime(forUTI uti: String) -> String {
    switch uti {
    case "public.png": return "image/png"
    case "public.heic", "public.heif": return "image/heic"
    case "com.compuserve.gif": return "image/gif"
    case "org.webmproject.webp": return "image/webp"
    default: return "image/jpeg"
    }
  }

  private func ext(forMime mime: String) -> String {
    switch mime {
    case "image/png": return "png"
    case "image/heic": return "heic"
    case "image/gif": return "gif"
    case "image/webp": return "webp"
    default: return "jpg"
    }
  }

  /// Animated-WebP detection via the RIFF/VP8X header (animation = flag bit 0x02
  /// at offset 20). GIF is always treated as animated by the caller.
  private func isAnimatedWebp(_ data: Data) -> Bool {
    guard data.count >= 21 else { return false }
    let b = [UInt8](data.prefix(21))
    guard b[0] == 0x52, b[1] == 0x49, b[2] == 0x46, b[3] == 0x46, // RIFF
          b[8] == 0x57, b[9] == 0x45, b[10] == 0x42, b[11] == 0x50, // WEBP
          b[12] == 0x56, b[13] == 0x50, b[14] == 0x38, b[15] == 0x58 // VP8X
    else { return false }
    return (b[20] & 0x02) != 0
  }

  // MARK: - Library pipeline (preserves source format)

  /// Builds an asset dict from raw picked bytes, deciding passthrough vs transform.
  private func processData(_ data: Data, uti: String) -> [String: Any]? {
    let srcMime = mime(forUTI: uti)
    let animated = srcMime == "image/gif" || (srcMime == "image/webp" && isAnimatedWebp(data))

    if animated {
      return writePassthrough(data, mime: srcMime)
    }

    // No resize bound → return original bytes without decoding to a bitmap.
    guard maxWidth > 0 || maxHeight > 0 else {
      return writePassthrough(data, mime: srcMime)
    }

    guard let image = UIImage(data: data) else { return nil }
    let needsResize =
      image.size.width > effectiveMaxWidth(image) || image.size.height > effectiveMaxHeight(image)

    if !needsResize {
      return writePassthrough(data, mime: srcMime)
    }
    return writeTransformed(image, srcMime: srcMime)
  }

  private func effectiveMaxWidth(_ image: UIImage) -> CGFloat {
    maxWidth > 0 ? maxWidth : image.size.width
  }

  private func effectiveMaxHeight(_ image: UIImage) -> CGFloat {
    maxHeight > 0 ? maxHeight : image.size.height
  }

  /// Writes original encoded bytes verbatim; reports the source format.
  private func writePassthrough(_ data: Data, mime: String) -> [String: Any]? {
    let fileExt = ext(forMime: mime)
    let fileName = "media_picker_\(UUID().uuidString).\(fileExt)"
    let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
    do {
      try data.write(to: fileURL)
    } catch {
      return nil
    }
    let source = CGImageSourceCreateWithData(data as CFData, nil)
    let props = source.flatMap {
      CGImageSourceCopyPropertiesAtIndex($0, 0, nil) as? [CFString: Any]
    }
    let pixelWidth = (props?[kCGImagePropertyPixelWidth] as? Int) ?? 0
    let pixelHeight = (props?[kCGImagePropertyPixelHeight] as? Int) ?? 0
    if pixelWidth == 0 || pixelHeight == 0 {
      NSLog("[ReactNativeMediaPicker] warning: could not read image dimensions from source")
    }
    // EXIF orientations 5–8 are the 90°/270° rotations that transpose the stored
    // buffer's axes; swap so reported dimensions match how the image displays
    // (mirrors the Android passthrough path).
    let orientation = (props?[kCGImagePropertyOrientation] as? NSNumber)?.intValue ?? 1
    let axisSwapped = (5...8).contains(orientation)
    let width = axisSwapped ? pixelHeight : pixelWidth
    let height = axisSwapped ? pixelWidth : pixelHeight

    var asset: [String: Any] = [
      "uri": fileURL.absoluteString,
      "type": mime,
      "fileName": fileName,
      "fileSize": data.count,
      "width": width,
      "height": height,
    ]
    if includeBase64 {
      asset["base64"] = data.base64EncodedString()
    }
    return asset
  }

  /// Resizes and re-encodes in the source format; HEIC stays HEIC, WebP → JPEG.
  private func writeTransformed(_ image: UIImage, srcMime: String) -> [String: Any]? {
    let resized = resizeToFit(image)

    let outMime: String
    let encoded: Data?
    switch srcMime {
    case "image/png":
      outMime = "image/png"
      encoded = resized.pngData()
    case "image/heic":
      if let heic = heicData(from: resized, quality: quality) {
        outMime = "image/heic"
        encoded = heic
      } else {
        // HEIC encoder unavailable (Simulator, pre-A10 devices) — fall back to
        // JPEG, mirroring Android. ext(forMime:) keeps file/type consistent.
        outMime = "image/jpeg"
        encoded = resized.jpegData(compressionQuality: quality)
      }
    default: // jpeg, and webp (no iOS encoder) → jpeg fallback
      outMime = "image/jpeg"
      encoded = resized.jpegData(compressionQuality: quality)
    }
    guard let data = encoded else { return nil }

    let fileExt = ext(forMime: outMime)
    let fileName = "media_picker_\(UUID().uuidString).\(fileExt)"
    let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
    do {
      try data.write(to: fileURL)
    } catch {
      return nil
    }
    let (pixelWidth, pixelHeight) = pixelSize(of: resized)

    var asset: [String: Any] = [
      "uri": fileURL.absoluteString,
      "type": outMime,
      "fileName": fileName,
      "fileSize": data.count,
      "width": pixelWidth,
      "height": pixelHeight,
    ]
    if includeBase64 {
      asset["base64"] = data.base64EncodedString()
    }
    return asset
  }

  private func heicData(from image: UIImage, quality: CGFloat) -> Data? {
    guard let cg = image.cgImage else { return nil }
    let out = NSMutableData()
    guard let dest = CGImageDestinationCreateWithData(
      out, "public.heic" as CFString, 1, nil
    ) else { return nil }
    let options = [kCGImageDestinationLossyCompressionQuality: quality] as CFDictionary
    CGImageDestinationAddImage(dest, cg, options)
    guard CGImageDestinationFinalize(dest) else { return nil }
    return out as Data
  }

  private func processImage(_ image: UIImage) -> [String: Any]? {
    let resized = resizeToFit(image)
    guard let data = resized.jpegData(compressionQuality: quality) else { return nil }

    let fileName = "media_picker_\(UUID().uuidString).jpg"
    let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
    do {
      try data.write(to: fileURL)
    } catch {
      return nil
    }

    let (pixelWidth, pixelHeight) = pixelSize(of: resized)

    var asset: [String: Any] = [
      "uri": fileURL.absoluteString,
      "type": "image/jpeg",
      "fileName": fileName,
      "fileSize": data.count,
      "width": pixelWidth,
      "height": pixelHeight,
    ]
    if includeBase64 {
      asset["base64"] = data.base64EncodedString()
    }
    return asset
  }

  private func pixelSize(of image: UIImage) -> (width: Int, height: Int) {
    guard let cg = image.cgImage else {
      return (Int(image.size.width * image.scale), Int(image.size.height * image.scale))
    }
    switch image.imageOrientation {
    case .left, .leftMirrored, .right, .rightMirrored:
      // 90°/270°: the encoded (orientation-applied) image swaps the raw buffer's axes.
      return (cg.height, cg.width)
    default:
      return (cg.width, cg.height)
    }
  }

  private func resizeToFit(_ image: UIImage) -> UIImage {
    let w = image.size.width
    let h = image.size.height
    let maxW = maxWidth > 0 ? maxWidth : w
    let maxH = maxHeight > 0 ? maxHeight : h
    if w <= maxW && h <= maxH { return image }

    let ratio = min(maxW / w, maxH / h)
    let newSize = CGSize(width: w * ratio, height: h * ratio)
    let format = UIGraphicsImageRendererFormat.default()
    format.scale = 1
    let renderer = UIGraphicsImageRenderer(size: newSize, format: format)
    return renderer.image { _ in
      image.draw(in: CGRect(origin: .zero, size: newSize))
    }
  }

  private func finish(
    _ assets: [[String: Any]]?,
    _ didCancel: Bool,
    _ errorCode: String?,
    _ errorMessage: String?
  ) {
    let cb = resolve
    resolve = nil
    cb?(assets, didCancel, errorCode, errorMessage)
  }

  private static func topViewController() -> UIViewController? {
    let scenes = UIApplication.shared.connectedScenes
    let windowScene = scenes.first { $0.activationState == .foregroundActive } as? UIWindowScene
    var top = windowScene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
    while let presented = top?.presentedViewController {
      top = presented
    }
    return top
  }
}
