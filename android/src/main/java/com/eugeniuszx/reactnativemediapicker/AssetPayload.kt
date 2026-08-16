package com.eugeniuszx.reactnativemediapicker

internal data class Thumbnail(
  val uri: String,
  val width: Int,
  val height: Int,
)

internal data class AssetPayload(
  val uri: String,
  val mime: String,
  val fileName: String,
  val fileSize: Long,
  val width: Int,
  val height: Int,
  val base64: String?,
  val durationSeconds: Double? = null,
  val thumbnail: Thumbnail? = null,
  val exif: ExifPayload? = null,
)
