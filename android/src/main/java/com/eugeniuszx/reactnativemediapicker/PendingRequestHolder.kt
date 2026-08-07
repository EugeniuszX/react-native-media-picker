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

  /**
   * Clears the slot only if [expected] is still the request in it. Returns true
   * when it was cleared.
   *
   * The identity check is the point: a duplicate activity result can spawn a
   * second coroutine for a request that is already settled, and an unconditional
   * [take] there would evict a *newer* request that had since claimed the slot,
   * leaving it to be discarded unsettled.
   */
  fun release(expected: T): Boolean = current.compareAndSet(expected, null)

  fun peek(): T? = current.get()
}
