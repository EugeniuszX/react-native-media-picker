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

  @Test fun parsesCameraMediaType() {
    assertEquals(CameraMediaType.PHOTO, CameraMediaType.from("photo"))
    assertEquals(CameraMediaType.VIDEO, CameraMediaType.from("video"))
    assertEquals(CameraMediaType.PHOTO, CameraMediaType.from("mixed"))
    assertEquals(CameraMediaType.PHOTO, CameraMediaType.from(null))
  }

  @Test fun parsesVideoQuality() {
    assertEquals(VideoQuality.LOW, VideoQuality.from("low"))
    assertEquals(VideoQuality.MEDIUM, VideoQuality.from("medium"))
    assertEquals(VideoQuality.HIGH, VideoQuality.from("high"))
    assertEquals(VideoQuality.HIGH, VideoQuality.from("ultra"))
    assertEquals(VideoQuality.HIGH, VideoQuality.from(null))
  }

  @Test fun mapsVideoQualityToTheTwoValuesTheIntentExtraSupports() {
    assertEquals(0, VideoQuality.LOW.intentExtra)
    assertEquals(1, VideoQuality.MEDIUM.intentExtra)
    assertEquals(1, VideoQuality.HIGH.intentExtra)
  }
}
