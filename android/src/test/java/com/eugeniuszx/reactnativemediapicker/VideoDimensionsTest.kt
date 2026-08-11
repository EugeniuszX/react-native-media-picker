package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDimensionsTest {
  @Test fun keepsAxesForZeroAndHalfTurn() {
    assertEquals(1920 to 1080, VideoDimensions.displayed(1920, 1080, 0))
    assertEquals(1920 to 1080, VideoDimensions.displayed(1920, 1080, 180))
  }

  @Test fun swapsAxesForQuarterTurns() {
    assertEquals(1080 to 1920, VideoDimensions.displayed(1920, 1080, 90))
    assertEquals(1080 to 1920, VideoDimensions.displayed(1920, 1080, 270))
  }

  @Test fun normalizesRotationOutsideZeroTo359() {
    assertEquals(1080 to 1920, VideoDimensions.displayed(1920, 1080, 450))
    assertEquals(1080 to 1920, VideoDimensions.displayed(1920, 1080, -90))
  }
}
