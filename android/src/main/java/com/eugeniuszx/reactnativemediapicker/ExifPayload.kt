package com.eugeniuszx.reactnativemediapicker

/**
 * The normalised metadata subset returned as `Asset.exif`. Every field is optional: a source
 * without a GPS block simply has no coordinates.
 */
internal data class ExifPayload(
  val dateTimeOriginal: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val altitude: Double? = null,
  val make: String? = null,
  val model: String? = null,
  val orientation: Int? = null,
  val iso: Int? = null,
  val fNumber: Double? = null,
  val exposureTime: Double? = null,
  val focalLength: Double? = null,
) {
  val isEmpty: Boolean
    get() = dateTimeOriginal == null && latitude == null && longitude == null &&
      altitude == null && make == null && model == null && orientation == null &&
      iso == null && fNumber == null && exposureTime == null && focalLength == null
}
