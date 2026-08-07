package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ConcurrencyPrimitivesTest {
  private val pool = Executors.newFixedThreadPool(WORKERS)

  private val inFlight = AtomicInteger(0)

  private val peakConcurrency = AtomicInteger(0)

  private val overlappedTrials = AtomicInteger(0)
  private val trialsRun = AtomicInteger(0)

  @Before
  fun resetContentionMetrics() {
    inFlight.set(0)
    peakConcurrency.set(0)
    overlappedTrials.set(0)
    trialsRun.set(0)
  }

  @After
  fun tearDown() {
    pool.shutdownNow()
  }

  @Test
  fun `once gate admits a single caller`() {
    val gate = OnceGate()
    assertTrue(gate.tryEnter())
    assertFalse(gate.tryEnter())
    assertFalse(gate.tryEnter())
  }

  @Test
  fun `once gate admits a single caller under contention`() {
    repeat(TRIALS) { trial ->
      val gate = OnceGate()
      val admitted = AtomicInteger(0)
      runConcurrently { if (gate.tryEnter()) admitted.incrementAndGet() }
      assertEquals("trial $trial", 1, admitted.get())
    }
    assertWorkersOverlapped()
  }

  @Test
  fun `holder rejects a second begin while one is in flight`() {
    val holder = PendingRequestHolder<String>()
    assertTrue(holder.begin("first"))
    assertFalse(holder.begin("second"))
    assertEquals("first", holder.peek())
    assertEquals("first", holder.take())
    assertNull(holder.take())
  }

  @Test
  fun `holder is reusable after take`() {
    val holder = PendingRequestHolder<String>()
    assertTrue(holder.begin("first"))
    holder.take()
    assertTrue(holder.begin("second"))
    assertEquals("second", holder.take())
  }

  @Test
  fun `release clears the slot when the request is still the current one`() {
    val holder = PendingRequestHolder<String>()
    holder.begin("first")

    assertTrue(holder.release("first"))
    assertNull(holder.peek())
    assertFalse(holder.release("first"))
  }

  @Test
  fun `release leaves a newer request in the slot`() {
    val holder = PendingRequestHolder<String>()
    val stale = String(charArrayOf('r', 'e', 'q'))
    val fresh = String(charArrayOf('r', 'e', 'q'))
    assertEquals(stale, fresh)
    assertNotSame(stale, fresh)

    holder.begin(stale)
    holder.release(stale)
    assertTrue(holder.begin(fresh))

    assertFalse("stale release must not evict the newer request", holder.release(stale))
    assertSame(fresh, holder.peek())
  }

  @Test
  fun `only one of many concurrent begins succeeds`() {
    repeat(TRIALS) { trial ->
      val holder = PendingRequestHolder<Int>()
      val winners = AtomicInteger(0)
      runConcurrently { index -> if (holder.begin(index)) winners.incrementAndGet() }
      assertEquals("trial $trial", 1, winners.get())
    }
    assertWorkersOverlapped()
  }

  @Test
  fun `only one of many concurrent takes receives the value`() {
    repeat(TRIALS) { trial ->
      val holder = PendingRequestHolder<Int>()
      holder.begin(7)
      val received = AtomicInteger(0)
      runConcurrently { if (holder.take() != null) received.incrementAndGet() }
      assertEquals("trial $trial", 1, received.get())
    }
    assertWorkersOverlapped()
  }

  private fun runConcurrently(block: (Int) -> Unit) {
    val ready = CountDownLatch(WORKERS)
    val start = CountDownLatch(1)
    val done = CountDownLatch(WORKERS)
    val fireAt = AtomicLong(0)
    val trialPeak = AtomicInteger(0)
    repeat(WORKERS) { index ->
      pool.execute {
        ready.countDown()
        start.await()
        val deadline = fireAt.get()
        while (deadline - System.nanoTime() > 0) {
        }
        val depth = inFlight.incrementAndGet()
        trialPeak.updateAndGet { maxOf(it, depth) }
        peakConcurrency.updateAndGet { maxOf(it, depth) }
        try {
          block(index)
        } finally {
          inFlight.decrementAndGet()
          done.countDown()
        }
      }
    }
    ready.await()
    fireAt.set(System.nanoTime() + ALIGNMENT_NANOS)
    start.countDown()
    done.await()
    trialsRun.incrementAndGet()
    if (trialPeak.get() >= 2) overlappedTrials.incrementAndGet()
  }

  private fun assertWorkersOverlapped() {
    assertTrue(
      "workers never overlapped across ${trialsRun.get()} trials (peak concurrency " +
        "${peakConcurrency.get()}, overlapping trials ${overlappedTrials.get()}); this " +
        "harness cannot detect a race on this machine — check ALIGNMENT_NANOS",
      peakConcurrency.get() >= 2
    )
  }

  companion object {
    private const val WORKERS = 8

    private const val TRIALS = 1000

    private const val ALIGNMENT_NANOS = 200_000L
  }
}
