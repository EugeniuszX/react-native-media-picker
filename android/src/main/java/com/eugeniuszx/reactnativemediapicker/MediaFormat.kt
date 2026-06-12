package com.eugeniuszx.reactnativemediapicker

/**
 * Pure format helpers — no Android-framework imports so they stay JVM-unit-testable.
 * They decide the output mime/extension and which encoder to use when a resize
 * forces a re-encode.
 */
internal object MediaFormat {
  /** Formats the picker can actually encode on Android. */
  enum class OutputFormat { JPEG, PNG, WEBP }

  /** Collapse content-resolver mimes to a canonical set; unknown/null -> jpeg. */
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

  /**
   * Encoder to use when a transform (resize) forces a re-encode. HEIC has no
   * Android encoder, GIF is never resized, so both fall back to JPEG.
   */
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

  /**
   * Animated-WebP detection via the RIFF/VP8X header. `header` should hold at
   * least the first 21 bytes of the file. Animation = bit 0x02 of the flags
   * byte at offset 20 in a VP8X chunk.
   */
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
    return (header[20].toInt() and 0x02) != 0
  }
}
