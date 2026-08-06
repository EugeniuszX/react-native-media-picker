package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

  /** Workers currently inside the raced block. */
  private val inFlight = AtomicInteger(0)

  /** Highest [inFlight] depth seen across every trial of the current test. */
  private val peakConcurrency = AtomicInteger(0)

  /** Trials whose own peak depth reached 2 — i.e. that could detect anything. */
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

  /** The double-resolve defect: many threads racing to settle one promise. */
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

  /** The check-then-act defect: two JS calls arriving at the same moment. */
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

  /**
   * Parks [WORKERS] threads on a common barrier, releases them together, and
   * waits for all of them.
   *
   * The pool holds exactly [WORKERS] threads and each round submits exactly
   * [WORKERS] tasks, so every task really is parked before any is released —
   * submitting more tasks than threads would let the surplus run in later,
   * uncontended waves. Mirrors `runConcurrently` in
   * `ios/CoreTests/PickerSessionTests.swift`.
   *
   * The latch alone is not a tight enough start gate on the JVM. `CountDownLatch`
   * releases waiters through AQS, which unparks them one at a time, so workers
   * leave `await` microseconds apart while the check-then-act window these tests
   * probe is nanoseconds wide. Measured against the check-then-act mutants: the
   * latch-only version false-greened the whole suite about 1 run in 8. So after
   * the latch every worker spins to one shared [System.nanoTime] deadline — by
   * the time it passes all of them are already running on a CPU and they cross
   * the line together.
   */
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
          // Busy-wait: parking here would defeat the point of the alignment.
        }
        // Measured after the spin and immediately around the raced block:
        // incrementing before the spin would read 8 every time and measure
        // nothing but "all workers were submitted".
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
    // Written before the release, so the latch's happens-before publishes it.
    fireAt.set(System.nanoTime() + ALIGNMENT_NANOS)
    start.countDown()
    done.await()
    trialsRun.incrementAndGet()
    if (trialPeak.get() >= 2) overlappedTrials.incrementAndGet()
  }

  /**
   * The contention tests can only detect a race if the workers actually overlap.
   * Alignment quality depends on the machine, so assert that overlap was
   * observed rather than assuming [ALIGNMENT_NANOS] is adequate everywhere — a
   * silently unaligned harness passes at full cost while detecting nothing.
   *
   * The bar is 2, not [WORKERS]: the raced block is nanoseconds long, so even a
   * perfect release need not show all 8 at once, and a strict threshold would be
   * flaky on a single-core runner. Two is the least that means anything. The
   * per-trial overlap rate is reported in the failure message because the peak
   * alone cannot distinguish "aligned on every trial" from "aligned once in
   * 1000" — that rate is the number to look at when tuning the constant.
   */
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

    /**
     * Detection is probabilistic. Measured against check-then-act mutants of
     * [OnceGate.tryEnter] and [PendingRequestHolder.begin]: 1000 trials with
     * spin-aligned starts caught both every run.
     */
    private const val TRIALS = 1000

    /**
     * Head start the workers spin through before racing. Must comfortably exceed
     * the time for all [WORKERS] threads to wake from the latch and get onto a
     * CPU; 200µs measured as ample and costs ~0.2s per 1000-trial test.
     */
    private const val ALIGNMENT_NANOS = 200_000L
  }
}
