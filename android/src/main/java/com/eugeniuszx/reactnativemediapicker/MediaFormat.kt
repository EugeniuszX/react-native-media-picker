package com.eugeniuszx.reactnativemediapicker

internal object MediaFormat {
  enum class OutputFormat { JPEG, PNG, WEBP }

  private val imageFtypBrands = listOf("hei", "hevc", "mif1", "msf1", "avif", "avis")

  fun normalizeMime(mime: String?): String = when (mime?.lowercase()) {
    "image/jpeg", "image/jpg" -> "image/jpeg"
    "image/png" -> "image/png"
    "image/webp" -> "image/webp"
    "image/gif" -> "image/gif"
    "image/heic", "image/heif" -> "image/heic"
    else -> "image/jpeg"
  }

  fun extensionForMime(mime: String): String = when (mime) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/gif" -> "gif"
    "image/heic" -> "heic"
    else -> "jpg"
  }

  fun reencodeFormat(mime: String): OutputFormat = when (mime) {
    "image/png" -> OutputFormat.PNG
    "image/webp" -> OutputFormat.WEBP
    else -> OutputFormat.JPEG
  }

  fun reencodeMime(format: OutputFormat): String = when (format) {
    OutputFormat.PNG -> "image/png"
    OutputFormat.WEBP -> "image/webp"
    OutputFormat.JPEG -> "image/jpeg"
  }

  fun isAnimatedWebp(header: ByteArray): Boolean {
    if (header.size < 21) return false
    if (header[0].toInt() != 'R'.code || header[1].toInt() != 'I'.code ||
      header[2].toInt() != 'F'.code || header[3].toInt() != 'F'.code
    ) return false
    if (header[8].toInt() != 'W'.code || header[9].toInt() != 'E'.code ||
      header[10].toInt() != 'B'.code || header[11].toInt() != 'P'.code
    ) return false
    if (header[12].toInt() != 'V'.code || header[13].toInt() != 'P'.code ||
      header[14].toInt() != '8'.code || header[15].toInt() != 'X'.code
    ) return false
    return (header[20].toInt() and 0xFF and 0x02) != 0
  }

  fun isVideoHeader(header: ByteArray): Boolean {
    if (header.size < 4) return false
    if (header[0].toInt() and 0xFF == 0x1A && header[1].toInt() and 0xFF == 0x45 &&
      header[2].toInt() and 0xFF == 0xDF && header[3].toInt() and 0xFF == 0xA3
    ) return true
    if (header.size < 12) return false
    if (header[4].toInt() != 'f'.code || header[5].toInt() != 't'.code ||
      header[6].toInt() != 'y'.code || header[7].toInt() != 'p'.code
    ) return false
    val brand = String(header, 8, 4, Charsets.ISO_8859_1).lowercase()
    return imageFtypBrands.none { brand.startsWith(it) }
  }

  fun isVideoMime(mime: String?): Boolean =
    mime?.lowercase()?.startsWith("video/") == true

  fun normalizeVideoMime(mime: String?): String = when (mime?.lowercase()) {
    "video/quicktime" -> "video/quicktime"
    "video/webm" -> "video/webm"
    "video/3gpp" -> "video/3gpp"
    else -> "video/mp4"
  }

  /** Milliseconds as reported by [android.media.MediaMetadataRetriever] to seconds.
   * Returns null when the value is missing, unparsable or not positive, so a
   * duration that could not be determined stays absent from the payload. */
  fun durationSecondsFrom(raw: String?): Double? =
    raw?.toLongOrNull()?.let { it / 1000.0 }?.takeIf { it > 0 }

  fun extensionForVideoMime(mime: String): String = when (mime) {
    "video/quicktime" -> "mov"
    "video/webm" -> "webm"
    "video/3gpp" -> "3gp"
    else -> "mp4"
  }
}
