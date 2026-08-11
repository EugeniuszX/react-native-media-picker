package com.eugeniuszx.reactnativemediapicker

internal object MediaFormat {
  enum class OutputFormat { JPEG, PNG, WEBP }

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

  fun isVideoMime(mime: String?): Boolean =
    mime?.lowercase()?.startsWith("video/") == true

  fun normalizeVideoMime(mime: String?): String = when (mime?.lowercase()) {
    "video/quicktime" -> "video/quicktime"
    "video/webm" -> "video/webm"
    "video/3gpp" -> "video/3gpp"
    else -> "video/mp4"
  }

  fun extensionForVideoMime(mime: String): String = when (mime) {
    "video/quicktime" -> "mov"
    "video/webm" -> "webm"
    "video/3gpp" -> "3gp"
    else -> "mp4"
  }
}
