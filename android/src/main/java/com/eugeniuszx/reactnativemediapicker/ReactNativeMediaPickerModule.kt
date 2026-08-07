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
    // Sweep leftovers from previous runs. Anything recent is left alone in case
    // JS is still holding a URI from a reload-surviving pick.
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

    // Throwable, not Exception: this guard owns the slot, and anything escaping
    // it strands every later pick behind "Already waiting for a pick." for the
    // process lifetime. MediaStore.getPickImagesMaxLimit() inside
    // intents.imageLibrary is already lint-flagged NewApi, and the NoSuchMethodError
    // it would raise on a platform without it is an Error, not an Exception.
    try {
      activity.startActivityForResult(intents.imageLibrary(parsed.selectionLimit), REQUEST_CODE)
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

    // Guarded because anything escaping would leave the request in the slot
    // forever, and every later pick would answer "Already waiting for a pick."
    // Throwable rather than Exception: an Error here is just as stranding, and
    // this guard is the only thing standing between one and a bricked module.
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
    // Resolve first: the sweep is fire-and-forget, and a cancelled scope must
    // not be able to strand the promise. Matches the iOS coordinator, and the
    // API reports no result, so a caller cannot observe the difference.
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

    // Null only when invalidate() already answered this request; nothing to do.
    val request = pending.peek() ?: return

    // One guard around the whole body: anything escaping here would strand the
    // request in the slot and brick every later pick. FileProvider.getUriForFile
    // in particular throws IllegalArgumentException if a host app has overridden
    // the provider's declared paths. Throwable rather than Exception, because an
    // Error strands the slot exactly as thoroughly as an exception does.
    try {
      val photoFile = tempFiles.createFile("jpg").apply { createNewFile() }
      // From here on `fail` owns deleting the capture file.
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
    // peek(), not take(): the slot stays claimed until the promise is actually
    // settled, so a concurrent invalidate() always finds a request to answer and
    // a second pick cannot start while this batch is still decoding (which would
    // put 8 items in flight rather than the mandated 4). Matches iOS, which holds
    // its session until the result is delivered.
    val request = pending.peek() ?: return
    // A request without library options cannot be processed, but must still be
    // answered rather than left hanging.
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
        // Bounded fan-out: each in-flight item holds a decoded bitmap plus the
        // encoded output, so this is the knob that caps peak memory.
        val gate = Semaphore(MAX_CONCURRENT_ITEMS)
        val assets = uris.map { uri ->
          async {
            gate.withPermit {
              try {
                processor.process(
                  uri,
                  options.maxWidth,
                  options.maxHeight,
                  options.quality,
                  options.includeBase64,
                )
              } catch (e: CancellationException) {
                throw e
              } catch (e: Exception) {
                // Drop the failed item and keep the rest, matching iOS.
                Log.w(NAME, "failed to process $uri", e)
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
      }
    }
  }

  private fun handleCameraResult(resultCode: Int) {
    // peek(), not take() — see handleLibraryResult. `cameraFile` deliberately
    // stays set: the coroutine's `finally` and invalidate() may both delete it,
    // and File.delete() on a missing file just returns false.
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
      } finally {
        // The raw capture is superseded by the processed output. Runs on
        // cancellation too, so invalidate() cannot orphan it.
        file.delete()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {}

  override fun invalidate() {
    // settle() is idempotent, so an in-flight coroutine finishing after this
    // point cannot resolve the promise a second time.
    pending.take()?.let { request ->
      // A capture still in the camera app's hands is orphaned from here on.
      request.cameraFile?.delete()
      request.cameraFile = null
      request.settle(
        ResponseFactory.failure(PickerError.OTHERS, "Module destroyed before result.")
      )
    }
    moduleScope.cancel()
    super.invalidate()
  }

  /**
   * Settles the request and only then releases the slot, so a concurrent
   * invalidate() always finds a request to answer. settle() is idempotent, so
   * whichever side wins, the promise is answered exactly once.
   *
   * The release is identity-checked. Both result handlers use peek(), so a
   * duplicate REQUEST_CODE delivery can run this twice for the same request:
   * the second settle() is a no-op, but an unconditional take() would clear
   * whatever is in the slot *now* — possibly a freshly begun second pick, which
   * would then never be settled at all.
   */
  private fun settleAndRelease(request: PendingRequest, value: WritableMap) {
    request.settle(value)
    pending.release(request)
  }

  /**
   * Answers the in-flight request with a failure, cleaning up any capture file
   * it still owns. A no-op when something else already took the request. Only
   * reached before a result arrives, so no coroutine can be holding the slot.
   */
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
