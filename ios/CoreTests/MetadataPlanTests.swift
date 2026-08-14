import XCTest

@testable import MediaPickerCore

final class MetadataPlanTests: XCTestCase {
  func testStripDisabledAlwaysSkips() {
    for willTransform in [true, false] {
      for isAnimated in [true, false] {
        for canScrub in [true, false] {
          XCTAssertEqual(
            MetadataPlan.resolve(
              stripMetadata: false,
              willTransform: willTransform,
              isAnimated: isAnimated,
              canScrub: canScrub
            ),
            .skip,
            "transform \(willTransform) animated \(isAnimated) canScrub \(canScrub)"
          )
        }
      }
    }
  }

  func testTransformAlreadyDropsMetadata() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: true, isAnimated: false, canScrub: true),
      .skip
    )
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: true, isAnimated: false, canScrub: false),
      .skip
    )
  }

  func testAnimatedSourcesAreNeverTouched() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, isAnimated: true, canScrub: true),
      .skip
    )
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, isAnimated: true, canScrub: false),
      .skip
    )
  }

  func testScrubbableStillImageIsScrubbed() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, isAnimated: false, canScrub: true),
      .scrub
    )
  }

  func testUnscrubbableStillImageIsReencoded() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, isAnimated: false, canScrub: false),
      .forceReencode
    )
  }

  func testCanScrubCoversTheContainersImageIOCanRewrite() {
    XCTAssertTrue(MetadataPlan.canScrub(.jpeg))
    XCTAssertTrue(MetadataPlan.canScrub(.png))
    XCTAssertTrue(MetadataPlan.canScrub(.heic))
    XCTAssertFalse(MetadataPlan.canScrub(.gif))
    XCTAssertFalse(MetadataPlan.canScrub(.webp))
  }
}
