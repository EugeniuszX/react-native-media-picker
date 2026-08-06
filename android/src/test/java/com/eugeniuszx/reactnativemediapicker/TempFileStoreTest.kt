package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TempFileStoreTest {
  @get:Rule
  val folder = TemporaryFolder()

  private fun store() = TempFileStore(folder.newFolder("cache").resolve(TempFileStore.DIRECTORY_NAME))

  @Test
  fun `creates uniquely named files with the requested extension`() {
    val store = store()
    val names = (0 until 50).map { store.createFile("jpg").name }
    assertEquals(50, names.toSet().size)
    assertTrue(names.all { it.startsWith("media_picker_") && it.endsWith(".jpg") })
  }

  @Test
  fun `removeAll deletes every file it produced`() {
    val store = store()
    repeat(3) { store.createFile("png").writeBytes(byteArrayOf(1)) }
    assertEquals(3, store.removeAll())
    assertEquals(0, store.removeAll())
  }

  /**
   * removeAll empties the directory but must not delete the directory itself —
   * one of the semantics bound to ios/Core/TempFileStore.swift. Task 9 sweeps
   * this directory at module init and then writes into it.
   */
  @Test
  fun `removeAll keeps the directory itself`() {
    val root = folder.newFolder("cache").resolve(TempFileStore.DIRECTORY_NAME)
    val store = TempFileStore(root)
    store.createFile("jpg").writeBytes(byteArrayOf(1))

    assertEquals(1, store.removeAll())
    assertTrue(root.isDirectory)
  }

  @Test
  fun `removeAll on a missing directory is a no-op`() {
    val store = TempFileStore(folder.root.resolve("does-not-exist"))
    assertEquals(0, store.removeAll())
  }

  @Test
  fun `age based sweep keeps recent files`() {
    val store = store()
    val old = store.createFile("jpg").apply { writeBytes(byteArrayOf(1)) }
    val recent = store.createFile("jpg").apply { writeBytes(byteArrayOf(1)) }

    val now = 1_000_000_000_000L
    old.setLastModified(now - 48 * 60 * 60 * 1000L)
    recent.setLastModified(now - 60 * 1000L)

    assertEquals(1, store.removeFilesOlderThan(TempFileStore.AUTO_SWEEP_AGE_MILLIS, now))
    assertFalse(old.exists())
    assertTrue(recent.exists())
  }

  /**
   * Mirrors the iOS store: an unreadable timestamp means "keep". File.lastModified
   * reports 0 for that case, which a bare subtraction would misread as ancient.
   */
  @Test
  fun `age based sweep keeps files whose timestamp cannot be read`() {
    val store = store()
    val file = store.createFile("jpg").apply { writeBytes(byteArrayOf(1)) }
    // Asserted: if the filesystem refuses the value, lastModified stays at ~now,
    // removeFilesOlderThan returns 0 for the wrong reason, and this parity test
    // becomes a permanent false green with no signal.
    assertTrue(file.setLastModified(0L))

    assertEquals(0, store.removeFilesOlderThan(TempFileStore.AUTO_SWEEP_AGE_MILLIS, 1_000_000_000_000L))
    assertTrue(file.exists())
  }
}
