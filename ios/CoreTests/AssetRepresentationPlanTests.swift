import XCTest

@testable import MediaPickerCore

final class AssetRepresentationPlanTests: XCTestCase {
  func testKeepsTheStoredRepresentationWhenTheSourceFormatIsPreserved() {
    XCTAssertEqual(AssetRepresentationPlan.resolve(requested: .original), .current)
  }

  func testLetsTheSystemPickARepresentationWhenReencodingAnyway() {
    XCTAssertEqual(AssetRepresentationPlan.resolve(requested: .jpeg), .automatic)
    XCTAssertEqual(AssetRepresentationPlan.resolve(requested: .png), .automatic)
  }
}
