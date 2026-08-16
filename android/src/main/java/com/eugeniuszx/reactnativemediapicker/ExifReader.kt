package com.eugeniuszx.reactnativemediapicker

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface

internal object ExifReader {
  fun read(resolver: ContentResolver, uri: Uri): ExifPayload? = try {
    resolver.openInputStream(uri)?.use { from(ExifInterface(it)) }
  } catch (e: Exception) {
    Log.w(ReactNativeMediaPickerModule.NAME, "failed to read exif for $uri", e)
    null
  }

  fun from(exif: ExifInterface): ExifPayload? {
    val payload = ExifPayload(
      dateTimeOriginal = ExifDate.iso8601(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)),
      latitude = GPSCoordinate.decimal(
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
      ),
      longitude = GPSCoordinate.decimal(
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
      ),
      altitude = GPSCoordinate.altitude(
        exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE),
        exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF),
      ),
      make = exif.getAttribute(ExifInterface.TAG_MAKE),
      model = exif.getAttribute(ExifInterface.TAG_MODEL),
      orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0).takeIf { it in 1..8 },
      iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
        .takeIf { it > 0 },
      fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0.0 },
      exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        .takeIf { it > 0.0 },
      focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        .takeIf { it > 0.0 },
    )
    return if (payload.isEmpty) null else payload
  }
}
