import Foundation

/// Single-flight gate for the one pick that may be in progress. `begin` and
/// `end` are atomic, which is what makes "at most one pick" and "exactly one
/// completion" true even though pickers call back from arbitrary queues.
final class PickerSession<Completion> {
  private let lock = NSLock()
  private var completion: Completion?

  /// Claims the session. Returns false when a pick is already in flight, in
  /// which case `completion` is not stored.
  func begin(_ completion: Completion) -> Bool {
    lock.lock()
    defer { lock.unlock() }
    guard self.completion == nil else { return false }
    self.completion = completion
    return true
  }

  /// Hands the stored completion back to the first caller and clears the
  /// session. Every later call returns nil.
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
