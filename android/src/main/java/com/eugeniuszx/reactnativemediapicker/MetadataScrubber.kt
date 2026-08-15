package com.eugeniuszx.reactnativemediapicker

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File

internal object MetadataScrubber {
  /**
   * The only tag that survives a strip. Dropping [ExifInterface.TAG_ORIENTATION] would render a
   * quarter-turned photo sideways, and the reported width/height already assume it survives.
   *
   * iOS keeps exactly this one value too: `ios/MetadataScrubber.swift` nulls the whole TIFF
   * dictionary and rebuilds it from the orientation alone.
   */
  val SURVIVING_TAGS = setOf(ExifInterface.TAG_ORIENTATION)

  /**
   * Every other attribute name [ExifInterface] knows about — an allowlist expressed by inversion,
   * not a denylist. `saveAttributes` writes back everything left in the attribute table, so any
   * tag missing from this list silently survives the strip: a hand-picked denylist of "the
   * sensitive ones" leaks `GPSDestLatitude`, `BodySerialNumber` and `ImageUniqueId` on the very
   * first pass, and rots further every time androidx learns a new tag.
   *
   * `MetadataScrubberTest` reflects over [ExifInterface]'s public `TAG_*` constants and fails if
   * any is in neither this list nor [SURVIVING_TAGS], so an androidx upgrade that adds a tag
   * breaks the build instead of quietly widening the leak.
   *
   * Structural TIFF tags (strip offsets, byte counts) are stripped too. They matter for RAW
   * containers, which never reach this code: `MetadataPlan.canScrub` admits only JPEG, PNG and
   * WebP, and for those `writeExifSegment` re-derives the thumbnail offsets itself.
   */
  @Suppress("DEPRECATION")
  val STRIPPED_TAGS = listOf(
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_BITS_PER_SAMPLE,
    ExifInterface.TAG_BODY_SERIAL_NUMBER,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_CAMARA_OWNER_NAME,
    ExifInterface.TAG_CAMERA_OWNER_NAME,
    ExifInterface.TAG_CFA_PATTERN,
    ExifInterface.TAG_COLOR_SPACE,
    ExifInterface.TAG_COMPONENTS_CONFIGURATION,
    ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
    ExifInterface.TAG_COMPRESSION,
    ExifInterface.TAG_CONTRAST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_CUSTOM_RENDERED,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DEFAULT_CROP_SIZE,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_DNG_VERSION,
    ExifInterface.TAG_EXIF_VERSION,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_EXPOSURE_INDEX,
    ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_FILE_SOURCE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_FLASHPIX_VERSION,
    ExifInterface.TAG_FLASH_ENERGY,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
    ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_GAIN_CONTROL,
    ExifInterface.TAG_GAMMA,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_DEST_BEARING,
    ExifInterface.TAG_GPS_DEST_BEARING_REF,
    ExifInterface.TAG_GPS_DEST_DISTANCE,
    ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
    ExifInterface.TAG_GPS_DEST_LATITUDE,
    ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE,
    ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_DIFFERENTIAL,
    ExifInterface.TAG_GPS_DOP,
    ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_MAP_DATUM,
    ExifInterface.TAG_GPS_MEASURE_MODE,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_SATELLITES,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_STATUS,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_GPS_VERSION_ID,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_IMAGE_LENGTH,
    ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_IMAGE_WIDTH,
    ExifInterface.TAG_INTEROPERABILITY_INDEX,
    ExifInterface.TAG_ISO_SPEED,
    ExifInterface.TAG_ISO_SPEED_LATITUDE_YYY,
    ExifInterface.TAG_ISO_SPEED_LATITUDE_ZZZ,
    ExifInterface.TAG_ISO_SPEED_RATINGS,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_LENS_SERIAL_NUMBER,
    ExifInterface.TAG_LENS_SPECIFICATION,
    ExifInterface.TAG_LIGHT_SOURCE,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MAKER_NOTE,
    ExifInterface.TAG_MAX_APERTURE_VALUE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_NEW_SUBFILE_TYPE,
    ExifInterface.TAG_OECF,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_ORF_ASPECT_FRAME,
    ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
    ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
    ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
    ExifInterface.TAG_PIXEL_X_DIMENSION,
    ExifInterface.TAG_PIXEL_Y_DIMENSION,
    ExifInterface.TAG_PLANAR_CONFIGURATION,
    ExifInterface.TAG_PRIMARY_CHROMATICITIES,
    ExifInterface.TAG_RECOMMENDED_EXPOSURE_INDEX,
    ExifInterface.TAG_REFERENCE_BLACK_WHITE,
    ExifInterface.TAG_RELATED_SOUND_FILE,
    ExifInterface.TAG_RESOLUTION_UNIT,
    ExifInterface.TAG_ROWS_PER_STRIP,
    ExifInterface.TAG_RW2_ISO,
    ExifInterface.TAG_RW2_JPG_FROM_RAW,
    ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
    ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
    ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
    ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
    ExifInterface.TAG_SAMPLES_PER_PIXEL,
    ExifInterface.TAG_SATURATION,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_SCENE_TYPE,
    ExifInterface.TAG_SENSING_METHOD,
    ExifInterface.TAG_SENSITIVITY_TYPE,
    ExifInterface.TAG_SHARPNESS,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
    ExifInterface.TAG_SPECTRAL_SENSITIVITY,
    ExifInterface.TAG_STANDARD_OUTPUT_SENSITIVITY,
    ExifInterface.TAG_STRIP_BYTE_COUNTS,
    ExifInterface.TAG_STRIP_OFFSETS,
    ExifInterface.TAG_SUBFILE_TYPE,
    ExifInterface.TAG_SUBJECT_AREA,
    ExifInterface.TAG_SUBJECT_DISTANCE,
    ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
    ExifInterface.TAG_SUBJECT_LOCATION,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
    ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
    ExifInterface.TAG_THUMBNAIL_ORIENTATION,
    ExifInterface.TAG_TRANSFER_FUNCTION,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_WHITE_POINT,
    ExifInterface.TAG_XMP,
    ExifInterface.TAG_X_RESOLUTION,
    ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
    ExifInterface.TAG_Y_CB_CR_POSITIONING,
    ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
    ExifInterface.TAG_Y_RESOLUTION,
  )

  /**
   * Rewrites the container in place without its metadata. Pixel data is left alone.
   *
   * Note this cannot remove an XMP packet — `ExifInterface` does not model XMP at all — so the
   * caller gates on [XMPPacket] first and re-encodes instead when one is present.
   */
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
