package com.eugeniuszx.reactnativemediapicker

/**
 * The closed set of failure codes the JS side can receive. Mirrors the
 * `ErrorCode` union in `src/NativeReactNativeMediaPicker.ts`.
 */
internal enum class PickerError(val code: String) {
  PERMISSION("permission"),
  CAMERA_UNAVAILABLE("camera_unavailable"),
  OTHERS("others"),
}
