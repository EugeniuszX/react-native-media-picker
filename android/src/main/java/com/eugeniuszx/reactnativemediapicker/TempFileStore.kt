package com.eugeniuszx.reactnativemediapicker

import java.io.File
import java.util.UUID

/**
 * Owns the picker's on-disk scratch space. Everything lives in one dedicated
 * subdirectory of the app cache so it can be swept without touching anything
 * else. Framework-free so it is JVM-unit-testable.
 */
internal class TempFileStore(private val root: File) {
  fun createFile(extension: String): File {
    root.mkdirs()
    return File(root, "media_picker_${UUID.randomUUID()}.$extension")
  }

  /** Deletes every file the picker has produced. Returns how many were removed. */
  fun removeAll(): Int = remove { true }

  /**
   * Deletes files last modified more than [ageMillis] before [nowMillis].
   *
   * A file whose timestamp cannot be read is kept, matching
   * `ios/Core/TempFileStore.swift`. This needs an explicit guard on Android:
   * [File.lastModified] returns 0 on an I/O error, and a bare subtraction would
   * read that as "epoch, therefore ancient" and delete the file instead.
   */
  fun removeFilesOlderThan(ageMillis: Long, nowMillis: Long): Int =
    remove { file ->
      val modified = file.lastModified()
      modified > 0L && nowMillis - modified > ageMillis
    }

  private fun remove(shouldRemove: (File) -> Boolean): Int {
    val entries = root.listFiles() ?: return 0
    return entries.count { file -> shouldRemove(file) && file.delete() }
  }

  companion object {
    const val DIRECTORY_NAME = "rn-media-picker"

    /**
     * Files older than this are removed when the module initializes. Long enough
     * that a URI handed to JS stays valid for any realistic session.
     */
    const val AUTO_SWEEP_AGE_MILLIS = 24L * 60 * 60 * 1000
  }
}
