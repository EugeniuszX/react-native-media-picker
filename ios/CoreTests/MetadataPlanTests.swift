import XCTest

@testable import MediaPickerCore

final class MetadataPlanTests: XCTestCase {
  func testStripDisabledAlwaysSkips() {
    for willTransform in [true, false] {
      for preserveSource in [true, false] {
        for canScrub in [true, false] {
          XCTAssertEqual(
            MetadataPlan.resolve(
              stripMetadata: false,
              willTransform: willTransform,
              preserveSource: preserveSource,
              canScrub: canScrub
            ),
            .skip,
            "transform \(willTransform) preserve \(preserveSource) canScrub \(canScrub)"
          )
        }
      }
    }
  }

  func testTransformAlreadyDropsMetadata() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: true, preserveSource: false, canScrub: true),
      .skip
    )
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: true, preserveSource: false, canScrub: false),
      .skip
    )
  }

  func testAnimatedSourcesAreNeverTouched() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, preserveSource: true, canScrub: true),
      .skip
    )
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, preserveSource: true, canScrub: false),
      .skip
    )
  }

  func testScrubbableStillImageIsScrubbed() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, preserveSource: false, canScrub: true),
      .scrub
    )
  }

  func testUnscrubbableStillImageIsReencoded() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, preserveSource: false, canScrub: false),
      .forceReencode
    )
  }

  func testAnimatedSourceIsPreserved() {
    XCTAssertTrue(MetadataPlan.preservesSource(format: .webp, preserveAnimation: true))
  }

  func testStaticGifIsPreserved() {
    XCTAssertTrue(MetadataPlan.preservesSource(format: .gif, preserveAnimation: false))
  }

  func testAnimatedGifIsPreserved() {
    XCTAssertTrue(MetadataPlan.preservesSource(format: .gif, preserveAnimation: true))
  }

  func testStaticNonGifSourcesAreNotPreserved() {
    for format in [ImageFormat.jpeg, .png, .heic, .webp] {
      XCTAssertFalse(
        MetadataPlan.preservesSource(format: format, preserveAnimation: false),
        "\(format)"
      )
    }
  }

  func testCanScrubCoversTheContainersImageIOCanRewrite() {
    XCTAssertTrue(MetadataPlan.canScrub(.jpeg))
    XCTAssertTrue(MetadataPlan.canScrub(.png))
    XCTAssertTrue(MetadataPlan.canScrub(.heic))
    XCTAssertFalse(MetadataPlan.canScrub(.gif))
    XCTAssertFalse(MetadataPlan.canScrub(.webp))
  }
}
