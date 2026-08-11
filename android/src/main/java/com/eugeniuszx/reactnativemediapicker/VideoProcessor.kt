package com.eugeniuszx.reactnativemediapicker

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

internal class VideoProcessor(
  private val contentResolver: ContentResolver,
  private val tempFiles: TempFileStore,
) {
  fun process(uri: Uri): AssetPayload {
    val mime = MediaFormat.normalizeVideoMime(contentResolver.getType(uri))
    val file = tempFiles.createFile(MediaFormat.extensionForVideoMime(mime))
    try {
      contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open $uri" }
        file.outputStream().use { output -> input.copyTo(output) }
      }
    } catch (e: Exception) {
      file.delete()
      throw e
    }

    var durationSeconds: Double? = null
    var width = 0
    var height = 0
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(file.path)
      durationSeconds = MediaFormat.durationSecondsFrom(
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      )
      val rawWidth = retriever
        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        ?.toIntOrNull() ?: 0
      val rawHeight = retriever
        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        ?.toIntOrNull() ?: 0
      val rotation = retriever
        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        ?.toIntOrNull() ?: 0
      val displayed = VideoDimensions.displayed(rawWidth, rawHeight, rotation)
      width = displayed.first
      height = displayed.second
    } catch (e: Exception) {
      Log.w(ReactNativeMediaPickerModule.NAME, "failed to read video metadata for $uri", e)
    } finally {
      try {
        retriever.release()
      } catch (e: Exception) {
        Log.w(ReactNativeMediaPickerModule.NAME, "failed to release retriever", e)
      }
    }

    return AssetPayload(
      uri = Uri.fromFile(file).toString(),
      mime = mime,
      fileName = file.name,
      fileSize = file.length(),
      width = width,
      height = height,
      base64 = null,
      durationSeconds = durationSeconds,
    )
  }
}
