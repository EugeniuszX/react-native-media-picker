package com.eugeniuszx.reactnativemediapicker

import com.facebook.react.bridge.ReadableMap

internal enum class CameraFacing {
  BACK,
  FRONT,
  ;

  companion object {
    /** Unknown values fall back to BACK, matching the JS-side normalization. */
    fun from(raw: String?): CameraFacing =
      if (raw == "front") FRONT else BACK
  }
}

/**
 * Immutable per-request options. Parsed once at the module boundary so nothing
 * downstream can be mutated mid-batch by a subsequent call.
 *
 * `quality` is converted from the JS 0..1 range to the 0..100 that
 * [android.graphics.Bitmap.compress] expects, right here and nowhere else.
 */
internal data class LibraryOptions(
  /** 0 means unlimited. */
  val selectionLimit: Int,
  /** 0 means "no bound on this axis". */
  val maxWidth: Int,
  val maxHeight: Int,
  /** 0..100. */
  val quality: Int,
  val includeBase64: Boolean,
) {
  companion object {
    fun from(map: ReadableMap) = LibraryOptions(
      selectionLimit = map.getInt("selectionLimit"),
      maxWidth = map.getInt("maxWidth"),
      maxHeight = map.getInt("maxHeight"),
      quality = toCompressQuality(map.getDouble("quality")),
      includeBase64 = map.getBoolean("includeBase64"),
    )
  }
}

internal data class CameraOptions(
  val facing: CameraFacing,
  val maxWidth: Int,
  val maxHeight: Int,
  val quality: Int,
  val includeBase64: Boolean,
) {
  companion object {
    fun from(map: ReadableMap) = CameraOptions(
      facing = CameraFacing.from(map.getString("cameraType")),
      maxWidth = map.getInt("maxWidth"),
      maxHeight = map.getInt("maxHeight"),
      quality = toCompressQuality(map.getDouble("quality")),
      includeBase64 = map.getBoolean("includeBase64"),
    )
  }
}

private fun toCompressQuality(quality: Double): Int =
  (quality * 100).toInt().coerceIn(0, 100)
