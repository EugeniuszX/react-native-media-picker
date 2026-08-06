import Foundation
import UIKit

/// The object the Objective-C++ TurboModule talks to. It owns the single-flight
/// session and the temp-file store; everything else is built per request.
@objc public final class PickerCoordinator: NSObject {
  private typealias Completion = ([[String: Any]]?, Bool, String?, String?) -> Void

  private let session = PickerSession<Completion>()
  private let tempFiles: TempFileStore
  private let processor: ImageProcessor

  @objc public override init() {
    let store = TempFileStore()
    tempFiles = store
    processor = ImageProcessor(tempFiles: store)
    super.init()
    // Sweep leftovers from previous runs. Anything recent is left alone in case
    // JS is still holding a URI from a reload-surviving pick.
    DispatchQueue.global(qos: .utility).async {
      store.removeFiles(olderThan: TempFileStore.autoSweepAge)
    }
  }

  @objc public func launchImageLibrary(
    selectionLimit: Int,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    includeBase64: Bool,
    completion: @escaping ([[String: Any]]?, Bool, String?, String?) -> Void
  ) {
    guard session.begin(completion) else {
      completion(nil, false, PickerError.others.code, "Already waiting for a pick.")
      return
    }
    let options = LibraryOptions(
      selectionLimit: selectionLimit,
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      quality: quality,
      includeBase64: includeBase64
    )
    LibraryPicker(options: options, processor: processor) {
      [weak self] assets, didCancel, error, message in
      self?.finish(assets, didCancel, error, message)
    }.present()
  }

  @objc public func launchCamera(
    cameraType: String,
    maxWidth: Int,
    maxHeight: Int,
    quality: Double,
    includeBase64: Bool,
    completion: @escaping ([[String: Any]]?, Bool, String?, String?) -> Void
  ) {
    guard session.begin(completion) else {
      completion(nil, false, PickerError.others.code, "Already waiting for a pick.")
      return
    }
    let options = CameraOptions(
      facing: CameraFacing.from(rawValue: cameraType),
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      quality: quality,
      includeBase64: includeBase64
    )
    let picker = CameraPicker(options: options, processor: processor) {
      [weak self] assets, didCancel, error, message in
      self?.finish(assets, didCancel, error, message)
    }
    if !picker.start() {
      finish(nil, false, .cameraUnavailable, "Camera is not available on this device")
    }
  }

  @objc public func cleanTempFiles() {
    let store = tempFiles
    DispatchQueue.global(qos: .utility).async {
      store.removeAll()
    }
  }

  /// Delivers the result to JS at most once, whichever queue we are on.
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
