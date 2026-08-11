import XCTest

@testable import MediaPickerCore

final class VideoFormatTests: XCTestCase {
  func testMapsKnownUTIs() {
    XCTAssertEqual(VideoFormat.from(uti: "com.apple.quicktime-movie"), .quickTime)
    XCTAssertEqual(VideoFormat.from(uti: "public.mpeg-4"), .mp4)
  }

  func testFallsBackToMp4ForUnknownUTI() {
    XCTAssertEqual(VideoFormat.from(uti: "public.movie"), .mp4)
    XCTAssertEqual(VideoFormat.from(uti: "com.example.unknown"), .mp4)
  }

  func testMimeAndExtensionAgree() {
    XCTAssertEqual(VideoFormat.mp4.mime, "video/mp4")
    XCTAssertEqual(VideoFormat.mp4.fileExtension, "mp4")
    XCTAssertEqual(VideoFormat.quickTime.mime, "video/quicktime")
    XCTAssertEqual(VideoFormat.quickTime.fileExtension, "mov")
  }

  func testParsesRequestedMediaType() {
    XCTAssertEqual(RequestedMediaType.from(rawValue: "photo"), .photo)
    XCTAssertEqual(RequestedMediaType.from(rawValue: "video"), .video)
    XCTAssertEqual(RequestedMediaType.from(rawValue: "mixed"), .mixed)
    XCTAssertEqual(RequestedMediaType.from(rawValue: "garbage"), .photo)
  }
}
