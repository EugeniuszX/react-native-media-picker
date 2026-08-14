package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThumbnailPlanTest {
  @Test
  fun `a frame already within the bound is not scaled`() {
    assertNull(ThumbnailPlan.scaledSize(320, 240, bound = 512))
    assertNull(ThumbnailPlan.scaledSize(512, 512, bound = 512))
  }

  @Test
  fun `a landscape frame is bound by its width`() {
    assertEquals(512 to 288, ThumbnailPlan.scaledSize(1920, 1080, bound = 512))
  }

  @Test
  fun `a portrait frame is bound by its height`() {
    assertEquals(288 to 512, ThumbnailPlan.scaledSize(1080, 1920, bound = 512))
  }

  @Test
  fun `a square frame stays square`() {
    assertEquals(512 to 512, ThumbnailPlan.scaledSize(2000, 2000, bound = 512))
  }

  @Test
  fun `an extreme aspect ratio never collapses to zero`() {
    assertEquals(512 to 1, ThumbnailPlan.scaledSize(10_000, 3, bound = 512))
    assertEquals(1 to 512, ThumbnailPlan.scaledSize(3, 10_000, bound = 512))
  }

  @Test
  fun `unusable sizes produce no plan`() {
    assertNull(ThumbnailPlan.scaledSize(0, 1080))
    assertNull(ThumbnailPlan.scaledSize(1920, 0))
    assertNull(ThumbnailPlan.scaledSize(-1, -1))
    assertNull(ThumbnailPlan.scaledSize(1920, 1080, bound = 0))
  }
}
