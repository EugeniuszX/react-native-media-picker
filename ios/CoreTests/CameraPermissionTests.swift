import XCTest

@testable import MediaPickerCore

final class CameraPermissionTests: XCTestCase {
  func testReportsUnavailableWithoutACamera() {
    for authorization: CameraAuthorization in [
      .authorized, .notDetermined, .denied, .restricted, .unknown,
    ] {
      XCTAssertEqual(
        CameraPermission.resolve(hasCamera: false, authorization: authorization),
        .unavailable
      )
    }
  }

  func testMapsAuthorizationStates() {
    XCTAssertEqual(
      CameraPermission.resolve(hasCamera: true, authorization: .authorized), .granted)
    XCTAssertEqual(
      CameraPermission.resolve(hasCamera: true, authorization: .notDetermined), .notDetermined)
  }

  func testTreatsEveryRefusalAsBlockedBecauseIOSNeverReasks() {
    XCTAssertEqual(CameraPermission.resolve(hasCamera: true, authorization: .denied), .blocked)
    XCTAssertEqual(CameraPermission.resolve(hasCamera: true, authorization: .restricted), .blocked)
    XCTAssertEqual(CameraPermission.resolve(hasCamera: true, authorization: .unknown), .blocked)
  }

  func testRawValuesMatchTheJSUnion() {
    XCTAssertEqual(CameraPermission.granted.rawValue, "granted")
    XCTAssertEqual(CameraPermission.notDetermined.rawValue, "not_determined")
    XCTAssertEqual(CameraPermission.blocked.rawValue, "blocked")
    XCTAssertEqual(CameraPermission.unavailable.rawValue, "unavailable")
  }
}
