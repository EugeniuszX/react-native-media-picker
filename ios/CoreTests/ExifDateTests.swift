import XCTest

@testable import MediaPickerCore

final class ExifDateTests: XCTestCase {
  func testConvertsAWellFormedExifTimestamp() {
    XCTAssertEqual(ExifDate.iso8601(from: "2026:08:14 15:29:03"), "2026-08-14T15:29:03")
  }

  func testTrimsSurroundingWhitespace() {
    XCTAssertEqual(ExifDate.iso8601(from: "  2026:08:14 15:29:03 "), "2026-08-14T15:29:03")
  }

  func testRejectsTheAllZeroPlaceholderCamerasWrite() {
    XCTAssertNil(ExifDate.iso8601(from: "0000:00:00 00:00:00"))
  }

  func testRejectsMalformedInput() {
    XCTAssertNil(ExifDate.iso8601(from: nil))
    XCTAssertNil(ExifDate.iso8601(from: ""))
    XCTAssertNil(ExifDate.iso8601(from: "2026-08-14T15:29:03"))
    XCTAssertNil(ExifDate.iso8601(from: "2026:08:14"))
    XCTAssertNil(ExifDate.iso8601(from: "20xx:08:14 15:29:03"))
    XCTAssertNil(ExifDate.iso8601(from: "2026:8:14 15:29:03"))
  }

  /// `Character.isNumber` covers every Unicode number category, so the digit check has to be
  /// ASCII-only to stay in step with the Kotlin unit and with the ISO-8601 output contract.
  func testRejectsNonAsciiDigits() {
    XCTAssertNil(ExifDate.iso8601(from: "٢٠٢٦:٠٨:١٤ ١٥:٢٩:٠٣"))
    XCTAssertNil(ExifDate.iso8601(from: "2026:08:1٤ 15:29:03"))
  }
}
