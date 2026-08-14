package com.eugeniuszx.reactnativemediapicker

internal object GPSCoordinate {
  /**
   * ExifInterface reports latitude and longitude as three rationals (`'50/1,27/1,36/100'`)
   * plus a hemisphere ref. Returns signed decimal degrees, or null when anything is malformed.
   */
  fun decimal(rational: String?, ref: String?): Double? {
    val parts = rational?.split(",") ?: return null
    if (parts.size != 3) return null

    var magnitude = 0.0
    val divisors = doubleArrayOf(1.0, 60.0, 3600.0)
    for (index in parts.indices) {
      val value = rationalToDouble(parts[index]) ?: return null
      magnitude += value / divisors[index]
    }

    // `toDoubleOrNull` accepts `Infinity` and `NaN`; neither is a coordinate.
    if (!magnitude.isFinite()) return null

    val hemisphere = ref?.trim()?.uppercase()
    val negative = hemisphere == "S" || hemisphere == "W"
    return if (negative) -magnitude else magnitude
  }

  /** `GPSAltitudeRef` is `"1"` when the altitude is below sea level, `"0"` (or absent) above it. */
  fun altitude(rational: String?, ref: String?): Double? {
    val value = rationalToDouble(rational ?: return null) ?: return null
    if (!value.isFinite()) return null
    return if (ref?.trim() == "1") -value else value
  }

  private fun rationalToDouble(raw: String): Double? {
    val parts = raw.trim().split("/")
    if (parts.size != 2) return null
    val numerator = parts[0].toDoubleOrNull() ?: return null
    val denominator = parts[1].toDoubleOrNull() ?: return null
    if (denominator == 0.0) return null
    return numerator / denominator
  }
}
