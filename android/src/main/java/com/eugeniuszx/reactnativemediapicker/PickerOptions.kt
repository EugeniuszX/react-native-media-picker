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

internal enum class RequestedMediaType {
  PHOTO,
  VIDEO,
  MIXED,
  ;

  companion object {
    fun from(raw: String?): RequestedMediaType = when (raw) {
      "video" -> VIDEO
      "mixed" -> MIXED
      else -> PHOTO
    }
  }
}

internal data class LibraryOptions(
  val selectionLimit: Int,
  val maxWidth: Int,
  val maxHeight: Int,
  val quality: Int,
  val includeBase64: Boolean,
  val format: RequestedFormat,
  val mediaType: RequestedMediaType,
  val includeThumbnail: Boolean,
  val includeExif: Boolean,
  val stripMetadata: Boolean,
) {
  companion object {
    fun from(map: ReadableMap) = LibraryOptions(
      selectionLimit = map.getInt("selectionLimit"),
      maxWidth = map.getInt("maxWidth"),
      maxHeight = map.getInt("maxHeight"),
      quality = toCompressQuality(map.getDouble("quality")),
      includeBase64 = map.getBoolean("includeBase64"),
      format = RequestedFormat.from(map.getString("format")),
      mediaType = RequestedMediaType.from(map.getString("mediaType")),
      includeThumbnail = map.getBoolean("includeThumbnail"),
      includeExif = map.getBoolean("includeExif"),
      stripMetadata = map.getBoolean("stripMetadata"),
    )
  }
}

internal data class CameraOptions(
  val facing: CameraFacing,
  val maxWidth: Int,
  val maxHeight: Int,
  val quality: Int,
  val includeBase64: Boolean,
  val format: RequestedFormat,
) {
  companion object {
    fun from(map: ReadableMap) = CameraOptions(
      facing = CameraFacing.from(map.getString("cameraType")),
      maxWidth = map.getInt("maxWidth"),
      maxHeight = map.getInt("maxHeight"),
      quality = toCompressQuality(map.getDouble("quality")),
      includeBase64 = map.getBoolean("includeBase64"),
      format = RequestedFormat.from(map.getString("format")),
    )
  }
}

private fun toCompressQuality(quality: Double): Int =
  (quality * 100).toInt().coerceIn(0, 100)
