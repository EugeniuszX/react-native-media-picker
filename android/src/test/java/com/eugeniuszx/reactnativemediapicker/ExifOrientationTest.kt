package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifOrientationTest {
  @Test
  fun `maps all eight exif values`() {
    val expected = mapOf(
      1 to Pair(0, false),
      2 to Pair(0, true),
      3 to Pair(180, false),
      4 to Pair(180, true),
      5 to Pair(90, true),
      6 to Pair(90, false),
      7 to Pair(270, true),
      8 to Pair(270, false),
    )
    expected.forEach { (value, pair) ->
      val orientation = ExifOrientation.fromExifValue(value)
      assertEquals("exif $value rotation", pair.first, orientation.rotationDegrees)
      assertEquals("exif $value mirrored", pair.second, orientation.isMirrored)
    }
  }

  @Test
  fun `out of range values are treated as upright`() {
    assertEquals(ExifOrientation.UPRIGHT, ExifOrientation.fromExifValue(0))
    assertEquals(ExifOrientation.UPRIGHT, ExifOrientation.fromExifValue(9))
    assertEquals(ExifOrientation.UPRIGHT, ExifOrientation.fromExifValue(-1))
  }

  @Test
  fun `swaps axes for quarter turns only`() {
    listOf(5, 6, 7, 8).forEach {
      assertTrue("exif $it", ExifOrientation.fromExifValue(it).swapsAxes)
    }
    listOf(1, 2, 3, 4).forEach {
      assertFalse("exif $it", ExifOrientation.fromExifValue(it).swapsAxes)
    }
  }
}
