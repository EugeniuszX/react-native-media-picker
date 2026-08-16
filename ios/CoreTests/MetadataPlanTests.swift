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

  func testPreserveSourceGuardsOnlyTheReencode() {
    // A scrub rewrites the container without decoding a pixel, so a preserved source is still
    // scrubbed when its container can be rewritten — only the re-encode is held back. No iOS
    // format reaches this combination (see testAnimatedWebPIsSkippedOnIOS), but the decision
    // core is shared with Android, where WebP is scrubbable.
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, preserveSource: true, canScrub: true),
      .scrub
    )
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true, willTransform: false, preserveSource: true, canScrub: false),
      .skip
    )
  }

  /// The deliberate platform divergence, asserted rather than incidental: Android scrubs an
  /// animated WebP, iOS cannot rewrite a WebP at all and so leaves it alone.
  func testAnimatedWebPIsSkippedOnIOS() {
    XCTAssertEqual(
      MetadataPlan.resolve(
        stripMetadata: true,
        willTransform: false,
        preserveSource: MetadataPlan.preservesSource(format: .webp, preserveAnimation: true),
        canScrub: MetadataPlan.canScrub(.webp)
      ),
      .skip
    )
  }

  func testGifIsLeftAloneWhetherAnimatedOrNot() {
    for animated in [true, false] {
      XCTAssertEqual(
        MetadataPlan.resolve(
          stripMetadata: true,
          willTransform: false,
          preserveSource: MetadataPlan.preservesSource(format: .gif, preserveAnimation: animated),
          canScrub: MetadataPlan.canScrub(.gif)
        ),
        .skip,
        "animated \(animated)"
      )
    }
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

  /// JPEG only — `CGImageDestinationCopyImageSource` was measured to leave PNG `tEXt` credits and
  /// HEIC Artist/Copyright/Software in place, so those two take the re-encode instead.
  func testCanScrubCoversTheContainersImageIOCanRewrite() {
    XCTAssertTrue(MetadataPlan.canScrub(.jpeg))
    XCTAssertFalse(MetadataPlan.canScrub(.png))
    XCTAssertFalse(MetadataPlan.canScrub(.heic))
    XCTAssertFalse(MetadataPlan.canScrub(.gif))
    XCTAssertFalse(MetadataPlan.canScrub(.webp))
  }

  /// A PNG or HEIC asked to strip must not be passed through untouched now that it cannot be
  /// scrubbed — it re-encodes, which removes the metadata by decoding.
  func testPngAndHeicReencodeWhenStripped() {
    for format in [ImageFormat.png, .heic] {
      XCTAssertEqual(
        MetadataPlan.resolve(
          stripMetadata: true,
          willTransform: false,
          preserveSource: MetadataPlan.preservesSource(format: format, preserveAnimation: false),
          canScrub: MetadataPlan.canScrub(format)
        ),
        .forceReencode,
        "\(format)"
      )
    }
  }
}
