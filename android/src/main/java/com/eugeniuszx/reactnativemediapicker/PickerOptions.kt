package com.eugeniuszx.reactnativemediapicker

import com.facebook.react.bridge.ReadableMap

internal enum class CameraFacing {
  BACK,
  FRONT,
  ;

  companion object {
    fun from(raw: String?): CameraFacing =
      if (raw == "front") FRONT else BACK
  }
}

internal data class LibraryOptions(
  val selectionLimit: Int,
  val maxWidth: Int,
  val maxHeight: Int,
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
