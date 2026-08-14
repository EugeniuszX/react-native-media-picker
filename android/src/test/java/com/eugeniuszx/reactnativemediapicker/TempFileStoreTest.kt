package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
  fun `remove by uri deletes only the named files`() {
    val store = store()
    val released = store.createFile("mp4").apply { writeBytes(byteArrayOf(1)) }
    val kept = store.createFile("jpg").apply { writeBytes(byteArrayOf(1)) }

    assertEquals(1, store.remove(listOf(released.toURI().toString())))
    assertFalse(released.exists())
    assertTrue(kept.exists())
  }

  @Test
  fun `remove by uri ignores files outside the picker directory`() {
    val cache = folder.newFolder("cache")
    val store = TempFileStore(cache.resolve(TempFileStore.DIRECTORY_NAME))
    store.createFile("jpg").writeBytes(byteArrayOf(1))
    val outsider = cache.resolve("not-ours.jpg").apply { writeBytes(byteArrayOf(1)) }

    assertEquals(0, store.remove(listOf(outsider.toURI().toString())))
    assertTrue(outsider.exists())
  }

  @Test
  fun `remove by uri ignores traversal and non-file uris`() {
    val cache = folder.newFolder("cache")
    val root = cache.resolve(TempFileStore.DIRECTORY_NAME)
    val store = TempFileStore(root)
    val kept = store.createFile("jpg").apply { writeBytes(byteArrayOf(1)) }
    val outsider = cache.resolve("not-ours.jpg").apply { writeBytes(byteArrayOf(1)) }

    val removed = store.remove(
      listOf(
        "file://${root.path}/../not-ours.jpg",
        "content://media/external/images/media/1",
        "https://example.com/a.jpg",
        "",
      ),
    )

    assertEquals(0, removed)
    assertTrue(outsider.exists())
    assertTrue(kept.exists())
  }

  @Test
  fun `remove by uri with no usable entries is a no-op`() {
    assertEquals(0, store().remove(emptyList()))
  }

  @Test
  fun `file name for uri decodes percent escapes and rejects other schemes`() {
    assertEquals(
      "media picker.jpg",
      TempFileStore.fileNameForUri("file:///tmp/media%20picker.jpg"),
    )
    assertNull(TempFileStore.fileNameForUri("file:///"))
    assertNull(TempFileStore.fileNameForUri("content://media/external/1"))
    assertNull(TempFileStore.fileNameForUri("media_picker_A.jpg"))
    assertNull(TempFileStore.fileNameForUri("not a uri at all"))
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

  @Test
  fun `age based sweep keeps files whose timestamp cannot be read`() {
    val store = store()
    val file = store.createFile("jpg").apply { writeBytes(byteArrayOf(1)) }
    assertTrue(file.setLastModified(0L))

    assertEquals(0, store.removeFilesOlderThan(TempFileStore.AUTO_SWEEP_AGE_MILLIS, 1_000_000_000_000L))
    assertTrue(file.exists())
  }
}
