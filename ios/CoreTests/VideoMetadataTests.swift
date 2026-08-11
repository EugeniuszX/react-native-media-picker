import CoreGraphics
import XCTest

@testable import MediaPickerCore

final class VideoMetadataTests: XCTestCase {
  private let naturalSize = CGSize(width: 1920, height: 1080)

  func testIdentityTransformKeepsAxes() {
    let size = VideoMetadata.displayedSize(
      naturalSize: naturalSize, preferredTransform: .identity)
    XCTAssertEqual(size.width, 1920)
    XCTAssertEqual(size.height, 1080)
  }

  func testQuarterTurnSwapsAxes() {
    // 90° clockwise, as produced by portrait phone captures:
    // (a b c d tx ty) = (0 1 -1 0 h 0)
    let transform = CGAffineTransform(a: 0, b: 1, c: -1, d: 0, tx: 1080, ty: 0)
    let size = VideoMetadata.displayedSize(
      naturalSize: naturalSize, preferredTransform: transform)
    XCTAssertEqual(size.width, 1080)
    XCTAssertEqual(size.height, 1920)
  }

  func testHalfTurnKeepsAxes() {
    let transform = CGAffineTransform(a: -1, b: 0, c: 0, d: -1, tx: 1920, ty: 1080)
    let size = VideoMetadata.displayedSize(
      naturalSize: naturalSize, preferredTransform: transform)
    XCTAssertEqual(size.width, 1920)
    XCTAssertEqual(size.height, 1080)
  }

  func testThreeQuarterTurnSwapsAxes() {
    let transform = CGAffineTransform(a: 0, b: -1, c: 1, d: 0, tx: 0, ty: 1920)
    let size = VideoMetadata.displayedSize(
      naturalSize: naturalSize, preferredTransform: transform)
    XCTAssertEqual(size.width, 1080)
    XCTAssertEqual(size.height, 1920)
  }

  func testRotationAngleTransformRoundsCleanly() {
    let transform = CGAffineTransform(rotationAngle: .pi / 2)
    let size = VideoMetadata.displayedSize(
      naturalSize: naturalSize, preferredTransform: transform)
    XCTAssertEqual(size.width, 1080)
    XCTAssertEqual(size.height, 1920)
  }
}
