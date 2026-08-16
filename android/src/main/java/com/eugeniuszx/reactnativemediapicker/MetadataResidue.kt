package com.eugeniuszx.reactnativemediapicker

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Detects metadata that lives outside the EXIF segment and therefore survives
 * [MetadataScrubber.scrub], which rewrites only that one segment and copies the rest of the
 * container verbatim. A source carrying any of it is declined so the caller re-encodes instead,
 * which drops every container-level channel at once.
 *
 * Broader than its iOS counterpart, `ios/Core/XMPPacket.swift`, which only has to look for XMP:
 * ImageIO nulls the IPTC dictionary and rebuilds the PNG dictionary from rendering keys alone, so
 * those two channels are already handled there by the writer.
 */
internal object MetadataResidue {
  /**
   * Literal ASCII that appears in the container when a packet or block is present.
   *
   * The first three are XMP: the usual `x:xmpmeta` wrapper, the fixed `xpacket` id (present when
   * the wrapper is not) and the XMP namespace URI. `XML:com.adobe.xmp` is the PNG `iTXt` keyword —
   * a keyword is stored uncompressed even when its payload is deflated. `Photoshop 3.0` is the
   * JPEG `APP13` Image Resource Block identifier, which is how IPTC-IIM creator, city,
   * sub-location, contact and copyright strings ride along; at thirteen bytes its false-positive
   * rate against compressed pixel data is negligible.
   */
  private val MARKERS = listOf(
    "<x:xmpmeta",
    "W5M0MpCehiHzreSzNTczkc9d",
    "ns.adobe.com/xap",
    "XML:com.adobe.xmp",
    "Photoshop 3.0",
  ).map { it.toByteArray(Charsets.US_ASCII) }

  private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
  )

  /** `tEXt`, `zTXt` and `iTXt` carry Author, Comment, Software and Creation Time credits. */
  private val PNG_TEXT_CHUNK_TYPES = setOf("tEXt", "zTXt", "iTXt")
  private const val PNG_CHUNK_IEND = "IEND"

  /** A PNG chunk is `length(4) + type(4) + data + crc(4)`. */
  private const val PNG_CHUNK_HEADER_LENGTH = 8
  private const val PNG_CHUNK_CRC_LENGTH = 4L

  private val OVERLAP = MARKERS.maxOf { it.size } - 1
  private const val CHUNK = 64 * 1024

  fun isPresent(bytes: ByteArray): Boolean = ByteArrayInputStream(bytes).use { scan(it) }

  /**
   * Streams the file rather than reading it whole, so a 40 MB image is never materialised.
   *
   * A read failure reports `true`: the caller declines the lossless scrub and re-encodes instead,
   * which is the safe answer when we cannot prove the file is clean.
   */
  fun isPresent(file: File): Boolean = try {
    file.inputStream().buffered().use { scan(it) }
  } catch (e: Exception) {
    Log.w(ReactNativeMediaPickerModule.NAME, "failed to scan ${file.name} for metadata", e)
    true
  }

  private fun scan(stream: InputStream): Boolean {
    val signature = ByteArray(PNG_SIGNATURE.size)
    val read = readFully(stream, signature)
    return if (read == signature.size && signature.contentEquals(PNG_SIGNATURE)) {
      pngCarriesTextChunk(stream)
    } else {
      containsMarker(stream, signature, read)
    }
  }

  /**
   * Walks the chunk table looking for a text chunk. Anything that does not parse — an implausible
   * length, a walk that runs off the end, no `IEND` — is reported as a hit: a PNG we cannot read
   * is a PNG we cannot vouch for.
   */
  private fun pngCarriesTextChunk(stream: InputStream): Boolean {
    val header = ByteArray(PNG_CHUNK_HEADER_LENGTH)
    while (readFully(stream, header) == PNG_CHUNK_HEADER_LENGTH) {
      val dataLength = readInt32(header)
      if (dataLength < 0) return true

      val type = String(header, 4, 4, Charsets.US_ASCII)
      if (type in PNG_TEXT_CHUNK_TYPES) return true
      if (type == PNG_CHUNK_IEND) return false

      val toSkip = dataLength.toLong() + PNG_CHUNK_CRC_LENGTH
      if (skipFully(stream, toSkip) != toSkip) return true
    }
    return true
  }

  /**
   * Scans in chunks that overlap by one marker's length, so a marker straddling a chunk boundary
   * is still found. [prefix] holds the bytes already consumed to test for the PNG signature.
   */
  private fun containsMarker(stream: InputStream, prefix: ByteArray, prefixLength: Int): Boolean {
    val buffer = ByteArray(CHUNK + OVERLAP)
    var carried = minOf(prefixLength, OVERLAP)
    System.arraycopy(prefix, 0, buffer, 0, carried)

    while (true) {
      val read = stream.read(buffer, carried, CHUNK)
      if (read <= 0) return false
      val filled = carried + read
      if (containsMarker(buffer, filled)) return true
      carried = minOf(OVERLAP, filled)
      System.arraycopy(buffer, filled - carried, buffer, 0, carried)
    }
  }

  private fun containsMarker(bytes: ByteArray, length: Int): Boolean =
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

  private fun readInt32(bytes: ByteArray): Int =
    (bytes[0].toInt() and 0xFF shl 24) or
      (bytes[1].toInt() and 0xFF shl 16) or
      (bytes[2].toInt() and 0xFF shl 8) or
      (bytes[3].toInt() and 0xFF)

  private fun readFully(stream: InputStream, buffer: ByteArray): Int {
    var total = 0
    while (total < buffer.size) {
      val read = stream.read(buffer, total, buffer.size - total)
      if (read == -1) break
      total += read
    }
    return total
  }

  /** [InputStream.skip] may return short without being at the end, so it is driven to completion. */
  private fun skipFully(stream: InputStream, count: Long): Long {
    var remaining = count
    while (remaining > 0) {
      val skipped = stream.skip(remaining)
      if (skipped > 0) {
        remaining -= skipped
        continue
      }
      if (stream.read() == -1) break
      remaining--
    }
    return count - remaining
  }
}
