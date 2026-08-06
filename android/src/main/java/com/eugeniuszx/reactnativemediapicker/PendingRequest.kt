package com.eugeniuszx.reactnativemediapicker

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import java.io.File

/**
 * The one in-flight pick. Settlement goes through [OnceGate], so a late
 * coroutine cannot resolve a promise that `invalidate` already answered.
 */
internal class PendingRequest(
  private val promise: Promise,
  val libraryOptions: LibraryOptions? = null,
  val cameraOptions: CameraOptions? = null,
) {
  private val gate = OnceGate()

  /** Temp file the camera app writes into. Only set on the camera path. */
  @Volatile
  var cameraFile: File? = null

  /** Resolves the promise if nothing else has. Returns true when it did. */
  fun settle(value: WritableMap): Boolean {
    if (!gate.tryEnter()) return false
    promise.resolve(value)
    return true
  }
}
