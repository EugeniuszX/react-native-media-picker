import Foundation

final class PickerSession<Completion> {
  private let lock = NSLock()
  private var completion: Completion?

  func begin(_ completion: Completion) -> Bool {
    lock.lock()
    defer { lock.unlock() }
    guard self.completion == nil else { return false }
    self.completion = completion
    return true
  }

  func end() -> Completion? {
    lock.lock()
    defer { lock.unlock() }
    let stored = completion
    completion = nil
    return stored
  }

  var isActive: Bool {
    lock.lock()
    defer { lock.unlock() }
    return completion != nil
  }
}
