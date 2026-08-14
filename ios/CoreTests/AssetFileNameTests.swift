import XCTest

@testable import MediaPickerCore

final class AssetFileNameTests: XCTestCase {
  private let fallback = "media_picker_ABC.jpg"

  func testKeepsTheSourceNameAndAppliesTheOutputExtension() {
    XCTAssertEqual(
      AssetFileName.resolve(suggested: "IMG_4821", fallback: fallback, fileExtension: "jpg"),
      "IMG_4821.jpg"
    )
  }

  func testReplacesTheSourceExtensionWithTheOutputOne() {
    XCTAssertEqual(
      AssetFileName.resolve(suggested: "IMG_4821.HEIC", fallback: fallback, fileExtension: "jpg"),
      "IMG_4821.jpg"
    )
  }

  func testFallsBackWhenNoNameIsAvailable() {
    XCTAssertEqual(
      AssetFileName.resolve(suggested: nil, fallback: fallback, fileExtension: "jpg"),
      fallback
    )
    XCTAssertEqual(
      AssetFileName.resolve(suggested: "   ", fallback: fallback, fileExtension: "jpg"),
      fallback
    )
    XCTAssertEqual(
      AssetFileName.resolve(suggested: ".HEIC", fallback: fallback, fileExtension: "jpg"),
      fallback
    )
  }

  func testKeepsOnlyTheLastPathComponent() {
    XCTAssertEqual(
      AssetFileName.resolve(
        suggested: "../../etc/passwd", fallback: fallback, fileExtension: "jpg"),
      "passwd.jpg"
    )
    XCTAssertEqual(
      AssetFileName.resolve(
        suggested: "DCIM\\Camera\\IMG_1.jpg", fallback: fallback, fileExtension: "jpg"),
      "IMG_1.jpg"
    )
    XCTAssertEqual(
      AssetFileName.resolve(suggested: "..", fallback: fallback, fileExtension: "jpg"),
      fallback
    )
  }

  func testStripsControlCharacters() {
    XCTAssertEqual(
      AssetFileName.resolve(
        suggested: "IMG\u{0}_\n48\t21", fallback: fallback, fileExtension: "jpg"),
      "IMG_4821.jpg"
    )
  }

  func testKeepsInnerSpacesAndUnicode() {
    XCTAssertEqual(
      AssetFileName.resolve(
        suggested: " Отпуск 2026.png ", fallback: fallback, fileExtension: "jpg"),
      "Отпуск 2026.jpg"
    )
  }

  func testTruncatesLongNames() {
    let long = String(repeating: "a", count: 240)
    let resolved = AssetFileName.resolve(
      suggested: long, fallback: fallback, fileExtension: "jpeg")
    XCTAssertEqual(resolved, String(repeating: "a", count: AssetFileName.maxBaseLength) + ".jpeg")
  }

  func testOnlyStripsAnExtensionThatLooksLikeOne() {
    XCTAssertEqual(
      AssetFileName.resolve(
        suggested: "report.final.v2", fallback: fallback, fileExtension: "jpg"),
      "report.final.jpg"
    )
    XCTAssertEqual(
      AssetFileName.resolve(
        suggested: "shot 2026.08.14 18.22.31", fallback: fallback, fileExtension: "jpg"),
      "shot 2026.08.14 18.22.jpg"
    )
  }

  func testReturnsTheBareNameWhenNoExtensionIsGiven() {
    XCTAssertEqual(
      AssetFileName.resolve(suggested: "IMG_4821.HEIC", fallback: fallback, fileExtension: ""),
      "IMG_4821"
    )
  }
}
