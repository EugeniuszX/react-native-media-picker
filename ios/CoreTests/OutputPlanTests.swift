import XCTest

@testable import MediaPickerCore

final class OutputPlanTests: XCTestCase {
  func testRequestedFormatParsesKnownValuesAndFallsBackToOriginal() {
    XCTAssertEqual(RequestedFormat.from(rawValue: "original"), .original)
    XCTAssertEqual(RequestedFormat.from(rawValue: "jpeg"), .jpeg)
    XCTAssertEqual(RequestedFormat.from(rawValue: "png"), .png)
    XCTAssertEqual(RequestedFormat.from(rawValue: "webp"), .original)
    XCTAssertEqual(RequestedFormat.from(rawValue: ""), .original)
  }

  func testOriginalKeepsCurrentBehavior() {
    let jpeg = OutputPlan.resolve(source: .jpeg, requested: .original, isAnimatedSource: false)
    XCTAssertEqual(jpeg, OutputPlan(forceReencode: false, preserveAnimation: false, target: .jpeg))

    let heic = OutputPlan.resolve(source: .heic, requested: .original, isAnimatedSource: false)
    XCTAssertEqual(heic, OutputPlan(forceReencode: false, preserveAnimation: false, target: .heic))

    let gif = OutputPlan.resolve(source: .gif, requested: .original, isAnimatedSource: true)
    XCTAssertEqual(gif, OutputPlan(forceReencode: false, preserveAnimation: true, target: .jpeg))

    let webp = OutputPlan.resolve(source: .webp, requested: .original, isAnimatedSource: true)
    XCTAssertEqual(webp, OutputPlan(forceReencode: false, preserveAnimation: true, target: .jpeg))
  }

  func testExplicitFormatMatchingStaticSourcePassesThrough() {
    let jpeg = OutputPlan.resolve(source: .jpeg, requested: .jpeg, isAnimatedSource: false)
    XCTAssertEqual(jpeg, OutputPlan(forceReencode: false, preserveAnimation: false, target: .jpeg))

    let png = OutputPlan.resolve(source: .png, requested: .png, isAnimatedSource: false)
    XCTAssertEqual(png, OutputPlan(forceReencode: false, preserveAnimation: false, target: .png))
  }

  func testExplicitFormatMismatchForcesReencode() {
    let heicToJpeg = OutputPlan.resolve(source: .heic, requested: .jpeg, isAnimatedSource: false)
    XCTAssertEqual(
      heicToJpeg,
      OutputPlan(forceReencode: true, preserveAnimation: false, target: .jpeg)
    )

    let jpegToPng = OutputPlan.resolve(source: .jpeg, requested: .png, isAnimatedSource: false)
    XCTAssertEqual(
      jpegToPng,
      OutputPlan(forceReencode: true, preserveAnimation: false, target: .png)
    )
  }

  func testExplicitFormatDropsAnimationProtection() {
    let gif = OutputPlan.resolve(source: .gif, requested: .jpeg, isAnimatedSource: true)
    XCTAssertEqual(gif, OutputPlan(forceReencode: true, preserveAnimation: false, target: .jpeg))

    let webp = OutputPlan.resolve(source: .webp, requested: .png, isAnimatedSource: true)
    XCTAssertEqual(webp, OutputPlan(forceReencode: true, preserveAnimation: false, target: .png))
  }
}
