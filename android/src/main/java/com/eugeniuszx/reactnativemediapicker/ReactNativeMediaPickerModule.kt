package com.eugeniuszx.reactnativemediapicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
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
      promise.resolve(ResponseFactory.failure(PickerError.OTHERS, "Already waiting for a pick."))
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
      promise.resolve(ResponseFactory.failure(PickerError.OTHERS, "Already waiting for a pick."))
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

  override fun cleanTempFiles(promise: Promise) {
    promise.resolve(null)
    moduleScope.launch {
      try {
        tempFiles.removeAll()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(NAME, "failed to clean temp files", e)
      }
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
      val photoFile = tempFiles.createFile("jpg").apply { createNewFile() }
      request.cameraFile = photoFile

      val authority = "${reactContext.packageName}.rnmediapicker.fileprovider"
      val outputUri = FileProvider.getUriForFile(reactContext, authority, photoFile)
      val intent = intents.imageCapture(outputUri, options.facing)

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
      CAMERA_REQUEST_CODE -> handleCameraResult(resultCode)
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
                processor.process(
                  uri,
                  options.format,
                  options.maxWidth,
                  options.maxHeight,
                  options.quality,
                  options.includeBase64,
                )
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

  private fun handleCameraResult(resultCode: Int) {
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
          Uri.fromFile(file),
          options.format,
          options.maxWidth,
          options.maxHeight,
          options.quality,
          options.includeBase64,
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
  }
}
