import AVFoundation
import UIKit
import UniformTypeIdentifiers

final class CameraPicker: NSObject, UIImagePickerControllerDelegate,
  UINavigationControllerDelegate
{
  static let dismissalTimeout: TimeInterval = 3

  private let options: CameraOptions
  private let processor: ImageProcessor
  private let videoProcessor: VideoProcessor
  private let finish: ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  /// Kept alive until completion — taken before the permission prompt, not after.
  private var selfReference: CameraPicker?

  init(
    options: CameraOptions,
    processor: ImageProcessor,
    videoProcessor: VideoProcessor,
    finish: @escaping ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  ) {
    self.options = options
    self.processor = processor
    self.videoProcessor = videoProcessor
    self.finish = finish
    super.init()
  }

  func start() -> Bool {
    guard UIImagePickerController.isSourceTypeAvailable(.camera) else { return false }
    // A camera source can exist while movie capture does not — the iOS simulator is exactly that
    // case, and so are some managed configurations. `setMediaTypes:` raises
    // `NSInvalidArgumentException` ("No available types for source 1") for a type the source does
    // not offer, and an Objective-C exception cannot be caught from Swift: the process aborts.
    // So the video branch is gated on the media type as well as on the source, and reports the
    // same `camera_unavailable` the missing-camera path does.
    if options.mediaType == .video,
      UIImagePickerController.availableMediaTypes(for: .camera)?
        .contains(UTType.movie.identifier) != true
    {
      return false
    }

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
      guard presenter.presentedViewController == nil else {
        complete(nil, false, .others, "A view controller is already being presented")
        return
      }
      let picker = UIImagePickerController()
      picker.sourceType = .camera
      picker.delegate = self
      if options.mediaType == .video {
        picker.mediaTypes = [UTType.movie.identifier]
        picker.videoQuality = Self.quality(for: options.videoQuality)
        // 0 means "leave the platform default", which on iOS is a 10-minute ceiling.
        if options.maxDuration > 0 {
          picker.videoMaximumDuration = TimeInterval(options.maxDuration)
        }
      }
      if options.facing == .front,
        UIImagePickerController.isCameraDeviceAvailable(.front)
      {
        picker.cameraDevice = .front
      }
      presenter.present(picker, animated: true)
    }
  }

  private static func quality(
    for quality: VideoQuality
  ) -> UIImagePickerController.QualityType {
    switch quality {
    case .low: return .typeLow
    case .medium: return .typeMedium
    case .high: return .typeHigh
    }
  }

  func imagePickerController(
    _ picker: UIImagePickerController,
    didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
  ) {
    let dismissed = DispatchGroup()
    dismissed.enter()
    picker.dismiss(animated: true) { dismissed.leave() }

    if options.mediaType == .video {
      guard let url = info[.mediaURL] as? URL else {
        DispatchQueue.global(qos: .userInitiated).async { [self] in
          _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
          complete(nil, false, .others, "Failed to record video")
        }
        return
      }
      DispatchQueue.global(qos: .userInitiated).async { [self] in
        let payload = videoProcessor.process(
          sourceURL: url,
          uti: UTType.quickTimeMovie.identifier,
          includeThumbnail: options.includeThumbnail
        )
        // The recording is written straight into `NSTemporaryDirectory()` by the system picker,
        // one level outside `tmp/rn-media-picker` — so neither the 24-hour sweep nor
        // `cleanTempFiles()` would ever reach it. `process` has already copied it into the store
        // and the returned uri points at that copy, so the original is ours to drop, on the
        // failure path as much as on the success one.
        try? FileManager.default.removeItem(at: url)
        _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
        guard let payload else {
          complete(nil, false, .others, "Failed to record video")
          return
        }
        complete([payload], false, nil, nil)
      }
      return
    }

    guard let image = info[.originalImage] as? UIImage else {
      DispatchQueue.global(qos: .userInitiated).async { [self] in
        _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
        complete(nil, false, .others, "Failed to capture image")
      }
      return
    }
    DispatchQueue.global(qos: .userInitiated).async { [self] in
      // `stripMetadata` is a no-op here: the capture is re-encoded from raw pixels, so the
      // written file never carries EXIF in the first place. Reported EXIF comes from the
      // picker's own metadata dictionary, not from the file.
      let exif =
        options.includeExif
        ? ExifReader.read(
          properties: info[.mediaMetadata] as? [CFString: Any])
        : nil
      let payload = processor.process(
        capturedImage: image,
        requested: options.format,
        maxWidth: options.maxWidth,
        maxHeight: options.maxHeight,
        quality: options.quality,
        includeBase64: options.includeBase64,
        exif: exif
      )
      _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
      guard let payload else {
        complete(nil, false, .others, "Failed to capture image")
        return
      }
      complete([payload], false, nil, nil)
    }
  }

  func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
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
