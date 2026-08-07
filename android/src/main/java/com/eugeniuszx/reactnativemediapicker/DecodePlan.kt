package com.eugeniuszx.reactnativemediapicker

internal data class DecodePlan(
  val displayWidth: Int,
  val displayHeight: Int,
  val targetWidth: Int,
  val targetHeight: Int,
  val sampleSize: Int,
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
