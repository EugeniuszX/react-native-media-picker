package com.eugeniuszx.reactnativemediapicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ReactNativeMediaPickerModule(private val reactContext: ReactApplicationContext) :
  NativeReactNativeMediaPickerSpec(reactContext),
  ActivityEventListener {
  private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val pending = PendingRequestHolder<PendingRequest>()
  private val intents = PickerIntentFactory(reactContext)
  private val permissions = CameraPermissionGate(reactContext)
  private val tempFiles =
    TempFileStore(File(reactContext.cacheDir, TempFileStore.DIRECTORY_NAME))
  private val processor = ImageProcessor(reactContext.contentResolver, tempFiles)
  private val videoProcessor = VideoProcessor(reactContext.contentResolver, tempFiles)

  init {
    reactContext.addActivityEventListener(this)
    moduleScope.launch {
      tempFiles.removeFilesOlderThan(
        TempFileStore.AUTO_SWEEP_AGE_MILLIS,
        System.currentTimeMillis(),
      )
    }
  }

  override fun getName() = NAME

  override fun launchImageLibrary(options: ReadableMap, promise: Promise) {
    val activity = reactContext.currentActivity
    if (activity == null) {
      promise.resolve(ResponseFactory.failure(PickerError.OTHERS, "Activity is null"))
      return
    }

    val parsed = try {
      LibraryOptions.from(options)
    } catch (e: Exception) {
      promise.resolve(
        ResponseFactory.failure(PickerError.OTHERS, e.message ?: "invalid options")
      )
      return
    }

    val request = PendingRequest(promise, libraryOptions = parsed)
    if (!pending.begin(request)) {
      promise.resolve(ResponseFactory.failure(PickerError.BUSY, "Already waiting for a pick."))
      return
    }

    try {
      activity.startActivityForResult(
        intents.mediaLibrary(parsed.selectionLimit, parsed.mediaType),
        REQUEST_CODE,
      )
    } catch (e: Throwable) {
      pending.take()
      request.settle(ResponseFactory.failure(PickerError.OTHERS, e.message ?: "launch error"))
    }
  }

  override fun launchCamera(options: ReadableMap, promise: Promise) {
    val activity = reactContext.currentActivity
    if (activity == null) {
      promise.resolve(ResponseFactory.failure(PickerError.OTHERS, "Activity is null"))
      return
    }

    val parsed = try {
      CameraOptions.from(options)
    } catch (e: Exception) {
      promise.resolve(
        ResponseFactory.failure(PickerError.OTHERS, e.message ?: "invalid options")
      )
      return
    }

    val request = PendingRequest(promise, cameraOptions = parsed)
    if (!pending.begin(request)) {
      promise.resolve(ResponseFactory.failure(PickerError.BUSY, "Already waiting for a pick."))
      return
    }

    try {
      permissions.ensure(
        activity,
        onGranted = { launchCameraIntent(parsed) },
        onDenied = { error, message -> fail(error, message) },
      )
    } catch (e: Throwable) {
      fail(PickerError.OTHERS, e.message ?: "failed to request camera permission")
    }
  }

  override fun getCameraPermissionStatus(promise: Promise) {
    promise.resolve(permissions.status(reactContext.currentActivity).value)
  }

  override fun requestCameraPermission(promise: Promise) {
    val settled = AtomicBoolean(false)
    try {
      permissions.request(reactContext.currentActivity) { status ->
        if (settled.compareAndSet(false, true)) promise.resolve(status.value)
      }
    } catch (e: Throwable) {
      Log.w(NAME, "failed to request the camera permission", e)
      if (settled.compareAndSet(false, true)) {
        promise.resolve(permissions.status(reactContext.currentActivity).value)
      }
    }
  }

  override fun cleanTempFiles(promise: Promise) {
    resolveWithRemovedCount(promise, "failed to clean temp files") {
      tempFiles.removeAll()
    }
  }

  override fun releaseAssets(uris: ReadableArray, promise: Promise) {
    val names = buildList {
      for (index in 0 until uris.size()) {
        if (uris.getType(index) == ReadableType.String) {
          uris.getString(index)?.let { add(it) }
        }
      }
    }

    if (names.isEmpty()) {
      promise.resolve(0)
      return
    }

    resolveWithRemovedCount(promise, "failed to release temp files") {
      tempFiles.remove(names)
    }
  }

  /**
   * Resolves [promise] with the number of files [remove] deleted, once the deletion is done.
   * A cancelled module scope still settles the promise — including when the scope was already
   * cancelled before the job could start — so a caller never awaits forever.
   */
  private fun resolveWithRemovedCount(promise: Promise, label: String, remove: () -> Int) {
    val settled = AtomicBoolean(false)
    fun settle(count: Int) {
      if (settled.compareAndSet(false, true)) promise.resolve(count)
    }

    val job = moduleScope.launch {
      try {
        settle(remove())
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(NAME, label, e)
        settle(0)
      }
    }
    job.invokeOnCompletion { cause ->
      if (cause != null) settle(0)
    }
  }

  private fun launchCameraIntent(options: CameraOptions) {
    val activity = reactContext.currentActivity
    if (activity == null) {
      fail(PickerError.OTHERS, "Activity is null")
      return
    }

    val request = pending.peek() ?: return

    try {
      val isVideo = options.mediaType == CameraMediaType.VIDEO
      val captureFile = tempFiles.createFile(if (isVideo) "mp4" else "jpg")
        .apply { createNewFile() }
      request.cameraFile = captureFile

      val authority = "${reactContext.packageName}.rnmediapicker.fileprovider"
      val outputUri = FileProvider.getUriForFile(reactContext, authority, captureFile)
      val intent = if (isVideo) {
        intents.videoCapture(outputUri, options.facing, options.maxDuration, options.videoQuality)
      } else {
        intents.imageCapture(outputUri, options.facing)
      }

      if (!intents.canBeHandled(intent)) {
        fail(PickerError.CAMERA_UNAVAILABLE, "No camera app available")
        return
      }
      activity.startActivityForResult(intent, CAMERA_REQUEST_CODE)
    } catch (e: Throwable) {
      fail(PickerError.OTHERS, e.message ?: "failed to launch the camera")
    }
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ) {
    when (requestCode) {
      CAMERA_REQUEST_CODE -> handleCameraResult(resultCode, data)
      REQUEST_CODE -> handleLibraryResult(resultCode, data)
    }
  }

  private fun handleLibraryResult(resultCode: Int, data: Intent?) {
    val request = pending.peek() ?: return
    val options = request.libraryOptions ?: run {
      settleAndRelease(
        request,
        ResponseFactory.failure(PickerError.OTHERS, "Mismatched picker result"),
      )
      return
    }

    if (resultCode != Activity.RESULT_OK || data == null) {
      settleAndRelease(request, ResponseFactory.cancelled())
      return
    }

    val collected = intents.collectUris(data)
    val uris =
      if (options.selectionLimit > 1) collected.take(options.selectionLimit) else collected
    if (uris.isEmpty()) {
      settleAndRelease(request, ResponseFactory.cancelled())
      return
    }

    moduleScope.launch {
      try {
        val gate = Semaphore(MAX_CONCURRENT_ITEMS)
        val assets = uris.map { uri ->
          async {
            gate.withPermit {
              try {
                val suggestedName = SourceDisplayName.of(reactContext.contentResolver, uri)
                if (options.mediaType != RequestedMediaType.PHOTO && isVideoContent(uri)) {
                  videoProcessor.process(uri, options.includeThumbnail, suggestedName)
                } else {
                  processor.process(
                    uri = uri,
                    format = options.format,
                    maxWidth = options.maxWidth,
                    maxHeight = options.maxHeight,
                    quality = options.quality,
                    includeBase64 = options.includeBase64,
                    stripMetadata = options.stripMetadata,
                    includeExif = options.includeExif,
                    suggestedName = suggestedName,
                  )
                }
              } catch (e: CancellationException) {
                throw e
              } catch (e: Exception) {
                Log.w(NAME, "failed to process $uri", e)
                null
              } catch (e: OutOfMemoryError) {
                Log.w(NAME, "out of memory while processing $uri", e)
                null
              }
            }
          }
        }.awaitAll().filterNotNull()

        if (assets.isEmpty()) {
          settleAndRelease(
            request,
            ResponseFactory.failure(PickerError.OTHERS, "Failed to load the selected image(s)."),
          )
        } else {
          settleAndRelease(request, ResponseFactory.success(assets))
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        settleAndRelease(
          request,
          ResponseFactory.failure(PickerError.OTHERS, e.message ?: "processing error"),
        )
      } catch (e: OutOfMemoryError) {
        settleAndRelease(
          request,
          ResponseFactory.failure(PickerError.OTHERS, "Out of memory while processing images"),
        )
      }
    }
  }

  private fun isVideoContent(uri: Uri): Boolean {
    val mime = reactContext.contentResolver.getType(uri)
    if (MediaFormat.isVideoMime(mime)) return true
    if (mime?.lowercase()?.startsWith("image/") == true) return false

    return try {
      reactContext.contentResolver.openInputStream(uri)?.use { input ->
        val header = ByteArray(HEADER_SNIFF_BYTES)
        var read = 0
        while (read < HEADER_SNIFF_BYTES) {
          val count = input.read(header, read, HEADER_SNIFF_BYTES - read)
          if (count <= 0) break
          read += count
        }
        MediaFormat.isVideoHeader(header.copyOf(read))
      } ?: false
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(NAME, "failed to sniff the header of $uri", e)
      false
    }
  }

  private fun handleCameraResult(resultCode: Int, data: Intent?) {
    val request = pending.peek() ?: return
    val file = request.cameraFile

    val options = request.cameraOptions ?: run {
      file?.delete()
      settleAndRelease(
        request,
        ResponseFactory.failure(PickerError.OTHERS, "Mismatched picker result"),
      )
      return
    }

    if (options.mediaType == CameraMediaType.VIDEO) {
      // Some camera apps ignore EXTRA_OUTPUT and hand back a content uri instead.
      val source = when {
        file != null && file.exists() && file.length() > 0L -> Uri.fromFile(file)
        data?.data != null -> data.data
        else -> null
      }
      if (resultCode != Activity.RESULT_OK || source == null) {
        file?.delete()
        settleAndRelease(request, ResponseFactory.cancelled())
        return
      }
      moduleScope.launch {
        try {
          val asset = videoProcessor.process(source, options.includeThumbnail)
          settleAndRelease(request, ResponseFactory.success(listOf(asset)))
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          settleAndRelease(
            request,
            ResponseFactory.failure(PickerError.OTHERS, e.message ?: "processing error"),
          )
        } finally {
          file?.delete()
        }
      }
      return
    }

    if (resultCode != Activity.RESULT_OK || file == null || !file.exists() ||
      file.length() == 0L
    ) {
      file?.delete()
      settleAndRelease(request, ResponseFactory.cancelled())
      return
    }

    moduleScope.launch {
      try {
        val asset = processor.process(
          uri = Uri.fromFile(file),
          format = options.format,
          maxWidth = options.maxWidth,
          maxHeight = options.maxHeight,
          quality = options.quality,
          includeBase64 = options.includeBase64,
          stripMetadata = options.stripMetadata,
          includeExif = options.includeExif,
        )
        settleAndRelease(request, ResponseFactory.success(listOf(asset)))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        settleAndRelease(
          request,
          ResponseFactory.failure(PickerError.OTHERS, e.message ?: "processing error"),
        )
      } catch (e: OutOfMemoryError) {
        settleAndRelease(
          request,
          ResponseFactory.failure(PickerError.OTHERS, "Out of memory while processing images"),
        )
      } finally {
        file.delete()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {}

  override fun invalidate() {
    pending.take()?.let { request ->
      request.cameraFile?.delete()
      request.cameraFile = null
      request.settle(
        ResponseFactory.failure(PickerError.OTHERS, "Module destroyed before result.")
      )
    }
    moduleScope.cancel()
    super.invalidate()
  }

  private fun settleAndRelease(request: PendingRequest, value: WritableMap) {
    request.settle(value)
    pending.release(request)
  }

  private fun fail(error: PickerError, message: String) {
    pending.take()?.let { request ->
      request.cameraFile?.delete()
      request.cameraFile = null
      request.settle(ResponseFactory.failure(error, message))
    }
  }

  companion object {
    const val NAME = NativeReactNativeMediaPickerSpec.NAME
    private const val REQUEST_CODE = 48211
    private const val CAMERA_REQUEST_CODE = 48212
    private const val MAX_CONCURRENT_ITEMS = 4
    private const val HEADER_SNIFF_BYTES = 16
  }
}
