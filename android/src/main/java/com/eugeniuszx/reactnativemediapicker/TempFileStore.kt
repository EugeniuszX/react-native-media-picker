package com.eugeniuszx.reactnativemediapicker

import java.io.File
import java.util.UUID

internal class TempFileStore(private val root: File) {
  fun createFile(extension: String): File {
    root.mkdirs()
    return File(root, "media_picker_${UUID.randomUUID()}.$extension")
  }

  fun removeAll(): Int = remove { true }

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

    const val AUTO_SWEEP_AGE_MILLIS = 24L * 60 * 60 * 1000
  }
}
