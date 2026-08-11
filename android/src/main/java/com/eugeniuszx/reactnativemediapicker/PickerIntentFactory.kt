package com.eugeniuszx.reactnativemediapicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts

internal class PickerIntentFactory(private val context: Context) {
  fun mediaLibrary(selectionLimit: Int, mediaType: RequestedMediaType): Intent =
    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
      Intent(MediaStore.ACTION_PICK_IMAGES).apply {
        LibraryIntentPlan.pickImagesMimeType(mediaType)?.let { type = it }
        if (selectionLimit != 1) {
          val systemMax = MediaStore.getPickImagesMaxLimit()
          val max = if (selectionLimit == 0) systemMax else selectionLimit.coerceAtMost(systemMax)
          putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, max)
        }
      }
    } else {
      val mimeTypes = LibraryIntentPlan.getContentMimeTypes(mediaType)
      Intent(Intent.ACTION_GET_CONTENT).apply {
        type = if (mimeTypes.size == 1) mimeTypes.first() else "*/*"
        if (mimeTypes.size > 1) {
          putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        }
        addCategory(Intent.CATEGORY_OPENABLE)
        if (selectionLimit != 1) {
          putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
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
