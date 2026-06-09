import AVFoundation
import Foundation
import PhotosUI
import UIKit

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
    // though loadObject completions arrive out of order.
    var slots = [[String: Any]?](repeating: nil, count: results.count)
    let lock = NSLock()

    // TODO(phase-2): cap concurrency for large unlimited selections to bound peak memory.
    for (index, result) in results.enumerated() {
      let provider = result.itemProvider
      guard provider.canLoadObject(ofClass: UIImage.self) else { continue }
      group.enter()
      provider.loadObject(ofClass: UIImage.self) { [weak self] object, error in
        defer { group.leave() }
        guard let self else { return }
        if let error {
          NSLog("[ReactNativeMediaPicker] failed to load image: %@", error.localizedDescription)
          return
        }
        guard let image = object as? UIImage else { return }
        if let asset = self.processImage(image) {
          lock.lock()
          slots[index] = asset
          lock.unlock()
        }
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
