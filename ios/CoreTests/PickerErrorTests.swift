import XCTest

@testable import MediaPickerCore

final class PickerErrorTests: XCTestCase {
  func testCodesMatchThePublicErrorCodeUnion() {
    XCTAssertEqual(PickerError.permission.code, "permission")
    XCTAssertEqual(PickerError.cameraUnavailable.code, "camera_unavailable")
    XCTAssertEqual(PickerError.busy.code, "busy")
    XCTAssertEqual(PickerError.others.code, "others")
  }
}
