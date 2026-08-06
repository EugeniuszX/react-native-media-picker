package com.eugeniuszx.reactnativemediapicker

/**
 * Everything decided from metadata alone, before a single pixel is decoded:
 * whether the original bytes can be handed back untouched and, if not, what the
 * output should look like.
 *
 * All sizes are in *display* space — after the EXIF orientation is applied —
 * because that is the space the caller's maxWidth/maxHeight are expressed in.
 *
 * Kept in lockstep with `ios/Core/DecodePlan.swift`.
 */
internal data class DecodePlan(
  val displayWidth: Int,
  val displayHeight: Int,
  val targetWidth: Int,
  val targetHeight: Int,
  /** Power-of-two `inSampleSize`; never large enough to undershoot the target. */
  val sampleSize: Int,
  /** When false, the original encoded bytes are copied verbatim. */
  val needsTransform: Boolean,
) {
  companion object {
    fun compute(
      pixelWidth: Int,
      pixelHeight: Int,
      orientation: ExifOrientation,
      maxWidth: Int,
      maxHeight: Int,
      isAnimated: Boolean,
    ): DecodePlan {
      val displayWidth = if (orientation.swapsAxes) pixelHeight else pixelWidth
      val displayHeight = if (orientation.swapsAxes) pixelWidth else pixelHeight

      // Sizes are clamped to 0 because a failed metadata read reports a negative
      // dimension on both platforms (BitmapFactory.Options.outWidth is -1), and
      // a negative width must never reach JS. Kept identical to the Swift core.
      fun passthrough() = DecodePlan(
        displayWidth = displayWidth.coerceAtLeast(0),
        displayHeight = displayHeight.coerceAtLeast(0),
        targetWidth = displayWidth.coerceAtLeast(0),
        targetHeight = displayHeight.coerceAtLeast(0),
        sampleSize = 1,
        needsTransform = false,
      )

      if (displayWidth <= 0 || displayHeight <= 0 || isAnimated) return passthrough()

      val boundWidth = if (maxWidth > 0) maxWidth else displayWidth
      val boundHeight = if (maxHeight > 0) maxHeight else displayHeight
      if (displayWidth <= boundWidth && displayHeight <= boundHeight) return passthrough()

      // Integer arithmetic on purpose — see ios/Core/DecodePlan.swift for the
      // rationale, including why capping the bounds first is behaviour-neutral.
      // Cross-products are widened to Long because Kotlin's Int is 32-bit and
      // would overflow *silently* where Swift traps.
      val cappedWidth = minOf(boundWidth, displayWidth)
      val cappedHeight = minOf(boundHeight, displayHeight)
      val widthBinds = cappedWidth.toLong() * displayHeight <= cappedHeight.toLong() * displayWidth
      val targetWidth = if (widthBinds) {
        cappedWidth
      } else {
        (cappedHeight.toLong() * displayWidth / displayHeight).toInt().coerceAtLeast(1)
      }
      val targetHeight = if (widthBinds) {
        (cappedWidth.toLong() * displayHeight / displayWidth).toInt().coerceAtLeast(1)
      } else {
        cappedHeight
      }

      var sampleSize = 1
      while (displayWidth / (sampleSize * 2) >= targetWidth &&
        displayHeight / (sampleSize * 2) >= targetHeight
      ) {
        sampleSize *= 2
      }

      return DecodePlan(
        displayWidth = displayWidth,
        displayHeight = displayHeight,
        targetWidth = targetWidth,
        targetHeight = targetHeight,
        sampleSize = sampleSize,
        needsTransform = true,
      )
    }
  }
}
