import XCTest

@testable import MediaPickerCore

final class GPSCoordinateTests: XCTestCase {
  func testAppliesTheHemisphereSign() {
    XCTAssertEqual(GPSCoordinate.signed(50.4501, ref: "N")!, 50.4501, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.signed(50.4501, ref: "S")!, -50.4501, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.signed(30.5234, ref: "E")!, 30.5234, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.signed(30.5234, ref: "W")!, -30.5234, accuracy: 1e-9)
  }

  func testTreatsAMissingOrOddRefAsPositive() {
    XCTAssertEqual(GPSCoordinate.signed(1.5, ref: nil)!, 1.5, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.signed(1.5, ref: "?")!, 1.5, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.signed(1.5, ref: " s ")!, -1.5, accuracy: 1e-9)
  }

  func testReturnsNilWithoutAMagnitude() {
    XCTAssertNil(GPSCoordinate.signed(nil, ref: "N"))
    XCTAssertNil(GPSCoordinate.altitude(nil, ref: 0))
  }

  func testAltitudeRefOneMeansBelowSeaLevel() {
    XCTAssertEqual(GPSCoordinate.altitude(150.0, ref: 0)!, 150.0, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.altitude(150.0, ref: 1)!, -150.0, accuracy: 1e-9)
    XCTAssertEqual(GPSCoordinate.altitude(150.0, ref: nil)!, 150.0, accuracy: 1e-9)
  }
}
