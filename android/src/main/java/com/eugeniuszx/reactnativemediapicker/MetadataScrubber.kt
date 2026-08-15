package com.eugeniuszx.reactnativemediapicker

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File

internal object MetadataScrubber {
  /**
   * TAG_ORIENTATION is deliberately absent: dropping it would render a quarter-turned photo
   * sideways, and the reported width/height already assume it survives.
   */
  private val STRIPPED_TAGS = listOf(
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_MAKER_NOTE,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
  )

  /** Rewrites the container in place without its metadata. Pixel data is left alone. */
  fun scrub(file: File): Boolean = try {
    val exif = ExifInterface(file)
    STRIPPED_TAGS.forEach { exif.setAttribute(it, null) }
    exif.saveAttributes()
    true
  } catch (e: Exception) {
    Log.w(ReactNativeMediaPickerModule.NAME, "failed to strip metadata from ${file.name}", e)
    false
  }
}
