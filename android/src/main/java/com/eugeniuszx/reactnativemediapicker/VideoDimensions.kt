package com.eugeniuszx.reactnativemediapicker

internal object VideoDimensions {
  fun displayed(width: Int, height: Int, rotationDegrees: Int): Pair<Int, Int> =
    when (((rotationDegrees % 360) + 360) % 360) {
      90, 270 -> height to width
      else -> width to height
    }
}
