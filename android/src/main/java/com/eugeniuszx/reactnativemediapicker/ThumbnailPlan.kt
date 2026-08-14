package com.eugeniuszx.reactnativemediapicker

internal object ThumbnailPlan {
  const val MAX_DIMENSION = 512

  const val JPEG_QUALITY = 80

  fun scaledSize(width: Int, height: Int, bound: Int = MAX_DIMENSION): Pair<Int, Int>? {
    if (width <= 0 || height <= 0 || bound <= 0) return null
    if (width <= bound && height <= bound) return null

    return if (width >= height) {
      bound to (bound.toLong() * height / width).toInt().coerceAtLeast(1)
    } else {
      (bound.toLong() * width / height).toInt().coerceAtLeast(1) to bound
    }
  }
}
