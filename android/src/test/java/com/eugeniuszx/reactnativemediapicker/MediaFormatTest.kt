package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
