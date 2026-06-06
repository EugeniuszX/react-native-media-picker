package com.eugeniuszx.reactnativemediapicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ReactNativeMediaPickerModule(private val reactContext: ReactApplicationContext) :
  NativeReactNativeMediaPickerSpec(reactContext),
  com.facebook.react.bridge.ActivityEventListener {

  private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Volatile private var pickerPromise: Promise? = null
  @Volatile private var maxWidth: Int = 0
  @Volatile private var maxHeight: Int = 0
  @Volatile private var quality: Int = 100
  @Volatile private var includeBase64: Boolean = false

  init {
    reactContext.addActivityEventListener(this)
  }

  override fun getName() = NAME

  override fun launchImageLibrary(options: ReadableMap, promise: Promise) {
    val activity = reactContext.currentActivity
    if (activity == null) {
      promise.resolve(errorResponse("others", "Activity is null"))
      return
    }
    if (pickerPromise != null) {
      promise.resolve(errorResponse("others", "Already waiting for a pick."))
      return
    }

    pickerPromise = promise

    try {
      this.maxWidth = options.getInt("maxWidth")
      this.maxHeight = options.getInt("maxHeight")
      this.quality = (options.getDouble("quality") * 100).toInt().coerceIn(1, 100)
      this.includeBase64 = options.getBoolean("includeBase64")
      val selectionLimit = options.getInt("selectionLimit")

      val photoPickerAvailable =
        ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(reactContext)
      val intent = if (photoPickerAvailable) {
        Intent(MediaStore.ACTION_PICK_IMAGES).apply {
          type = "image/*"
          if (selectionLimit != 1) {
            val max = if (selectionLimit == 0) {
              MediaStore.getPickImagesMaxLimit()
            } else {
              selectionLimit.coerceAtMost(MediaStore.getPickImagesMaxLimit())
            }
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, max)
          }
        }
      } else {
        Intent(Intent.ACTION_GET_CONTENT).apply {
          type = "image/*"
          addCategory(Intent.CATEGORY_OPENABLE)
          if (selectionLimit != 1) {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
          }
        }
      }
      activity.startActivityForResult(intent, REQUEST_CODE)
    } catch (e: Exception) {
      pickerPromise = null
      promise.resolve(errorResponse("others", e.message ?: "launch error"))
    }
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ) {
    if (requestCode != REQUEST_CODE) return

    val promise = pickerPromise ?: return
    pickerPromise = null

    if (resultCode != Activity.RESULT_OK || data == null) {
      promise.resolve(cancelResponse())
      return
    }

    val uris = collectUris(data)
    if (uris.isEmpty()) {
      promise.resolve(cancelResponse())
      return
    }

    // Capture options on the main thread before launching the IO coroutine, so a
    // subsequent launchImageLibrary() cannot mutate them mid-batch.
    val reqMaxWidth = maxWidth
    val reqMaxHeight = maxHeight
    val reqQuality = quality
    val reqIncludeBase64 = includeBase64

    moduleScope.launch {
      try {
        val assets: WritableArray = Arguments.createArray()
        uris.forEach { uri ->
          assets.pushMap(processImage(uri, reqMaxWidth, reqMaxHeight, reqQuality, reqIncludeBase64))
        }
        val response = Arguments.createMap().apply {
          putBoolean("didCancel", false)
          putArray("assets", assets)
        }
        promise.resolve(response)
      } catch (e: Exception) {
        promise.resolve(errorResponse("others", e.message ?: "processing error"))
      }
    }
  }

  override fun onNewIntent(intent: Intent) {}

  override fun invalidate() {
    pickerPromise?.resolve(errorResponse("others", "Module destroyed before result."))
    pickerPromise = null
    moduleScope.cancel()
    super.invalidate()
  }

  private fun collectUris(data: Intent): List<Uri> {
    val clip = data.clipData
    if (clip != null) {
      return (0 until clip.itemCount).map { clip.getItemAt(it).uri }
    }
    val single = data.data ?: return emptyList()
    return listOf(single)
  }

  private fun cancelResponse(): WritableMap =
    Arguments.createMap().apply { putBoolean("didCancel", true) }

  private fun errorResponse(code: String, message: String): WritableMap =
    Arguments.createMap().apply {
      putBoolean("didCancel", false)
      putString("errorCode", code)
      putString("errorMessage", message)
    }

  /**
   * Decodes, rotates, scales and JPEG-compresses [uri] into a temp file in cacheDir.
   * The returned file lives in the app cache; the caller owns its lifecycle (the OS
   * reclaims cacheDir under storage pressure).
   */
  private fun processImage(
    uri: Uri,
    maxWidth: Int,
    maxHeight: Int,
    quality: Int,
    includeBase64: Boolean,
  ): WritableMap {
    val resolver = reactContext.contentResolver
    val reqW = if (maxWidth > 0) maxWidth else Int.MAX_VALUE
    val reqH = if (maxHeight > 0) maxHeight else Int.MAX_VALUE

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
    }
    var bitmap = resolver.openInputStream(uri).use {
      BitmapFactory.decodeStream(it, null, decodeOpts)
    } ?: throw IllegalStateException("Failed to decode image")

    bitmap = applyExifRotation(uri, bitmap)
    bitmap = scaleToFit(bitmap, reqW, reqH)

    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
    val jpegBytes = baos.toByteArray()

    val outFile = File.createTempFile("media_picker_", ".jpg", reactContext.cacheDir)
    FileOutputStream(outFile).use { it.write(jpegBytes) }

    val width = bitmap.width
    val height = bitmap.height
    bitmap.recycle()

    return Arguments.createMap().apply {
      putString("uri", Uri.fromFile(outFile).toString())
      putString("type", "image/jpeg")
      putString("fileName", outFile.name)
      putDouble("fileSize", jpegBytes.size.toDouble())
      putInt("width", width)
      putInt("height", height)
      if (includeBase64) {
        putString("base64", Base64.encodeToString(jpegBytes, Base64.NO_WRAP))
      }
    }
  }

  private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
    if (srcW <= 0 || srcH <= 0) return 1
    var sample = 1
    val halfW = srcW / 2
    val halfH = srcH / 2
    while (halfW / sample >= reqW && halfH / sample >= reqH) {
      sample *= 2
    }
    return sample
  }

  private fun scaleToFit(bitmap: Bitmap, maxW: Int, maxH: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxW && h <= maxH) return bitmap
    val ratio = minOf(maxW.toFloat() / w, maxH.toFloat() / h)
    val newW = (w * ratio).toInt().coerceAtLeast(1)
    val newH = (h * ratio).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
  }

  private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
    val orientation = reactContext.contentResolver.openInputStream(uri).use { stream ->
      stream ?: return bitmap
      ExifInterface(stream).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
      )
    }

    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      // TODO(phase-2): handle FLIP_HORIZONTAL/VERTICAL/TRANSPOSE/TRANSVERSE (mirrored shots).
      else -> return bitmap
    }

    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated != bitmap) bitmap.recycle()
    return rotated
  }

  companion object {
    const val NAME = NativeReactNativeMediaPickerSpec.NAME
    private const val REQUEST_CODE = 48211
  }
}
