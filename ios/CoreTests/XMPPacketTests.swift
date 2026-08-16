import XCTest

@testable import MediaPickerCore

final class XMPPacketTests: XCTestCase {
  private func buffer(embedding marker: String) -> Data {
    var data = Data([0xFF, 0xD8, 0xFF, 0xE1, 0x00, 0x00])
    data.append(marker.data(using: .ascii)!)
    data.append(Data([0x00, 0x01, 0x02]))
    return data
  }

  func testDetectsTheXmpmetaWrapper() {
    let data = buffer(embedding: "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"")
    XCTAssertTrue(XMPPacket.isPresent(in: data))
  }

  func testDetectsAPacketIdWithoutTheWrapper() {
    XCTAssertTrue(XMPPacket.isPresent(in: buffer(embedding: "W5M0MpCehiHzreSzNTczkc9d")))
  }

  func testDetectsTheXmpNamespaceUri() {
    XCTAssertTrue(XMPPacket.isPresent(in: buffer(embedding: "http://ns.adobe.com/xap/1.0/")))
  }

  func testDetectsThePngKeywordOnItsOwn() {
    // A compression-flagged `iTXt` chunk deflates its payload, so the keyword is all that is
    // left in plain bytes — none of the other three markers is present here.
    XCTAssertTrue(XMPPacket.isPresent(in: buffer(embedding: "iTXtXML:com.adobe.xmp")))
  }

  func testAnEmptyBufferHasNoPacket() {
    XCTAssertFalse(XMPPacket.isPresent(in: Data()))
  }

  func testUnrelatedBytesHaveNoPacket() {
    XCTAssertFalse(XMPPacket.isPresent(in: Data([0xFF, 0xD8, 0xFF, 0xE0, 0x10, 0x4A, 0x46, 0x49])))
  }
}
