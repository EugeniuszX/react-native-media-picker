package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodePlanTest {
  private fun plan(
    width: Int,
    height: Int,
    maxWidth: Int = 0,
    maxHeight: Int = 0,
    exif: Int = 1,
    isAnimated: Boolean = false,
  ): DecodePlan = DecodePlan.compute(
    pixelWidth = width,
    pixelHeight = height,
    orientation = ExifOrientation.fromExifValue(exif),
    maxWidth = maxWidth,
    maxHeight = maxHeight,
    isAnimated = isAnimated,
  )

  @Test
  fun `no bounds means passthrough`() {
    val p = plan(4000, 3000)
    assertFalse(p.needsTransform)
    assertEquals(4000, p.displayWidth)
    assertEquals(3000, p.displayHeight)
    assertEquals(1, p.sampleSize)
  }

  @Test
  fun `image already within bounds is passed through`() {
    val p = plan(600, 400, maxWidth = 640, maxHeight = 640)
    assertFalse(p.needsTransform)
    assertEquals(600, p.targetWidth)
    assertEquals(400, p.targetHeight)
  }

  @Test
  fun `animated images ignore bounds`() {
    val p = plan(4000, 3000, maxWidth = 100, maxHeight = 100, isAnimated = true)
    assertFalse(p.needsTransform)
    assertEquals(4000, p.targetWidth)
  }

  @Test
  fun `scales down preserving aspect ratio`() {
    val p = plan(4000, 2000, maxWidth = 640, maxHeight = 640)
    assertTrue(p.needsTransform)
    assertEquals(640, p.targetWidth)
    assertEquals(320, p.targetHeight)
  }

  @Test
  fun `a single axis bound constrains that axis only`() {
    val wide = plan(4000, 2000, maxWidth = 1000, maxHeight = 0)
    assertEquals(1000, wide.targetWidth)
    assertEquals(500, wide.targetHeight)

    val tall = plan(2000, 4000, maxWidth = 0, maxHeight = 1000)
    assertEquals(500, tall.targetWidth)
    assertEquals(1000, tall.targetHeight)
  }

  @Test
  fun `bounds apply to displayed axes for rotated images`() {
    val p = plan(4000, 3000, maxWidth = 600, maxHeight = 600, exif = 6)
    assertEquals(3000, p.displayWidth)
    assertEquals(4000, p.displayHeight)
    assertTrue(p.needsTransform)
    assertEquals(450, p.targetWidth)
    assertEquals(600, p.targetHeight)
  }

  @Test
  fun `rotated image within bounds after swap is passed through`() {
    val p = plan(400, 800, maxWidth = 900, maxHeight = 900, exif = 6)
    assertFalse(p.needsTransform)
    assertEquals(800, p.displayWidth)
    assertEquals(400, p.displayHeight)
  }

  @Test
  fun `sample size is the largest power of two that still covers the target`() {
    assertEquals(4, plan(4000, 3000, maxWidth = 1000, maxHeight = 1000).sampleSize)
    assertEquals(8, plan(4000, 3000, maxWidth = 500, maxHeight = 500).sampleSize)
    assertEquals(16, plan(4000, 3000, maxWidth = 250, maxHeight = 250).sampleSize)
    assertEquals(1, plan(4000, 3000, maxWidth = 2500, maxHeight = 2500).sampleSize)
  }

  @Test
  fun `downsampled buffer never falls below the target`() {
    val p = plan(4000, 3000, maxWidth = 700, maxHeight = 700)
    assertTrue(p.displayWidth / p.sampleSize >= p.targetWidth)
    assertTrue(p.displayHeight / p.sampleSize >= p.targetHeight)
  }

  @Test
  fun `binding axis lands exactly on its bound`() {
    val large = plan(5712, 4284, maxWidth = 1000, maxHeight = 1000)
    assertEquals(1000, large.targetWidth)
    assertEquals(750, large.targetHeight)

    val medium = plan(3088, 2320, maxWidth = 800, maxHeight = 800)
    assertEquals(800, medium.targetWidth)
    assertEquals(601, medium.targetHeight)
  }

  @Test
  fun `a bound larger than the source is never the binding axis`() {
    val p = plan(4000, 3000, maxWidth = Int.MAX_VALUE, maxHeight = 500)
    assertEquals(666, p.targetWidth)
    assertEquals(500, p.targetHeight)
  }

  @Test
  fun `target never collapses to zero`() {
    val p = plan(4000, 10, maxWidth = 1, maxHeight = 1)
    assertTrue(p.targetWidth >= 1)
    assertTrue(p.targetHeight >= 1)
  }

  @Test
  fun `unreadable dimensions produce passthrough with zero size`() {
    val p = plan(0, 0, maxWidth = 640, maxHeight = 640)
    assertFalse(p.needsTransform)
    assertEquals(0, p.displayWidth)
    assertEquals(1, p.sampleSize)
  }

  @Test
  fun `negative dimensions are clamped to zero`() {
    val p = plan(-1, -1, maxWidth = 640, maxHeight = 640)
    assertFalse(p.needsTransform)
    assertEquals(0, p.displayWidth)
    assertEquals(0, p.displayHeight)
    assertEquals(0, p.targetWidth)
    assertEquals(0, p.targetHeight)
  }
}
