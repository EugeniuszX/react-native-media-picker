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

  func testErrorCodesMatchThePublicContract() {
    XCTAssertEqual(PickerError.permission.code, "permission")
    XCTAssertEqual(PickerError.cameraUnavailable.code, "camera_unavailable")
    XCTAssertEqual(PickerError.others.code, "others")
  }

  func testCameraFacingFallsBackToBack() {
    XCTAssertEqual(CameraFacing.from(rawValue: "front"), .front)
    XCTAssertEqual(CameraFacing.from(rawValue: "back"), .back)
    XCTAssertEqual(CameraFacing.from(rawValue: "sideways"), .back)
    XCTAssertEqual(CameraFacing.from(rawValue: ""), .back)
  }
}
