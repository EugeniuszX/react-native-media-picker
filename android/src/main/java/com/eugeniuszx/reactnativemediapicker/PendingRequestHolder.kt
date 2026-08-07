package com.eugeniuszx.reactnativemediapicker

import java.util.concurrent.atomic.AtomicReference

internal class PendingRequestHolder<T : Any> {
  private val current = AtomicReference<T?>(null)

  fun begin(value: T): Boolean = current.compareAndSet(null, value)

  fun take(): T? = current.getAndSet(null)

  fun release(expected: T): Boolean = current.compareAndSet(expected, null)

  fun peek(): T? = current.get()
}
