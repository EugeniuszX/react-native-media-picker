package com.eugeniuszx.reactnativemediapicker

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

internal class VideoProcessor(
  private val contentResolver: ContentResolver,
  private val tempFiles: TempFileStore,
) {
  fun process(
    uri: Uri,
    includeThumbnail: Boolean = false,
    suggestedName: String? = null,
  ): AssetPayload {
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
    var thumbnail: Thumbnail? = null
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
      if (includeThumbnail) {
        thumbnail = makeThumbnail(retriever)
      }
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
      fileName = AssetFileName.resolve(suggestedName, file.name, file.extension),
      fileSize = file.length(),
      width = width,
      height = height,
      base64 = null,
      durationSeconds = durationSeconds,
      thumbnail = thumbnail,
    )
  }

  private fun makeThumbnail(retriever: MediaMetadataRetriever): Thumbnail? {
    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      ?: retriever.frameAtTime
      ?: return null

    var scaled: Bitmap? = null
    val file = tempFiles.createFile("jpg")
    return try {
      val target = ThumbnailPlan.scaledSize(frame.width, frame.height)
      val output = if (target == null) {
        frame
      } else {
        Bitmap.createScaledBitmap(frame, target.first, target.second, true)
          .also { scaled = it }
      }
      file.outputStream().use { stream ->
        if (!output.compress(Bitmap.CompressFormat.JPEG, ThumbnailPlan.JPEG_QUALITY, stream)) {
          throw IllegalStateException("failed to encode the thumbnail")
        }
      }
      Thumbnail(
        uri = Uri.fromFile(file).toString(),
        width = output.width,
        height = output.height,
      )
    } catch (e: Exception) {
      Log.w(ReactNativeMediaPickerModule.NAME, "failed to build a video thumbnail", e)
      file.delete()
      null
    } catch (e: OutOfMemoryError) {
      Log.w(ReactNativeMediaPickerModule.NAME, "out of memory while building a thumbnail", e)
      file.delete()
      null
    } finally {
      scaled?.recycle()
      frame.recycle()
    }
  }
}
