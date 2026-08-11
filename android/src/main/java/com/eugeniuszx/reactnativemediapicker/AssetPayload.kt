package com.eugeniuszx.reactnativemediapicker

internal data class AssetPayload(
  val uri: String,
  val mime: String,
  val fileName: String,
  val fileSize: Long,
  val width: Int,
  val height: Int,
  val base64: String?,
  val durationSeconds: Double? = null,
)
