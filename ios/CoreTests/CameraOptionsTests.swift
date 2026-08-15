import XCTest

@testable import MediaPickerCore

final class CameraOptionsTests: XCTestCase {
  func testParsesCameraMediaType() {
    XCTAssertEqual(CameraMediaType.from(rawValue: "photo"), .photo)
    XCTAssertEqual(CameraMediaType.from(rawValue: "video"), .video)
    XCTAssertEqual(CameraMediaType.from(rawValue: "mixed"), .photo)
    XCTAssertEqual(CameraMediaType.from(rawValue: ""), .photo)
  }

  func testParsesVideoQuality() {
    XCTAssertEqual(VideoQuality.from(rawValue: "low"), .low)
    XCTAssertEqual(VideoQuality.from(rawValue: "medium"), .medium)
    XCTAssertEqual(VideoQuality.from(rawValue: "high"), .high)
    XCTAssertEqual(VideoQuality.from(rawValue: "ultra"), .high)
    XCTAssertEqual(VideoQuality.from(rawValue: ""), .high)
  }
}
