package com.eugeniuszx.reactnativemediapicker

import java.util.concurrent.atomic.AtomicBoolean

internal class OnceGate {
  private val used = AtomicBoolean(false)

  fun tryEnter(): Boolean = used.compareAndSet(false, true)
}
