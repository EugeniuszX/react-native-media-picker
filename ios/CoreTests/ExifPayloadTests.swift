import XCTest

@testable import MediaPickerCore

final class ExifPayloadTests: XCTestCase {
  func testAFreshPayloadIsEmpty() {
    XCTAssertTrue(ExifPayload().isEmpty)
  }

  func testAnyPopulatedFieldMakesItNonEmpty() {
    var payload = ExifPayload()
    payload.model = "iPhone 15 Pro"
    XCTAssertFalse(payload.isEmpty)
  }

  func testDictionaryOmitsAbsentFields() {
    var payload = ExifPayload()
    payload.dateTimeOriginal = "2026-08-14T15:29:03"
    payload.latitude = 50.4501
    payload.longitude = -30.5234

    let dictionary = payload.dictionary
    XCTAssertEqual(dictionary["dateTimeOriginal"] as? String, "2026-08-14T15:29:03")
    XCTAssertEqual(dictionary["latitude"] as? Double, 50.4501)
    XCTAssertEqual(dictionary["longitude"] as? Double, -30.5234)
    XCTAssertNil(dictionary["make"])
    XCTAssertNil(dictionary["iso"])
    XCTAssertEqual(dictionary.count, 3)
  }

  func testAssetPayloadCarriesExifOnlyWhenPresent() {
    var exif = ExifPayload()
    exif.iso = 100

    let without = AssetPayload(
      uri: "file:///tmp/a.jpg", mime: "image/jpeg", fileName: "a.jpg",
      fileSize: 1, width: 2, height: 3, base64: nil
    )
    XCTAssertNil(without.dictionary["exif"])

    var with = without
    with.exif = exif
    let nested = with.dictionary["exif"] as? [String: Any]
    XCTAssertEqual(nested?["iso"] as? Int, 100)
  }
}
