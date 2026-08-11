package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFormatTest {
  @Test fun normalizesKnownMimes() {
    assertEquals("image/jpeg", MediaFormat.normalizeMime("image/jpeg"))
    assertEquals("image/jpeg", MediaFormat.normalizeMime("image/jpg"))
    assertEquals("image/png", MediaFormat.normalizeMime("image/png"))
    assertEquals("image/webp", MediaFormat.normalizeMime("image/webp"))
    assertEquals("image/gif", MediaFormat.normalizeMime("image/gif"))
    assertEquals("image/heic", MediaFormat.normalizeMime("image/heic"))
    assertEquals("image/heic", MediaFormat.normalizeMime("image/heif"))
  }

  @Test fun fallsBackToJpegForUnknownOrNullMime() {
    assertEquals("image/jpeg", MediaFormat.normalizeMime(null))
    assertEquals("image/jpeg", MediaFormat.normalizeMime("application/octet-stream"))
  }

  @Test fun mapsMimeToExtension() {
    assertEquals("jpg", MediaFormat.extensionForMime("image/jpeg"))
    assertEquals("png", MediaFormat.extensionForMime("image/png"))
    assertEquals("webp", MediaFormat.extensionForMime("image/webp"))
    assertEquals("gif", MediaFormat.extensionForMime("image/gif"))
    assertEquals("heic", MediaFormat.extensionForMime("image/heic"))
  }

  @Test fun picksReencodeFormatWithHeicFallingBackToJpeg() {
    assertEquals(MediaFormat.OutputFormat.PNG, MediaFormat.reencodeFormat("image/png"))
    assertEquals(MediaFormat.OutputFormat.WEBP, MediaFormat.reencodeFormat("image/webp"))
    assertEquals(MediaFormat.OutputFormat.JPEG, MediaFormat.reencodeFormat("image/jpeg"))
    assertEquals(MediaFormat.OutputFormat.JPEG, MediaFormat.reencodeFormat("image/heic"))
  }

  @Test fun mapsReencodeFormatBackToMime() {
    assertEquals("image/png", MediaFormat.reencodeMime(MediaFormat.OutputFormat.PNG))
    assertEquals("image/webp", MediaFormat.reencodeMime(MediaFormat.OutputFormat.WEBP))
    assertEquals("image/jpeg", MediaFormat.reencodeMime(MediaFormat.OutputFormat.JPEG))
  }

  @Test fun detectsAnimatedWebp() {
    val header = ByteArray(21)
    "RIFF".forEachIndexed { i, c -> header[i] = c.code.toByte() }
    "WEBP".forEachIndexed { i, c -> header[8 + i] = c.code.toByte() }
    "VP8X".forEachIndexed { i, c -> header[12 + i] = c.code.toByte() }
    header[20] = 0x02
    assertTrue(MediaFormat.isAnimatedWebp(header))
  }

  @Test fun staticWebpIsNotAnimated() {
    val header = ByteArray(21)
    "RIFF".forEachIndexed { i, c -> header[i] = c.code.toByte() }
    "WEBP".forEachIndexed { i, c -> header[8 + i] = c.code.toByte() }
    "VP8 ".forEachIndexed { i, c -> header[12 + i] = c.code.toByte() }
    assertFalse(MediaFormat.isAnimatedWebp(header))
  }

  @Test fun shortOrNonWebpHeaderIsNotAnimated() {
    assertFalse(MediaFormat.isAnimatedWebp(ByteArray(4)))
    assertFalse(MediaFormat.isAnimatedWebp(ByteArray(0)))
  }

  @Test fun twentyByteHeaderIsNotAnimated() {
    assertFalse(MediaFormat.isAnimatedWebp(ByteArray(20)))
  }

  @Test fun vp8xWithClearAnimationFlagIsNotAnimated() {
    val header = ByteArray(21)
    "RIFF".forEachIndexed { i, c -> header[i] = c.code.toByte() }
    "WEBP".forEachIndexed { i, c -> header[8 + i] = c.code.toByte() }
    "VP8X".forEachIndexed { i, c -> header[12 + i] = c.code.toByte() }
    header[20] = 0x00
    assertFalse(MediaFormat.isAnimatedWebp(header))
  }

  @Test fun gifFallsBackToJpegOnReencode() {
    assertEquals(MediaFormat.OutputFormat.JPEG, MediaFormat.reencodeFormat("image/gif"))
  }

  @Test fun detectsVideoMimes() {
    assertTrue(MediaFormat.isVideoMime("video/mp4"))
    assertTrue(MediaFormat.isVideoMime("VIDEO/QUICKTIME"))
    assertFalse(MediaFormat.isVideoMime("image/jpeg"))
    assertFalse(MediaFormat.isVideoMime(null))
  }

  @Test fun normalizesVideoMimes() {
    assertEquals("video/mp4", MediaFormat.normalizeVideoMime("video/mp4"))
    assertEquals("video/quicktime", MediaFormat.normalizeVideoMime("video/quicktime"))
    assertEquals("video/webm", MediaFormat.normalizeVideoMime("video/webm"))
    assertEquals("video/3gpp", MediaFormat.normalizeVideoMime("video/3gpp"))
    assertEquals("video/mp4", MediaFormat.normalizeVideoMime("video/x-matroska"))
    assertEquals("video/mp4", MediaFormat.normalizeVideoMime(null))
  }

  @Test fun mapsVideoMimeToExtension() {
    assertEquals("mp4", MediaFormat.extensionForVideoMime("video/mp4"))
    assertEquals("mov", MediaFormat.extensionForVideoMime("video/quicktime"))
    assertEquals("webm", MediaFormat.extensionForVideoMime("video/webm"))
    assertEquals("3gp", MediaFormat.extensionForVideoMime("video/3gpp"))
  }

  @Test fun detectsIsoBmffVideoHeaders() {
    assertTrue(MediaFormat.isVideoHeader(ftypHeader("isom")))
    assertTrue(MediaFormat.isVideoHeader(ftypHeader("iso2")))
    assertTrue(MediaFormat.isVideoHeader(ftypHeader("mp42")))
    assertTrue(MediaFormat.isVideoHeader(ftypHeader("qt  ")))
    assertTrue(MediaFormat.isVideoHeader(ftypHeader("3gp4")))
    assertTrue(MediaFormat.isVideoHeader(ftypHeader("M4V ")))
  }

  @Test fun detectsMatroskaVideoHeader() {
    val header = ByteArray(16)
    header[0] = 0x1A
    header[1] = 0x45
    header[2] = 0xDF.toByte()
    header[3] = 0xA3.toByte()
    assertTrue(MediaFormat.isVideoHeader(header))
  }

  @Test fun treatsHeicAndAvifFtypBrandsAsNotVideo() {
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("heic")))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("heix")))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("hevc")))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("mif1")))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("msf1")))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("avif")))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("avis")))
  }

  @Test fun treatsImageHeadersAsNotVideo() {
    val jpeg = ByteArray(16)
    jpeg[0] = 0xFF.toByte()
    jpeg[1] = 0xD8.toByte()
    jpeg[2] = 0xFF.toByte()
    assertFalse(MediaFormat.isVideoHeader(jpeg))
  }

  @Test fun treatsShortOrEmptyHeaderAsNotVideo() {
    assertFalse(MediaFormat.isVideoHeader(ByteArray(0)))
    assertFalse(MediaFormat.isVideoHeader(ByteArray(3)))
    assertFalse(MediaFormat.isVideoHeader(ftypHeader("isom").copyOf(11)))
  }

  @Test fun convertsPositiveDurationMillisToSeconds() {
    assertEquals(10.0, MediaFormat.durationSecondsFrom("10000")!!, 0.0)
    assertEquals(0.5, MediaFormat.durationSecondsFrom("500")!!, 0.0)
  }

  @Test fun omitsNonPositiveOrUnparsableDurations() {
    assertNull(MediaFormat.durationSecondsFrom("0"))
    assertNull(MediaFormat.durationSecondsFrom("-100"))
    assertNull(MediaFormat.durationSecondsFrom("garbage"))
    assertNull(MediaFormat.durationSecondsFrom(null))
  }

  private fun ftypHeader(brand: String): ByteArray {
    val header = ByteArray(16)
    header[3] = 0x18
    "ftyp".forEachIndexed { i, c -> header[4 + i] = c.code.toByte() }
    brand.forEachIndexed { i, c -> header[8 + i] = c.code.toByte() }
    return header
  }
}
