import XCTest

@testable import MediaPickerCore

final class ImageOrientationTests: XCTestCase {
  func testMapsAllEightExifValues() {
    let expected: [Int: (Int, Bool)] = [
      1: (0, false),  // normal
      2: (0, true),  // flip horizontal
      3: (180, false),  // rotate 180
      4: (180, true),  // flip vertical
      5: (90, true),  // transpose
      6: (90, false),  // rotate 90 CW
      7: (270, true),  // transverse
      8: (270, false),  // rotate 270 CW
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

  /// The four 90°/270° cases transpose the stored buffer, so reported width and
  /// height must be swapped for them and only them.
  func testSwapsAxesForQuarterTurnsOnly() {
    for value in [5, 6, 7, 8] {
      XCTAssertTrue(ImageOrientation.from(exifValue: value).swapsAxes, "exif \(value)")
    }
    for value in [1, 2, 3, 4] {
      XCTAssertFalse(ImageOrientation.from(exifValue: value).swapsAxes, "exif \(value)")
    }
  }
}
