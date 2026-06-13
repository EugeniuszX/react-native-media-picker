package com.eugeniuszx.reactnativemediapicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
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
  @Volatile private var selectionLimit: Int = 1
  @Volatile private var cameraType: String = "back"
  @Volatile private var cameraFile: File? = null

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
      this.quality = (options.getDouble("quality") * 100).toInt().coerceIn(0, 100)
      this.includeBase64 = options.getBoolean("includeBase64")
      this.selectionLimit = options.getInt("selectionLimit")

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

  override fun launchCamera(options: ReadableMap, promise: Promise) {
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
      this.quality = (options.getDouble("quality") * 100).toInt().coerceIn(0, 100)
      this.includeBase64 = options.getBoolean("includeBase64")
      this.cameraType = options.getString("cameraType") ?: "back"

      if (isCameraPermissionDeclared() && !isCameraPermissionGranted()) {
        requestCameraPermission(activity)
      } else {
        launchCameraIntent(activity)
      }
    } catch (e: Exception) {
      pickerPromise = null
      promise.resolve(errorResponse("others", e.message ?: "launch error"))
    }
  }

  private fun isCameraPermissionDeclared(): Boolean =
    try {
      val info = reactContext.packageManager.getPackageInfo(
        reactContext.packageName,
        PackageManager.GET_PERMISSIONS,
      )
      info.requestedPermissions?.contains(Manifest.permission.CAMERA) == true
    } catch (e: Exception) {
      false
    }

  private fun isCameraPermissionGranted(): Boolean =
    ContextCompat.checkSelfPermission(reactContext, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED

  private fun requestCameraPermission(activity: Activity) {
    val listener = PermissionListener { requestCode, _, grantResults ->
      if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) {
        return@PermissionListener false
      }
      val granted =
        grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
      if (granted) {
        val act = reactContext.currentActivity
        if (act != null) {
          launchCameraIntent(act)
        } else {
          pickerPromise?.resolve(errorResponse("others", "Activity is null"))
          pickerPromise = null
        }
      } else {
        pickerPromise?.resolve(errorResponse("permission", "Camera permission denied"))
        pickerPromise = null
      }
      true
    }
    val permissionActivity = activity as? PermissionAwareActivity
    if (permissionActivity == null) {
      pickerPromise?.resolve(
        errorResponse("others", "Host activity does not support runtime permission requests")
      )
      pickerPromise = null
      return
    }
    permissionActivity.requestPermissions(
      arrayOf(Manifest.permission.CAMERA),
      CAMERA_PERMISSION_REQUEST_CODE,
      listener,
    )
  }

  private fun launchCameraIntent(activity: Activity) {
    val photoFile = File.createTempFile("media_picker_capture_", ".jpg", reactContext.cacheDir)
    cameraFile = photoFile
    val authority = "${reactContext.packageName}.rnmediapicker.fileprovider"
    val outputUri = FileProvider.getUriForFile(reactContext, authority, photoFile)

    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
      putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
      addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
      if (cameraType == "front") {
        putExtra("android.intent.extras.CAMERA_FACING", 1)
        putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
      }
    }

    if (intent.resolveActivity(reactContext.packageManager) == null) {
      cameraFile?.delete()
      cameraFile = null
      pickerPromise?.resolve(errorResponse("camera_unavailable", "No camera app available"))
      pickerPromise = null
      return
    }

    activity.startActivityForResult(intent, CAMERA_REQUEST_CODE)
  }

  private fun handleCameraResult(resultCode: Int) {
    val promise = pickerPromise ?: return
    pickerPromise = null

    val file = cameraFile
    cameraFile = null

    if (resultCode != Activity.RESULT_OK || file == null || !file.exists() || file.length() == 0L) {
      file?.delete()
      promise.resolve(cancelResponse())
      return
    }

    val reqMaxWidth = maxWidth
    val reqMaxHeight = maxHeight
    val reqQuality = quality
    val reqIncludeBase64 = includeBase64

    moduleScope.launch {
      try {
        val asset = processImage(Uri.fromFile(file), reqMaxWidth, reqMaxHeight, reqQuality, reqIncludeBase64)
        val assets = Arguments.createArray().apply { pushMap(asset) }
        val response = Arguments.createMap().apply {
          putBoolean("didCancel", false)
          putArray("assets", assets)
        }
        promise.resolve(response)
      } catch (e: Exception) {
        promise.resolve(errorResponse("others", e.message ?: "processing error"))
      } finally {
        file.delete()
      }
    }
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ) {
    if (requestCode == CAMERA_REQUEST_CODE) {
      handleCameraResult(resultCode)
      return
    }
    if (requestCode != REQUEST_CODE) return

    val promise = pickerPromise ?: return
    pickerPromise = null

    if (resultCode != Activity.RESULT_OK || data == null) {
      promise.resolve(cancelResponse())
      return
    }

    val reqSelectionLimit = selectionLimit
    val collected = collectUris(data)
    val uris = if (reqSelectionLimit in 2..Int.MAX_VALUE) collected.take(reqSelectionLimit) else collected
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
   * Decodes [uri], deciding between a lossless passthrough (animated images, or
   * when no resize is needed) and a transform (decode → EXIF-rotate → scale →
   * re-encode in the source format, with HEIC/GIF falling back to JPEG).
   * The returned file lives in cacheDir; the caller owns its lifecycle.
   */
  private fun processImage(
    uri: Uri,
    maxWidth: Int,
    maxHeight: Int,
    quality: Int,
    includeBase64: Boolean,
  ): WritableMap {
    val resolver = reactContext.contentResolver
    val srcMime = MediaFormat.normalizeMime(resolver.getType(uri))

    // GIF and animated WebP can't be re-encoded frame-by-frame, so they always
    // pass through untouched (resize is intentionally ignored for them).
    val usePassthroughFormat = srcMime == "image/gif" ||
      (srcMime == "image/webp" && isAnimatedWebpUri(uri))

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

    val resizeRequested = maxWidth > 0 || maxHeight > 0
    val exceedsBounds =
      (maxWidth > 0 && bounds.outWidth > maxWidth) ||
      (maxHeight > 0 && bounds.outHeight > maxHeight)

    return if (usePassthroughFormat || !resizeRequested || !exceedsBounds) {
      passthrough(uri, srcMime, bounds.outWidth, bounds.outHeight, includeBase64)
    } else {
      transform(uri, srcMime, bounds, maxWidth, maxHeight, quality, includeBase64)
    }
  }

  /** Copies the original encoded bytes verbatim, preserving format and EXIF. */
  private fun passthrough(
    uri: Uri,
    mime: String,
    srcWidth: Int,
    srcHeight: Int,
    includeBase64: Boolean,
  ): WritableMap {
    val resolver = reactContext.contentResolver
    val ext = MediaFormat.extensionForMime(mime)
    val outFile = File.createTempFile("media_picker_", ".$ext", reactContext.cacheDir)

    // Stream-copy by default (low memory); only hold bytes in memory when base64
    // is requested, to avoid reading the file twice.
    var base64: String? = null
    resolver.openInputStream(uri).use { input ->
      input ?: throw IllegalStateException("Failed to open image stream")
      if (includeBase64) {
        val bytes = input.readBytes()
        outFile.writeBytes(bytes)
        base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      } else {
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
      }
    }

    // GIF carries no EXIF, so isExifAxisSwapped returns false for it; the others
    // honor the orientation tag so reported dimensions match how it's displayed.
    val swap = isExifAxisSwapped(uri)
    val width = if (swap) srcHeight else srcWidth
    val height = if (swap) srcWidth else srcHeight

    return Arguments.createMap().apply {
      putString("uri", Uri.fromFile(outFile).toString())
      putString("type", mime)
      putString("fileName", outFile.name)
      putDouble("fileSize", outFile.length().toDouble())
      putInt("width", width)
      putInt("height", height)
      val b64 = base64
      if (b64 != null) {
        putString("base64", b64)
      }
    }
  }

  /** Decodes, rotates, scales and re-encodes in the source format (HEIC/GIF → JPEG). */
  private fun transform(
    uri: Uri,
    srcMime: String,
    bounds: BitmapFactory.Options,
    maxWidth: Int,
    maxHeight: Int,
    quality: Int,
    includeBase64: Boolean,
  ): WritableMap {
    val resolver = reactContext.contentResolver
    val reqW = if (maxWidth > 0) maxWidth else Int.MAX_VALUE
    val reqH = if (maxHeight > 0) maxHeight else Int.MAX_VALUE

    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
    }
    var bitmap = resolver.openInputStream(uri).use {
      BitmapFactory.decodeStream(it, null, decodeOpts)
    } ?: throw IllegalStateException("Failed to decode image")

    bitmap = applyExifRotation(uri, bitmap)
    bitmap = scaleToFit(bitmap, reqW, reqH)

    val outFormat = MediaFormat.reencodeFormat(srcMime)
    val compressFormat = when (outFormat) {
      MediaFormat.OutputFormat.PNG -> Bitmap.CompressFormat.PNG
      MediaFormat.OutputFormat.WEBP -> webpCompressFormat()
      MediaFormat.OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
    }
    val outMime = MediaFormat.reencodeMime(outFormat)
    val ext = MediaFormat.extensionForMime(outMime)

    val baos = ByteArrayOutputStream()
    bitmap.compress(compressFormat, quality, baos)
    val bytes = baos.toByteArray()

    val outFile = File.createTempFile("media_picker_", ".$ext", reactContext.cacheDir)
    FileOutputStream(outFile).use { it.write(bytes) }

    val width = bitmap.width
    val height = bitmap.height
    bitmap.recycle()

    return Arguments.createMap().apply {
      putString("uri", Uri.fromFile(outFile).toString())
      putString("type", outMime)
      putString("fileName", outFile.name)
      putDouble("fileSize", bytes.size.toDouble())
      putInt("width", width)
      putInt("height", height)
      if (includeBase64) {
        putString("base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
      }
    }
  }

  @Suppress("DEPRECATION")
  private fun webpCompressFormat(): Bitmap.CompressFormat =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
      Bitmap.CompressFormat.WEBP_LOSSY
    } else {
      Bitmap.CompressFormat.WEBP
    }

  private fun isAnimatedWebpUri(uri: Uri): Boolean =
    try {
      reactContext.contentResolver.openInputStream(uri).use { stream ->
        stream ?: return false
        val header = ByteArray(21)
        var total = 0
        while (total < header.size) {
          val n = stream.read(header, total, header.size - total)
          if (n == -1) break
          total += n
        }
        total >= 21 && MediaFormat.isAnimatedWebp(header)
      }
    } catch (e: Exception) {
      false
    }

  private fun isExifAxisSwapped(uri: Uri): Boolean {
    val orientation = reactContext.contentResolver.openInputStream(uri).use { stream ->
      stream ?: return false
      ExifInterface(stream).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
      )
    }
    return orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
      orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
      orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
      orientation == ExifInterface.ORIENTATION_TRANSVERSE
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
    private const val CAMERA_REQUEST_CODE = 48212
    private const val CAMERA_PERMISSION_REQUEST_CODE = 48213
  }
}
