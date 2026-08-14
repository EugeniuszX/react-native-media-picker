import AVFoundation
import Foundation
import UIKit

@objc public final class PickerCoordinator: NSObject {
  private typealias Completion = ([[String: Any]]?, Bool, String?, String?) -> Void

  private let session = PickerSession<Completion>()
  private let tempFiles: TempFileStore
  private let processor: ImageProcessor
  private let videoProcessor: VideoProcessor

  @objc public override init() {
    let store = TempFileStore()
    tempFiles = store
    processor = ImageProcessor(tempFiles: store)
    videoProcessor = VideoProcessor(tempFiles: store)
    super.init()
    DispatchQueue.global(qos: .utility).async {
      store.removeFiles(olderThan: TempFileStore.autoSweepAge)
    }
  }

  @objc public func launchImageLibrary(
    selectionLimit: Int,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    format: String,
    mediaType: String,
    includeBase64: Bool,
    includeThumbnail: Bool,
    completion: @escaping ([[String: Any]]?, Bool, String?, String?) -> Void
  ) {
    guard session.begin(completion) else {
      completion(nil, false, PickerError.busy.code, "Already waiting for a pick.")
      return
    }
    let options = LibraryOptions(
      selectionLimit: selectionLimit,
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      quality: quality,
      includeBase64: includeBase64,
      format: RequestedFormat.from(rawValue: format),
      mediaType: RequestedMediaType.from(rawValue: mediaType),
      includeThumbnail: includeThumbnail
    )
    LibraryPicker(options: options, processor: processor, videoProcessor: videoProcessor) {
      [weak self] assets, didCancel, error, message in
      self?.finish(assets, didCancel, error, message)
    }.present()
  }

  @objc public func launchCamera(
    cameraType: String,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    format: String,
    includeBase64: Bool,
    completion: @escaping ([[String: Any]]?, Bool, String?, String?) -> Void
  ) {
    guard session.begin(completion) else {
      completion(nil, false, PickerError.busy.code, "Already waiting for a pick.")
      return
    }
    let options = CameraOptions(
      facing: CameraFacing.from(rawValue: cameraType),
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      quality: quality,
      includeBase64: includeBase64,
      format: RequestedFormat.from(rawValue: format)
    )
    let picker = CameraPicker(options: options, processor: processor) {
      [weak self] assets, didCancel, error, message in
      self?.finish(assets, didCancel, error, message)
    }
    if !picker.start() {
      finish(nil, false, .cameraUnavailable, "Camera is not available on this device")
    }
  }

  @objc public func getCameraPermissionStatus() -> String {
    Self.currentPermission().rawValue
  }

  @objc public func requestCameraPermission(completion: @escaping (String) -> Void) {
    guard Self.currentPermission() == .notDetermined else {
      completion(Self.currentPermission().rawValue)
      return
    }
    AVCaptureDevice.requestAccess(for: .video) { _ in
      completion(Self.currentPermission().rawValue)
    }
  }

  private static func currentPermission() -> CameraPermission {
    CameraPermission.resolve(
      hasCamera: AVCaptureDevice.default(for: .video) != nil,
      authorization: authorization(of: AVCaptureDevice.authorizationStatus(for: .video))
    )
  }

  private static func authorization(of status: AVAuthorizationStatus) -> CameraAuthorization {
    switch status {
    case .authorized: return .authorized
    case .notDetermined: return .notDetermined
    case .denied: return .denied
    case .restricted: return .restricted
    @unknown default: return .unknown
    }
  }

  @objc public func cleanTempFiles(completion: @escaping (Int) -> Void) {
    let store = tempFiles
    DispatchQueue.global(qos: .utility).async {
      completion(store.removeAll())
    }
  }

  @objc public func releaseAssets(_ uris: [Any], completion: @escaping (Int) -> Void) {
    let names = uris.compactMap { $0 as? String }
    guard !names.isEmpty else {
      completion(0)
      return
    }
    let store = tempFiles
    DispatchQueue.global(qos: .utility).async {
      completion(store.remove(uris: names))
    }
  }

  private func finish(
    _ assets: [AssetPayload]?,
    _ didCancel: Bool,
    _ error: PickerError?,
    _ message: String?
  ) {
    guard let completion = session.end() else { return }
    completion(assets?.map(\.dictionary), didCancel, error?.code, message)
  }
}
