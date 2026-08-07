import XCTest

@testable import MediaPickerCore

final class DecodePlanTests: XCTestCase {
  private func plan(
    _ width: Int,
    _ height: Int,
    maxWidth: Int = 0,
    maxHeight: Int = 0,
    exif: Int = 1,
    isAnimated: Bool = false
  ) -> DecodePlan {
    DecodePlan.compute(
      pixelWidth: width,
      pixelHeight: height,
      orientation: ImageOrientation.from(exifValue: exif),
      maxWidth: maxWidth,
      maxHeight: maxHeight,
      isAnimated: isAnimated
    )
  }

  func testNoBoundsMeansPassthrough() {
    let p = plan(4000, 3000)
    XCTAssertFalse(p.needsTransform)
    XCTAssertEqual(p.displayWidth, 4000)
    XCTAssertEqual(p.displayHeight, 3000)
    XCTAssertEqual(p.targetWidth, 4000)
    XCTAssertEqual(p.targetHeight, 3000)
    XCTAssertEqual(p.sampleSize, 1)
  }

  func testImageAlreadyWithinBoundsIsPassedThrough() {
    let p = plan(600, 400, maxWidth: 640, maxHeight: 640)
    XCTAssertFalse(p.needsTransform)
    XCTAssertEqual(p.targetWidth, 600)
    XCTAssertEqual(p.targetHeight, 400)
  }

  func testAnimatedImagesIgnoreBounds() {
    let p = plan(4000, 3000, maxWidth: 100, maxHeight: 100, isAnimated: true)
    XCTAssertFalse(p.needsTransform)
    XCTAssertEqual(p.targetWidth, 4000)
    XCTAssertEqual(p.targetHeight, 3000)
  }

  func testScalesDownPreservingAspectRatio() {
    let p = plan(4000, 2000, maxWidth: 640, maxHeight: 640)
    XCTAssertTrue(p.needsTransform)
    XCTAssertEqual(p.targetWidth, 640)
    XCTAssertEqual(p.targetHeight, 320)
  }

  func testASingleAxisBoundConstrainsThatAxisOnly() {
    let wide = plan(4000, 2000, maxWidth: 1000, maxHeight: 0)
    XCTAssertTrue(wide.needsTransform)
    XCTAssertEqual(wide.targetWidth, 1000)
    XCTAssertEqual(wide.targetHeight, 500)

    let tall = plan(2000, 4000, maxWidth: 0, maxHeight: 1000)
    XCTAssertTrue(tall.needsTransform)
    XCTAssertEqual(tall.targetWidth, 500)
    XCTAssertEqual(tall.targetHeight, 1000)
  }

  func testBoundsApplyToDisplayedAxesForRotatedImages() {
    let p = plan(4000, 3000, maxWidth: 600, maxHeight: 600, exif: 6)
    XCTAssertEqual(p.displayWidth, 3000)
    XCTAssertEqual(p.displayHeight, 4000)
    XCTAssertTrue(p.needsTransform)
    XCTAssertEqual(p.targetWidth, 450)
    XCTAssertEqual(p.targetHeight, 600)
  }

  func testRotatedImageWithinBoundsAfterSwapIsPassedThrough() {
    let p = plan(400, 800, maxWidth: 900, maxHeight: 900, exif: 6)
    XCTAssertFalse(p.needsTransform)
    XCTAssertEqual(p.displayWidth, 800)
    XCTAssertEqual(p.displayHeight, 400)
  }

  func testSampleSizeIsTheLargestPowerOfTwoThatStillCoversTheTarget() {
    XCTAssertEqual(plan(4000, 3000, maxWidth: 1000, maxHeight: 1000).sampleSize, 4)
    XCTAssertEqual(plan(4000, 3000, maxWidth: 500, maxHeight: 500).sampleSize, 8)
    XCTAssertEqual(plan(4000, 3000, maxWidth: 250, maxHeight: 250).sampleSize, 16)
    XCTAssertEqual(plan(4000, 3000, maxWidth: 2500, maxHeight: 2500).sampleSize, 1)
  }

  func testDownsampledBufferNeverFallsBelowTheTarget() {
    let p = plan(4000, 3000, maxWidth: 700, maxHeight: 700)
    XCTAssertGreaterThanOrEqual(p.displayWidth / p.sampleSize, p.targetWidth)
    XCTAssertGreaterThanOrEqual(p.displayHeight / p.sampleSize, p.targetHeight)
  }

  func testBindingAxisLandsExactlyOnItsBound() {
    let large = plan(5712, 4284, maxWidth: 1000, maxHeight: 1000)
    XCTAssertEqual(large.targetWidth, 1000)
    XCTAssertEqual(large.targetHeight, 750)

    let medium = plan(3088, 2320, maxWidth: 800, maxHeight: 800)
    XCTAssertEqual(medium.targetWidth, 800)
    XCTAssertEqual(medium.targetHeight, 601)
  }

  func testSentinelSizedBoundDoesNotOverflow() {
    let p = plan(4000, 3000, maxWidth: 9_007_199_254_740_991, maxHeight: 500)
    XCTAssertEqual(p.targetWidth, 666)
    XCTAssertEqual(p.targetHeight, 500)
  }

  func testTargetNeverCollapsesToZero() {
    let p = plan(4000, 10, maxWidth: 1, maxHeight: 1)
    XCTAssertGreaterThanOrEqual(p.targetWidth, 1)
    XCTAssertGreaterThanOrEqual(p.targetHeight, 1)
  }

  func testUnreadableDimensionsProducePassthroughWithZeroSize() {
    let p = plan(0, 0, maxWidth: 640, maxHeight: 640)
    XCTAssertFalse(p.needsTransform)
    XCTAssertEqual(p.displayWidth, 0)
    XCTAssertEqual(p.displayHeight, 0)
    XCTAssertEqual(p.sampleSize, 1)
  }

  func testNegativeDimensionsAreClampedToZero() {
    let p = plan(-1, -1, maxWidth: 640, maxHeight: 640)
    XCTAssertFalse(p.needsTransform)
    XCTAssertEqual(p.displayWidth, 0)
    XCTAssertEqual(p.displayHeight, 0)
    XCTAssertEqual(p.targetWidth, 0)
    XCTAssertEqual(p.targetHeight, 0)
  }
}
