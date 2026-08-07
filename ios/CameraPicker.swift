import AVFoundation
import UIKit

/// Presents `UIImagePickerController` for a capture, including the camera
/// permission dance. One instance per capture.
final class CameraPicker: NSObject, UIImagePickerControllerDelegate,
  UINavigationControllerDelegate
{
  /// Liveness backstop, not an expected duration — see the same constant on
  /// `LibraryPicker`. A `dismiss(animated:completion:)` that never calls back
  /// must degrade to a late resolution, never to a promise that strands the
  /// single-flight session for the lifetime of the process.
  static let dismissalTimeout: TimeInterval = 3

  private let options: CameraOptions
  private let processor: ImageProcessor
  private let finish: ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  private var selfReference: CameraPicker?

  init(
    options: CameraOptions,
    processor: ImageProcessor,
    finish: @escaping ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  ) {
    self.options = options
    self.processor = processor
    self.finish = finish
    super.init()
  }

  /// Returns false when the device has no camera at all, in which case nothing
  /// is presented and no completion is delivered by this object.
  func start() -> Bool {
    guard UIImagePickerController.isSourceTypeAvailable(.camera) else { return false }

    // From here on every exit runs through `complete`, so the self-reference is
    // always given up. It has to be taken before the permission request: the
    // coordinator drops its reference the moment `start` returns, and
    // `requestAccess` does not retain us either.
    selfReference = self

    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .authorized:
      present()
    case .notDetermined:
      AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
        guard let self else { return }
        if granted {
          self.present()
        } else {
          self.complete(nil, false, .permission, "Camera permission denied")
        }
      }
    case .denied, .restricted:
      complete(nil, false, .permission, "Camera permission denied")
    @unknown default:
      complete(nil, false, .permission, "Camera permission unavailable")
    }
    return true
  }

  private func present() {
    DispatchQueue.main.async { [self] in
      guard let presenter = ViewControllerLocator.topViewController() else {
        complete(nil, false, .others, "No view controller to present from")
        return
      }
      // The locator only ever hands back a controller whose presentation slot is
      // free or occupied by something mid-dismissal. In the latter case UIKit
      // would silently refuse to present, so fail loudly instead of wedging.
      guard presenter.presentedViewController == nil else {
        complete(nil, false, .others, "A view controller is already being presented")
        return
      }
      let picker = UIImagePickerController()
      picker.sourceType = .camera
      picker.delegate = self
      if options.facing == .front,
        UIImagePickerController.isCameraDeviceAvailable(.front)
      {
        picker.cameraDevice = .front
      }
      presenter.present(picker, animated: true)
    }
  }

  func imagePickerController(
    _ picker: UIImagePickerController,
    didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
  ) {
    // The promise must not resolve until the picker is fully off screen: JS
    // routinely chains another pick in .then(), and UIKit silently refuses to
    // present while a dismissal is still animating.
    let dismissed = DispatchGroup()
    dismissed.enter()
    picker.dismiss(animated: true) { dismissed.leave() }

    guard let image = info[.originalImage] as? UIImage else {
      // Bounded: a missing dismissal callback must degrade to a late resolution,
      // never to a promise that strands the single-flight session forever.
      DispatchQueue.global(qos: .userInitiated).async { [self] in
        _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
        complete(nil, false, .others, "Failed to capture image")
      }
      return
    }
    DispatchQueue.global(qos: .userInitiated).async { [self] in
      let payload = processor.process(
        capturedImage: image,
        maxWidth: options.maxWidth,
        maxHeight: options.maxHeight,
        quality: options.quality,
        includeBase64: options.includeBase64
      )
      // Safe to block here: this is a global queue, and the dismissal
      // completion is delivered on main, which nothing in this path holds.
      // Bounded so a missing dismissal callback cannot strand the session.
      _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
      guard let payload else {
        complete(nil, false, .others, "Failed to capture image")
        return
      }
      complete([payload], false, nil, nil)
    }
  }

  func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    // Same shape as the success path: resolve only once the picker is off
    // screen, but bounded, so a missing dismissal callback degrades to a late
    // resolution rather than stranding the single-flight session forever.
    let dismissed = DispatchGroup()
    dismissed.enter()
    picker.dismiss(animated: true) { dismissed.leave() }

    DispatchQueue.global(qos: .userInitiated).async { [self] in
      _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
      complete(nil, true, nil, nil)
    }
  }

  private func complete(
    _ assets: [AssetPayload]?,
    _ didCancel: Bool,
    _ error: PickerError?,
    _ message: String?
  ) {
    selfReference = nil
    finish(assets, didCancel, error, message)
  }
}
