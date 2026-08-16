import XCTest

@testable import MediaPickerCore

final class AssetPayloadTests: XCTestCase {
  private func makePayload(base64: String?) -> AssetPayload {
    AssetPayload(
      uri: "file:///tmp/rn-media-picker/media_picker_A.jpg",
      mime: ImageFormat.jpeg.mime,
      fileName: "media_picker_A.jpg",
      fileSize: 1234,
      width: 640,
      height: 480,
      base64: base64
    )
  }

  func testDictionaryCarriesExactlyTheJsAssetKeys() {
    let dict = makePayload(base64: nil).dictionary
    XCTAssertEqual(
      Set(dict.keys),
      ["uri", "type", "fileName", "fileSize", "width", "height"]
    )
    XCTAssertEqual(dict["uri"] as? String, "file:///tmp/rn-media-picker/media_picker_A.jpg")
    XCTAssertEqual(dict["type"] as? String, "image/jpeg")
    XCTAssertEqual(dict["fileName"] as? String, "media_picker_A.jpg")
    XCTAssertEqual(dict["fileSize"] as? Int, 1234)
    XCTAssertEqual(dict["width"] as? Int, 640)
    XCTAssertEqual(dict["height"] as? Int, 480)
  }

  func testBase64IsAddedOnlyWhenPresent() {
    let dict = makePayload(base64: "QUJD").dictionary
    XCTAssertEqual(dict["base64"] as? String, "QUJD")
    XCTAssertEqual(dict.keys.count, 7)
  }

  func testDictionaryIncludesDurationWhenSet() {
    let payload = AssetPayload(
      uri: "file:///tmp/a.mp4",
      mime: "video/mp4",
      fileName: "a.mp4",
      fileSize: 10,
      width: 1920,
      height: 1080,
      base64: nil,
      duration: 12.5
    )
    XCTAssertEqual(payload.dictionary["duration"] as? Double, 12.5)
  }

  func testDictionaryOmitsDurationWhenNil() {
    let payload = AssetPayload(
      uri: "file:///tmp/a.jpg",
      mime: "image/jpeg",
      fileName: "a.jpg",
      fileSize: 10,
      width: 100,
      height: 100,
      base64: nil
    )
    XCTAssertNil(payload.dictionary["duration"])
  }

  func testDictionaryIncludesTheThumbnailKeysWhenSet() {
    let payload = AssetPayload(
      uri: "file:///tmp/a.mp4",
      mime: "video/mp4",
      fileName: "a.mp4",
      fileSize: 10,
      width: 1920,
      height: 1080,
      base64: nil,
      duration: 12.5,
      thumbnail: Thumbnail(uri: "file:///tmp/a.jpg", width: 512, height: 288)
    )
    let dict = payload.dictionary
    XCTAssertEqual(dict["thumbnailUri"] as? String, "file:///tmp/a.jpg")
    XCTAssertEqual(dict["thumbnailWidth"] as? Int, 512)
    XCTAssertEqual(dict["thumbnailHeight"] as? Int, 288)
  }

  func testDictionaryOmitsTheThumbnailKeysWhenNil() {
    let dict = makePayload(base64: nil).dictionary
    XCTAssertNil(dict["thumbnailUri"])
    XCTAssertNil(dict["thumbnailWidth"])
    XCTAssertNil(dict["thumbnailHeight"])
  }

  func testCameraFacingFallsBackToBack() {
    XCTAssertEqual(CameraFacing.from(rawValue: "front"), .front)
    XCTAssertEqual(CameraFacing.from(rawValue: "back"), .back)
    XCTAssertEqual(CameraFacing.from(rawValue: "sideways"), .back)
    XCTAssertEqual(CameraFacing.from(rawValue: ""), .back)
  }
}
