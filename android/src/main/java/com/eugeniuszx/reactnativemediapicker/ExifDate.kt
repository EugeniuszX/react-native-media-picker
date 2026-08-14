package com.eugeniuszx.reactnativemediapicker

internal object ExifDate {
  private val PATTERN = Regex("""^(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})$""")

  /**
   * Converts an EXIF timestamp (`'2026:08:14 15:29:03'`) to ISO-8601 without a timezone.
   * EXIF carries no offset, so none is invented. Anything malformed yields null rather than
   * a half-parsed value.
   */
  fun iso8601(raw: String?): String? {
    val match = PATTERN.matchEntire(raw?.trim() ?: return null) ?: return null
    val (year, month, day, hour, minute, second) = match.destructured
    // Cameras write an all-zero placeholder when the clock was never set.
    if (year == "0000") return null
    return "$year-$month-${day}T$hour:$minute:$second"
  }
}
