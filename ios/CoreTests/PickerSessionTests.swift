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

  private static let workers = 8
  private static let trials = 1000

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
