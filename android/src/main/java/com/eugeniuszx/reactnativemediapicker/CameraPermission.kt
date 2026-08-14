package com.eugeniuszx.reactnativemediapicker

/**
 * The camera permission status reported to JS. [value] is the string the JS union is made of.
 *
 * [NOT_REQUIRED] means the app does not declare `android.permission.CAMERA`, so the system camera
 * app is launched without any runtime permission. [UNAVAILABLE] means the device has no camera at
 * all — a device that has one but no app able to handle the capture intent still reports its real
 * permission status here, and surfaces as `camera_unavailable` from `launchCamera`.
 */
internal enum class CameraPermission(val value: String) {
  GRANTED("granted"),
  NOT_DETERMINED("not_determined"),
  DENIED("denied"),
  BLOCKED("blocked"),
  NOT_REQUIRED("not_required"),
  UNAVAILABLE("unavailable"),
  ;

  companion object {
    /**
     * [shouldShowRationale] alone cannot tell "never asked" from "permanently denied" — both report
     * false — so [hasAsked] is tracked by this library and consulted first.
     */
    fun resolve(
      hasCamera: Boolean,
      declared: Boolean,
      granted: Boolean,
      hasAsked: Boolean,
      shouldShowRationale: Boolean,
    ): CameraPermission = when {
      !hasCamera -> UNAVAILABLE
      granted -> GRANTED
      !declared -> NOT_REQUIRED
      !hasAsked -> NOT_DETERMINED
      shouldShowRationale -> DENIED
      else -> BLOCKED
    }
  }
}
