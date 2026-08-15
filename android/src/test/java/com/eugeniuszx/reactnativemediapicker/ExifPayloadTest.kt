package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifPayloadTest {
  @Test fun aFreshPayloadIsEmpty() {
    assertTrue(ExifPayload().isEmpty)
  }

  @Test fun anyPopulatedFieldMakesItNonEmpty() {
    assertFalse(ExifPayload(model = "Pixel 9").isEmpty)
    assertFalse(ExifPayload(latitude = 50.4501).isEmpty)
    assertFalse(ExifPayload(iso = 100).isEmpty)
  }
}
