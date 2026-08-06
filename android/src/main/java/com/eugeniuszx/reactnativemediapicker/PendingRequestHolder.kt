package com.eugeniuszx.reactnativemediapicker

import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the at-most-one in-flight request. [begin] is a compare-and-set, which
 * is what makes "only one pick at a time" true — the previous `if (field != null)`
 * check-then-act let two simultaneous JS calls both through.
 */
internal class PendingRequestHolder<T : Any> {
  private val current = AtomicReference<T?>(null)

  /** Claims the slot. Returns false when a request is already in flight. */
  fun begin(value: T): Boolean = current.compareAndSet(null, value)

  /** Hands the request to the first caller and clears the slot. */
  fun take(): T? = current.getAndSet(null)

  fun peek(): T? = current.get()
}
