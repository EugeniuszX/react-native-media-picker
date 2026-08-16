package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Test

class PickerErrorTest {
  @Test fun codesMatchThePublicErrorCodeUnion() {
    assertEquals("permission", PickerError.PERMISSION.code)
    assertEquals("camera_unavailable", PickerError.CAMERA_UNAVAILABLE.code)
    assertEquals("busy", PickerError.BUSY.code)
    assertEquals("others", PickerError.OTHERS.code)
  }
}
