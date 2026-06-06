import Foundation
import PhotosUI
import UIKit

@objc public class MediaPickerImpl: NSObject, PHPickerViewControllerDelegate {

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
    var assets: [[String: Any]] = []
    let lock = NSLock()

    for result in results {
      group.enter()
      let provider = result.itemProvider
      provider.loadObject(ofClass: UIImage.self) { object, _ in
        defer { group.leave() }
        guard let image = object as? UIImage else { return }
        if let asset = self.processImage(image) {
          lock.lock()
          assets.append(asset)
          lock.unlock()
        }
      }
    }

    group.notify(queue: .main) {
      self.finish(assets, false, nil, nil)
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

    var asset: [String: Any] = [
      "uri": fileURL.absoluteString,
      "type": "image/jpeg",
      "fileName": fileName,
      "fileSize": data.count,
      "width": Int(resized.size.width * resized.scale),
      "height": Int(resized.size.height * resized.scale),
    ]
    if includeBase64 {
      asset["base64"] = data.base64EncodedString()
    }
    return asset
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
