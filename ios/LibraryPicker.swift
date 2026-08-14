import PhotosUI
import UIKit
import UniformTypeIdentifiers

final class LibraryPicker: NSObject, PHPickerViewControllerDelegate {
  static let maxConcurrentItemLoads = 4

  static let dismissalTimeout: TimeInterval = 3

  private let options: LibraryOptions
  private let processor: ImageProcessor
  private let videoProcessor: VideoProcessor
  private let finish: ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  /// Kept alive until completion — PHPickerViewController holds its delegate weakly.
  private var selfReference: LibraryPicker?

  init(
    options: LibraryOptions,
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

  func present() {
    var configuration = PHPickerConfiguration()
    switch options.mediaType {
    case .photo: configuration.filter = .images
    case .video: configuration.filter = .videos
    case .mixed: configuration.filter = .any(of: [.images, .videos])
    }
    configuration.selectionLimit = options.selectionLimit

    selfReference = self

    DispatchQueue.main.async { [self] in
      guard let presenter = ViewControllerLocator.topViewController() else {
        complete(nil, false, .others, "No view controller to present from")
        return
      }
      guard presenter.presentedViewController == nil else {
        complete(nil, false, .others, "A view controller is already being presented")
        return
      }
      let picker = PHPickerViewController(configuration: configuration)
      picker.delegate = self
      presenter.present(picker, animated: true)
    }
  }

  func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
    let dismissed = DispatchGroup()
    dismissed.enter()
    picker.dismiss(animated: true) { dismissed.leave() }

    guard !results.isEmpty else {
      DispatchQueue.global(qos: .userInitiated).async { [self] in
        _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
        complete(nil, true, nil, nil)
      }
      return
    }

    var slots = [AssetPayload?](repeating: nil, count: results.count)
    let lock = NSLock()
    let group = DispatchGroup()
    let semaphore = DispatchSemaphore(value: Self.maxConcurrentItemLoads)

    DispatchQueue.global(qos: .userInitiated).async { [self] in
      for (index, result) in results.enumerated() {
        let provider = result.itemProvider
        if options.mediaType != .photo,
          let movieUTI = provider.registeredTypeIdentifiers.first(where: {
            UTType($0)?.conforms(to: .movie) == true
          })
        {
          semaphore.wait()
          group.enter()
          provider.loadFileRepresentation(forTypeIdentifier: movieUTI) { url, error in
            defer {
              semaphore.signal()
              group.leave()
            }
            if let error {
              NSLog(
                "[ReactNativeMediaPicker] failed to load video %d: %@", index,
                error.localizedDescription)
              return
            }
            guard let url,
              let payload = self.videoProcessor.process(
                sourceURL: url,
                uti: movieUTI,
                includeThumbnail: self.options.includeThumbnail,
                suggestedName: provider.suggestedName
              )
            else { return }
            lock.lock()
            slots[index] = payload
            lock.unlock()
          }
          continue
        }
        guard
          let uti = provider.registeredTypeIdentifiers.first(where: {
            UTType($0)?.conforms(to: .image) == true
          })
        else {
          NSLog("[ReactNativeMediaPicker] skipping item %d: no image UTI", index)
          continue
        }

        semaphore.wait()
        group.enter()
        provider.loadDataRepresentation(forTypeIdentifier: uti) { data, error in
          defer {
            semaphore.signal()
            group.leave()
          }
          if let error {
            NSLog(
              "[ReactNativeMediaPicker] failed to load item %d: %@", index,
              error.localizedDescription)
            return
          }
          guard let data,
            let payload = self.processor.process(
              data: data,
              format: ImageFormat.from(uti: uti),
              requested: self.options.format,
              maxWidth: self.options.maxWidth,
              maxHeight: self.options.maxHeight,
              quality: self.options.quality,
              includeBase64: self.options.includeBase64,
              suggestedName: provider.suggestedName
            )
          else { return }
          lock.lock()
          slots[index] = payload
          lock.unlock()
        }
      }

      group.wait()
      _ = dismissed.wait(timeout: .now() + Self.dismissalTimeout)
      let assets = slots.compactMap { $0 }
      if assets.isEmpty {
        complete(nil, false, .others, "Failed to load the selected image(s).")
      } else {
        complete(assets, false, nil, nil)
      }
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
