package com.eugeniuszx.reactnativemediapicker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore

internal class PickerIntentFactory(private val context: Context) {
  fun mediaLibrary(selectionLimit: Int, mediaType: RequestedMediaType): Intent =
    when (val request = LibraryIntentPlan.request(libraryStrategy(), mediaType, selectionLimit)) {
      is LibraryRequest.SystemPicker -> systemPicker(request)
      is LibraryRequest.GetContent -> getContent(request)
    }

  fun documentLibrary(selectionLimit: Int, mediaType: RequestedMediaType): Intent =
    getContent(LibraryIntentPlan.getContent(mediaType, selectionLimit))

  private fun libraryStrategy(): LibraryStrategy {
    val extensionR = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)
    } else {
      null
    }
    return LibraryIntentPlan.strategy(Build.VERSION.SDK_INT, extensionR)
  }

  @SuppressLint("NewApi")
  private fun systemPicker(request: LibraryRequest.SystemPicker): Intent =
    Intent(MediaStore.ACTION_PICK_IMAGES).apply {
      request.mimeType?.let { type = it }
      request.maxItems?.let { requested ->
        val max = LibraryIntentPlan.pickImagesMax(requested, MediaStore.getPickImagesMaxLimit())
        putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, max)
      }
    }

  private fun getContent(request: LibraryRequest.GetContent): Intent =
    Intent(Intent.ACTION_GET_CONTENT).apply {
      val mimeTypes = request.mimeTypes
      type = if (mimeTypes.size == 1) mimeTypes.first() else "*/*"
      if (mimeTypes.size > 1) {
        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
      }
      addCategory(Intent.CATEGORY_OPENABLE)
      if (request.allowMultiple) {
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      }
    }

  fun imageCapture(outputUri: Uri, facing: CameraFacing): Intent =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
      putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
      addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
      if (facing == CameraFacing.FRONT) {
        putExtra("android.intent.extras.CAMERA_FACING", 1)
        putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
      }
    }

  fun videoCapture(
    outputUri: Uri,
    facing: CameraFacing,
    maxDurationSeconds: Int,
    videoQuality: VideoQuality,
  ): Intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
    putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    if (maxDurationSeconds > 0) {
      putExtra(MediaStore.EXTRA_DURATION_LIMIT, maxDurationSeconds)
    }
    putExtra(MediaStore.EXTRA_VIDEO_QUALITY, videoQuality.intentExtra)
    if (facing == CameraFacing.FRONT) {
      putExtra("android.intent.extras.CAMERA_FACING", 1)
      putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
      putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
    }
  }

  @Suppress("DEPRECATION")
  fun canBeHandled(intent: Intent): Boolean =
    context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()

  fun collectUris(data: Intent): List<Uri> {
    val clip = data.clipData
    if (clip != null) {
      return (0 until clip.itemCount).map { clip.getItemAt(it).uri }
    }
    val single = data.data ?: return emptyList()
    return listOf(single)
  }
}
