package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Test

class PickerOptionsTest {
  @Test fun parsesRequestedMediaType() {
    assertEquals(RequestedMediaType.PHOTO, RequestedMediaType.from("photo"))
    assertEquals(RequestedMediaType.VIDEO, RequestedMediaType.from("video"))
    assertEquals(RequestedMediaType.MIXED, RequestedMediaType.from("mixed"))
    assertEquals(RequestedMediaType.PHOTO, RequestedMediaType.from("garbage"))
    assertEquals(RequestedMediaType.PHOTO, RequestedMediaType.from(null))
  }
}
