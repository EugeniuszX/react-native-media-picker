package com.eugeniuszx.reactnativemediapicker

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log

/**
 * Reads the display name a content provider reports for a picked item. Providers are free to
 * report nothing, so a missing name is an ordinary outcome rather than a failure — see
 * [AssetFileName] for what is reported instead.
 */
internal object SourceDisplayName {
  fun of(resolver: ContentResolver, uri: Uri): String? = try {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (!cursor.moveToFirst()) return null
      val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (index < 0) null else cursor.getString(index)
    }
  } catch (e: Exception) {
    Log.w(ReactNativeMediaPickerModule.NAME, "failed to read the display name of $uri", e)
    null
  }
}
