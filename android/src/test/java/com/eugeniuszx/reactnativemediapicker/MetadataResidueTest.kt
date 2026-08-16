package com.eugeniuszx.reactnativemediapicker

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MetadataResidueTest {
  @get:Rule
  val folder = TemporaryFolder()

  private fun bytes(text: String) = text.toByteArray(Charsets.US_ASCII)

  private val pngSignature =
    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

  /** Builds a structurally valid PNG chunk table. Chunk data is filler; the CRC is never read. */
  private fun png(vararg chunks: Pair<String, Int>): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(pngSignature)
    for ((type, dataLength) in chunks) {
      out.write(
        byteArrayOf(
          (dataLength ushr 24).toByte(),
          (dataLength ushr 16).toByte(),
          (dataLength ushr 8).toByte(),
          dataLength.toByte(),
        ),
      )
      out.write(bytes(type))
      out.write(ByteArray(dataLength) { 'x'.code.toByte() })
      out.write(ByteArray(4)) // CRC
    }
    return out.toByteArray()
  }

  @Test fun findsTheXmpmetaWrapper() {
    assertTrue(MetadataResidue.isPresent(bytes("  <x:xmpmeta xmlns:x=\"adobe:ns:meta/\">")))
  }

  @Test fun findsAPacketIdOnItsOwn() {
    assertTrue(
      MetadataResidue.isPresent(bytes("<?xpacket begin='' id='W5M0MpCehiHzreSzNTczkc9d'?>")),
    )
  }

  @Test fun findsTheNamespaceUriOnItsOwn() {
    assertTrue(MetadataResidue.isPresent(bytes("xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\"")))
  }

  @Test fun findsThePngKeywordOnItsOwn() {
    assertTrue(MetadataResidue.isPresent(bytes("iTXtXML:com.adobe.xmp   ")))
  }

  @Test fun findsTheJpegPhotoshopBlockThatCarriesIptc() {
    // How an APP13 segment opens: the IRB identifier, then the 8BIM resources that hold IPTC-IIM.
    assertTrue(MetadataResidue.isPresent(bytes("....Photoshop 3.0.8BIM....")))
  }

  @Test fun anEmptyBufferHasNoResidue() {
    assertFalse(MetadataResidue.isPresent(ByteArray(0)))
  }

  @Test fun unrelatedBytesHaveNoResidue() {
    assertFalse(MetadataResidue.isPresent(bytes("Exif  MM  *Pixel 9 GPSLatitude 50/1")))
    assertFalse(MetadataResidue.isPresent(ByteArray(4096) { (it % 251).toByte() }))
  }

  @Test fun findsATextChunkAfterAValidHeaderChunk() {
    assertTrue(MetadataResidue.isPresent(png("IHDR" to 13, "tEXt" to 24, "IDAT" to 64, "IEND" to 0)))
  }

  @Test fun findsTheCompressedTextChunkVariants() {
    assertTrue(MetadataResidue.isPresent(png("IHDR" to 13, "zTXt" to 40, "IEND" to 0)))
    assertTrue(MetadataResidue.isPresent(png("IHDR" to 13, "iTXt" to 40, "IEND" to 0)))
  }

  @Test fun aPngWithOnlyImageChunksIsClean() {
    assertFalse(
      MetadataResidue.isPresent(
        png("IHDR" to 13, "gAMA" to 4, "IDAT" to 512, "IDAT" to 512, "IEND" to 0),
      ),
    )
  }

  @Test fun aPngWhoseChunkTableDoesNotParseIsDeclined() {
    // Truncated mid-table: no IEND was ever reached, so the file cannot be vouched for.
    val truncated = png("IHDR" to 13, "IDAT" to 512).copyOfRange(0, 40)
    assertTrue(MetadataResidue.isPresent(truncated))
    // A chunk claiming far more data than the file holds.
    val overlong = ByteArrayOutputStream()
    overlong.write(pngSignature)
    overlong.write(byteArrayOf(0x40, 0, 0, 0)) // 1 GiB of data, in a 20-byte file
    overlong.write(bytes("IDAT"))
    assertTrue(MetadataResidue.isPresent(overlong.toByteArray()))
    // A chunk length with its top bit set, which the PNG spec forbids.
    val negative = ByteArrayOutputStream()
    negative.write(pngSignature)
    negative.write(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
    negative.write(bytes("IDAT"))
    assertTrue(MetadataResidue.isPresent(negative.toByteArray()))
    // The signature alone, with no chunk table at all.
    assertTrue(MetadataResidue.isPresent(pngSignature))
  }

  @Test fun pngPixelDataThatLooksLikeAChunkTypeIsNotAFalsePositive() {
    // "tEXt" sitting inside IDAT data is not a chunk, and the walk must not mistake it for one.
    val out = ByteArrayOutputStream()
    out.write(pngSignature)
    out.write(byteArrayOf(0, 0, 0, 13))
    out.write(bytes("IHDR"))
    out.write(ByteArray(13))
    out.write(ByteArray(4))
    out.write(byteArrayOf(0, 0, 0, 32))
    out.write(bytes("IDAT"))
    out.write(bytes("....tEXt....zTXt....iTXt........"))
    out.write(ByteArray(4))
    out.write(byteArrayOf(0, 0, 0, 0))
    out.write(bytes("IEND"))
    out.write(ByteArray(4))
    assertFalse(MetadataResidue.isPresent(out.toByteArray()))
  }

  @Test fun findsAMarkerStraddlingTheStreamingChunkBoundary() {
    // 64 KiB is the chunk size, so this marker starts five bytes before the first boundary and
    // is only found if the overlap between chunks works.
    val file = folder.newFile("straddle.jpg")
    file.outputStream().use { out ->
      out.write(ByteArray(64 * 1024 - 5) { 'a'.code.toByte() })
      out.write(bytes("<x:xmpmeta"))
      out.write(ByteArray(1024) { 'b'.code.toByte() })
    }
    assertTrue(MetadataResidue.isPresent(file))
  }

  @Test fun aLargeResidueFreeFileIsReportedClean() {
    val file = folder.newFile("clean.jpg")
    file.writeBytes(ByteArray(200 * 1024) { (it % 251).toByte() })
    assertFalse(MetadataResidue.isPresent(file))
  }

  @Test fun aPngFileIsWalkedRatherThanScanned() {
    val clean = folder.newFile("clean.png")
    clean.writeBytes(png("IHDR" to 13, "IDAT" to 200 * 1024, "IEND" to 0))
    assertFalse(MetadataResidue.isPresent(clean))

    val credited = folder.newFile("credited.png")
    credited.writeBytes(png("IHDR" to 13, "tEXt" to 32, "IDAT" to 200 * 1024, "IEND" to 0))
    assertTrue(MetadataResidue.isPresent(credited))
  }
}
