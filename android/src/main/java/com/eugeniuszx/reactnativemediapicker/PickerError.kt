package com.eugeniuszx.reactnativemediapicker

internal enum class PickerError(val code: String) {
  PERMISSION("permission"),
  CAMERA_UNAVAILABLE("camera_unavailable"),
  BUSY("busy"),
  OTHERS("others"),
}
