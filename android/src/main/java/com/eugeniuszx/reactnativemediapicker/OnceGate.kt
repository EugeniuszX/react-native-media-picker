package com.eugeniuszx.reactnativemediapicker

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Admits exactly one caller, ever. Used to make promise settlement idempotent:
 * a coroutine that finishes after [ReactNativeMediaPickerModule.invalidate] has
 * already answered must not answer a second time.
 */
internal class OnceGate {
  private val used = AtomicBoolean(false)

  fun tryEnter(): Boolean = used.compareAndSet(false, true)
}
