package com.eugeniuszx.reactnativemediapicker

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import java.io.File

internal class PendingRequest(
  private val promise: Promise,
  val libraryOptions: LibraryOptions? = null,
  val cameraOptions: CameraOptions? = null,
) {
  private val gate = OnceGate()

  @Volatile
  var cameraFile: File? = null

  fun settle(value: WritableMap): Boolean {
    if (!gate.tryEnter()) return false
    promise.resolve(value)
    return true
  }
}
