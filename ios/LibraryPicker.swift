import PhotosUI
import UIKit
import UniformTypeIdentifiers

/// Presents `PHPickerViewController` and turns the selection into assets.
/// One instance per pick; it retains itself as the picker's delegate until the
/// selection is resolved.
final class LibraryPicker: NSObject, PHPickerViewControllerDelegate {
  /// Items loaded in parallel. Each in-flight item holds original bytes plus,
  /// on the transform path, a decoded bitmap and the encoded output — so this
  /// is the knob that bounds peak memory on large selections.
  static let maxConcurrentItemLoads = 4

  private let options: LibraryOptions
  private let processor: ImageProcessor
  private let finish: ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  private var selfReference: LibraryPicker?

  init(
    options: LibraryOptions,
    processor: ImageProcessor,
    finish: @escaping ([AssetPayload]?, Bool, PickerError?, String?) -> Void
  ) {
    self.options = options
    self.processor = processor
    self.finish = finish
    super.init()
  }

  func present() {
    var configuration = PHPickerConfiguration()
    configuration.filter = .images
    configuration.selectionLimit = options.selectionLimit  // 0 = unlimited

    // Nothing else owns this object: the coordinator drops its reference as soon
    // as `present` returns, and `PHPickerViewController` holds its delegate
    // weakly. `complete` is the only place the reference is given up.
    selfReference = self

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
      let picker = PHPickerViewController(configuration: configuration)
      picker.delegate = self
      presenter.present(picker, animated: true)
    }
  }

  func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
    // The promise must not resolve until the picker is fully off screen: JS
    // routinely chains another pick in .then(), and UIKit silently refuses to
    // present while a dismissal is still animating.
    let dismissed = DispatchGroup()
    dismissed.enter()
    picker.dismiss(animated: true) { dismissed.leave() }

    guard !results.isEmpty else {
      dismissed.notify(queue: .main) { [self] in complete(nil, true, nil, nil) }
      return
    }

    // Pre-sized, index-addressed slots preserve the user's selection order even
    // though load completions arrive out of order.
    var slots = [AssetPayload?](repeating: nil, count: results.count)
    let lock = NSLock()
    let group = DispatchGroup()
    let semaphore = DispatchSemaphore(value: Self.maxConcurrentItemLoads)

    // The loop blocks on the semaphore, so it must not run on the main queue.
    DispatchQueue.global(qos: .userInitiated).async { [self] in
      for (index, result) in results.enumerated() {
        let provider = result.itemProvider
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
              maxWidth: self.options.maxWidth,
              maxHeight: self.options.maxHeight,
              quality: self.options.quality,
              includeBase64: self.options.includeBase64
            )
          else { return }
          lock.lock()
          slots[index] = payload
          lock.unlock()
        }
      }

      group.wait()
      // Safe to block here: this is a global queue, and the dismissal
      // completion is delivered on main, which nothing in this path holds.
      // Processing and the animation overlap rather than serialize.
      dismissed.wait()
      let assets = slots.compactMap { $0 }
      if assets.isEmpty {
        // The selection was non-empty but nothing survived loading/processing.
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
