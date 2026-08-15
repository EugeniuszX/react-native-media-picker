package com.eugeniuszx.reactnativemediapicker

import android.util.Log
import java.io.File

internal object XMPPacket {
  /**
   * XMP is stored as uncompressed XML text — a JPEG `APP1` segment, a PNG `iTXt` chunk, a HEIF
   * `mime` item — so a packet is literally present in the container's bytes and can be found
   * without a parser.
   *
   * Four markers rather than one: a packet can be written without the `x:xmpmeta` wrapper, so the
   * fixed packet id and the XMP namespace URI are checked too, and `XML:com.adobe.xmp` is the PNG
   * `iTXt` keyword — a keyword is always stored uncompressed even when its payload is deflated,
   * so it catches a compressed PNG packet the other three would miss.
   */
  private val MARKERS = listOf(
    "<x:xmpmeta",
    "W5M0MpCehiHzreSzNTczkc9d",
    "ns.adobe.com/xap",
    "XML:com.adobe.xmp",
  ).map { it.toByteArray(Charsets.US_ASCII) }

  private val OVERLAP = MARKERS.maxOf { it.size } - 1
  private const val CHUNK = 64 * 1024

  fun isPresent(bytes: ByteArray): Boolean = isPresent(bytes, bytes.size)

  /**
   * Streams the file in chunks that overlap by one marker's length, so a marker straddling a
   * chunk boundary is still found without materialising a 40 MB image in memory.
   *
   * A read failure reports `true`: the caller declines the lossless scrub and re-encodes instead,
   * which is the safe answer when we cannot prove the file is packet-free.
   */
  fun isPresent(file: File): Boolean = try {
    file.inputStream().buffered().use { stream ->
      val buffer = ByteArray(CHUNK + OVERLAP)
      var carried = 0
      var found = false
      while (!found) {
        val read = stream.read(buffer, carried, CHUNK)
        if (read <= 0) break
        val filled = carried + read
        if (isPresent(buffer, filled)) {
          found = true
        } else {
          carried = minOf(OVERLAP, filled)
          System.arraycopy(buffer, filled - carried, buffer, 0, carried)
        }
      }
      found
    }
  } catch (e: Exception) {
    Log.w(ReactNativeMediaPickerModule.NAME, "failed to scan ${file.name} for xmp", e)
    true
  }

  private fun isPresent(bytes: ByteArray, length: Int): Boolean =
    MARKERS.any { contains(bytes, length, it) }

  private fun contains(haystack: ByteArray, length: Int, needle: ByteArray): Boolean {
    if (needle.isEmpty() || needle.size > length) return false
    outer@ for (start in 0..length - needle.size) {
      for (index in needle.indices) {
        if (haystack[start + index] != needle[index]) continue@outer
      }
      return true
    }
    return false
  }
}
