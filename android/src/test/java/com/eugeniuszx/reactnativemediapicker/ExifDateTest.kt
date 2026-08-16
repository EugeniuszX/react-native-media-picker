package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExifDateTest {
  @Test fun convertsAWellFormedExifTimestamp() {
    assertEquals("2026-08-14T15:29:03", ExifDate.iso8601("2026:08:14 15:29:03"))
  }

  @Test fun trimsSurroundingWhitespace() {
    assertEquals("2026-08-14T15:29:03", ExifDate.iso8601("  2026:08:14 15:29:03 "))
  }

  @Test fun rejectsTheAllZeroPlaceholderCamerasWrite() {
    assertNull(ExifDate.iso8601("0000:00:00 00:00:00"))
  }

  @Test fun rejectsMalformedInput() {
    assertNull(ExifDate.iso8601(null))
    assertNull(ExifDate.iso8601(""))
    assertNull(ExifDate.iso8601("2026-08-14T15:29:03"))
    assertNull(ExifDate.iso8601("2026:08:14"))
    assertNull(ExifDate.iso8601("20xx:08:14 15:29:03"))
    assertNull(ExifDate.iso8601("2026:8:14 15:29:03"))
  }

  /** `\d` is ASCII-only here; this guards against a regression if anyone widens the pattern. */
  @Test fun rejectsNonAsciiDigits() {
    assertNull(ExifDate.iso8601("٢٠٢٦:٠٨:١٤ ١٥:٢٩:٠٣"))
    assertNull(ExifDate.iso8601("2026:08:1٤ 15:29:03"))
  }
}
