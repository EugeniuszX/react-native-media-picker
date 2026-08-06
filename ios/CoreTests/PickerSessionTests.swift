import XCTest

@testable import MediaPickerCore

final class PickerSessionTests: XCTestCase {
  func testFirstBeginWinsAndSecondIsRejected() {
    let session = PickerSession<String>()
    XCTAssertTrue(session.begin("first"))
    XCTAssertFalse(session.begin("second"))
    XCTAssertEqual(session.end(), "first")
  }

  func testEndYieldsTheCompletionExactlyOnce() {
    let session = PickerSession<String>()
    XCTAssertTrue(session.begin("only"))
    XCTAssertEqual(session.end(), "only")
    XCTAssertNil(session.end())
  }

  func testSessionIsReusableAfterEnd() {
    let session = PickerSession<String>()
    XCTAssertTrue(session.begin("first"))
    _ = session.end()
    XCTAssertTrue(session.begin("second"))
    XCTAssertEqual(session.end(), "second")
  }

  func testIsActiveReflectsTheClaim() {
    let session = PickerSession<String>()
    XCTAssertFalse(session.isActive)
    _ = session.begin("x")
    XCTAssertTrue(session.isActive)
    _ = session.end()
    XCTAssertFalse(session.isActive)
  }

  /// Concurrent claims must not both succeed — this is the race the old
  /// `if resolve != nil` check-then-act guard allowed.
  func testOnlyOneOfManyConcurrentBeginsSucceeds() {
    for trial in 0..<Self.trials {
      let session = PickerSession<Int>()
      let winners = Counter()
      runConcurrently { index in
        if session.begin(index) {
          winners.increment()
        }
      }
      XCTAssertEqual(winners.value, 1, "trial \(trial)")
    }
  }

  /// Concurrent completions must hand the payload to exactly one caller.
  func testOnlyOneOfManyConcurrentEndsReceivesTheCompletion() {
    for trial in 0..<Self.trials {
      let session = PickerSession<Int>()
      XCTAssertTrue(session.begin(7))
      let received = Counter()
      runConcurrently { _ in
        if session.end() != nil {
          received.increment()
        }
      }
      XCTAssertEqual(received.value, 1, "trial \(trial)")
    }
  }

  // MARK: - Contention harness

  private static let workers = 8
  /// Measured against an unwidened check-then-act mutant: at 200 trials each
  /// test individually false-greened about 1 run in 10; at 1000 both were red
  /// 6/6 with 8-26 failing trials, for ~0.5s of runtime.
  private static let trials = 1000

  /// Parks `workers` threads on a common barrier, releases them together, and
  /// waits for all of them.
  ///
  /// Real `Thread`s rather than `DispatchQueue.global().async`: the global pool
  /// grows lazily, so dispatched blocks can finish before their siblings are
  /// even enqueued. A "concurrency" test that never actually overlaps would
  /// pass against the very check-then-act race it exists to catch — which is
  /// why this runs many short trials instead of one wide unsynchronized burst.
  private func runConcurrently(_ body: @escaping (Int) -> Void) {
    let ready = DispatchSemaphore(value: 0)
    let start = DispatchSemaphore(value: 0)
    let done = DispatchSemaphore(value: 0)
    for index in 0..<Self.workers {
      Thread {
        ready.signal()
        start.wait()
        body(index)
        done.signal()
      }.start()
    }
    for _ in 0..<Self.workers { ready.wait() }
    for _ in 0..<Self.workers { start.signal() }
    for _ in 0..<Self.workers { done.wait() }
  }
}

/// Lock-protected tally. The tests count winners across threads, so the counter
/// itself must not be the thing that races.
private final class Counter {
  private let lock = NSLock()
  private var count = 0

  func increment() {
    lock.lock()
    count += 1
    lock.unlock()
  }

  var value: Int {
    lock.lock()
    defer { lock.unlock() }
    return count
  }
}
