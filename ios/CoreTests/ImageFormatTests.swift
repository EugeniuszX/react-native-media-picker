import XCTest

@testable import MediaPickerCore

final class ImageFormatTests: XCTestCase {
  func testMapsKnownUTIs() {
    XCTAssertEqual(ImageFormat.from(uti: "public.png"), .png)
    XCTAssertEqual(ImageFormat.from(uti: "public.heic"), .heic)
    XCTAssertEqual(ImageFormat.from(uti: "public.heif"), .heic)
    XCTAssertEqual(ImageFormat.from(uti: "com.compuserve.gif"), .gif)
    XCTAssertEqual(ImageFormat.from(uti: "org.webmproject.webp"), .webp)
    XCTAssertEqual(ImageFormat.from(uti: "public.jpeg"), .jpeg)
  }

  func testFallsBackToJpegForUnknownUTI() {
    XCTAssertEqual(ImageFormat.from(uti: "com.example.unknown"), .jpeg)
  }

  func testMimeAndExtensionAgree() {
    XCTAssertEqual(ImageFormat.png.mime, "image/png")
    XCTAssertEqual(ImageFormat.png.fileExtension, "png")
    XCTAssertEqual(ImageFormat.jpeg.mime, "image/jpeg")
    XCTAssertEqual(ImageFormat.jpeg.fileExtension, "jpg")
    XCTAssertEqual(ImageFormat.heic.fileExtension, "heic")
    XCTAssertEqual(ImageFormat.gif.fileExtension, "gif")
    XCTAssertEqual(ImageFormat.webp.fileExtension, "webp")
  }

  func testOnlyGifAndWebpAreConsideredPotentiallyAnimated() {
    XCTAssertTrue(ImageFormat.gif.isPotentiallyAnimated)
    XCTAssertTrue(ImageFormat.webp.isPotentiallyAnimated)
    XCTAssertFalse(ImageFormat.jpeg.isPotentiallyAnimated)
    XCTAssertFalse(ImageFormat.png.isPotentiallyAnimated)
    XCTAssertFalse(ImageFormat.heic.isPotentiallyAnimated)
  }

  func testReencodeTargetsMatchAvailableEncoders() {
    XCTAssertEqual(ImageFormat.png.reencodeFormat, .png)
    XCTAssertEqual(ImageFormat.heic.reencodeFormat, .heic)
    XCTAssertEqual(ImageFormat.jpeg.reencodeFormat, .jpeg)
    XCTAssertEqual(ImageFormat.webp.reencodeFormat, .jpeg)
    XCTAssertEqual(ImageFormat.gif.reencodeFormat, .jpeg)
  }

  func testDetectsAnimatedWebpFromVP8XFlags() {
    XCTAssertTrue(ImageFormat.isAnimatedWebP(header: Self.webpHeader(flags: 0x02)))
    XCTAssertTrue(ImageFormat.isAnimatedWebP(header: Self.webpHeader(flags: 0x12)))
    XCTAssertFalse(ImageFormat.isAnimatedWebP(header: Self.webpHeader(flags: 0x00)))
    XCTAssertFalse(ImageFormat.isAnimatedWebP(header: Self.webpHeader(flags: 0x10)))
  }

  func testRejectsShortOrNonWebpHeaders() {
    XCTAssertFalse(ImageFormat.isAnimatedWebP(header: Data(repeating: 0, count: 20)))
    var notRiff = Self.webpHeader(flags: 0x02)
    notRiff[0] = 0x00
    XCTAssertFalse(ImageFormat.isAnimatedWebP(header: notRiff))
    var notVP8X = Self.webpHeader(flags: 0x02)
    notVP8X[12] = 0x00
    XCTAssertFalse(ImageFormat.isAnimatedWebP(header: notVP8X))
  }

  private static func webpHeader(flags: UInt8) -> Data {
    var bytes = [UInt8](repeating: 0, count: 21)
    bytes[0...3] = [0x52, 0x49, 0x46, 0x46]
    bytes[8...11] = [0x57, 0x45, 0x42, 0x50]
    bytes[12...15] = [0x56, 0x50, 0x38, 0x58]
    bytes[20] = flags
    return Data(bytes)
  }
}
