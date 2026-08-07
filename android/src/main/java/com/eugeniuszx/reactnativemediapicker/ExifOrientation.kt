package com.eugeniuszx.reactnativemediapicker

internal data class ExifOrientation(
  val rotationDegrees: Int,
  /** Flip is applied after the rotation; the opposite order swaps EXIF 5 and 7. */
  val isMirrored: Boolean,
) {
  val swapsAxes: Boolean
    get() = rotationDegrees == 90 || rotationDegrees == 270

  companion object {
    val UPRIGHT = ExifOrientation(0, false)

    fun fromExifValue(value: Int): ExifOrientation = when (value) {
      2 -> ExifOrientation(0, true)
      3 -> ExifOrientation(180, false)
      4 -> ExifOrientation(180, true)
      5 -> ExifOrientation(90, true)
      6 -> ExifOrientation(90, false)
      7 -> ExifOrientation(270, true)
      8 -> ExifOrientation(270, false)
      else -> UPRIGHT
    }
  }
}
