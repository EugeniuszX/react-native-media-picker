package com.eugeniuszx.reactnativemediapicker

/**
 * One picked item, framework-free. [ResponseFactory] is the only place that
 * turns this into a `WritableMap`, so asset keys are spelled out exactly once.
 * Mirrors `ios/Core/AssetPayload.swift`.
 */
internal data class AssetPayload(
  val uri: String,
  val mime: String,
  val fileName: String,
  val fileSize: Long,
  val width: Int,
  val height: Int,
  val base64: String?,
)
