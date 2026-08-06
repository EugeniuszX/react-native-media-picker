package com.eugeniuszx.reactnativemediapicker

/**
 * An EXIF orientation tag decomposed into the two operations we care about.
 * No Android imports on purpose — ExifInterface's constants are plain ints,
 * so the caller converts and this stays JVM-unit-testable.
 *
 * Having one mapping shared by the passthrough and transform paths is what
 * keeps reported dimensions and rendered pixels consistent.
 */
internal data class ExifOrientation(
  /** Clockwise rotation, in degrees, needed to display the buffer upright. */
  val rotationDegrees: Int,
  /**
   * Whether the buffer must also be flipped horizontally. The flip is applied
   * **after** the rotation, in the rotated coordinate space — with the opposite
   * order, EXIF 5 and EXIF 7 would silently swap meanings. `ImageProcessor`
   * honours this by calling `postRotate` before `postScale(-1f, 1f)`.
   */
  val isMirrored: Boolean,
) {
  /** True when the rotation transposes the buffer's width and height. */
  val swapsAxes: Boolean
    get() = rotationDegrees == 90 || rotationDegrees == 270

  companion object {
    val UPRIGHT = ExifOrientation(0, false)

    /**
     * Maps a raw EXIF orientation tag (1..8). Anything outside that range is
     * treated as upright, which is how decoders behave on malformed metadata.
     */
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
