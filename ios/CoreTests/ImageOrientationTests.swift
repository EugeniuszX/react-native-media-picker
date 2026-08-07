import XCTest

@testable import MediaPickerCore

final class ImageOrientationTests: XCTestCase {
  func testMapsAllEightExifValues() {
    let expected: [Int: (Int, Bool)] = [
      1: (0, false),
      2: (0, true),
      3: (180, false),
      4: (180, true),
      5: (90, true),
      6: (90, false),
      7: (270, true),
      8: (270, false),
    ]
    for (value, pair) in expected {
      let orientation = ImageOrientation.from(exifValue: value)
      XCTAssertEqual(orientation.rotationDegrees, pair.0, "exif \(value)")
      XCTAssertEqual(orientation.isMirrored, pair.1, "exif \(value)")
    }
  }

  func testOutOfRangeValuesAreTreatedAsUpright() {
    XCTAssertEqual(ImageOrientation.from(exifValue: 0), .upright)
    XCTAssertEqual(ImageOrientation.from(exifValue: 9), .upright)
    XCTAssertEqual(ImageOrientation.from(exifValue: -1), .upright)
  }

  func testSwapsAxesForQuarterTurnsOnly() {
    for value in [5, 6, 7, 8] {
      XCTAssertTrue(ImageOrientation.from(exifValue: value).swapsAxes, "exif \(value)")
    }
    for value in [1, 2, 3, 4] {
      XCTAssertFalse(ImageOrientation.from(exifValue: value).swapsAxes, "exif \(value)")
    }
  }
}
