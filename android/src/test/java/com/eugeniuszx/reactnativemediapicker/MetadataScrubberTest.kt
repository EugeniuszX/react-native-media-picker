package com.eugeniuszx.reactnativemediapicker

import androidx.exifinterface.media.ExifInterface
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `scrub` itself needs real image bytes and a device, so it is verified on-device. What is
 * verified here is the property the guarantee actually rests on: that the tag list is complete.
 */
class MetadataScrubberTest {
  private fun knownTagNames(): Set<String> =
    ExifInterface::class.java.fields
      .filter {
        Modifier.isStatic(it.modifiers) &&
          it.type == String::class.java &&
          it.name.startsWith("TAG_")
      }
      .map { it.get(null) as String }
      .toSet()

  @Test fun everyTagExifInterfaceKnowsIsEitherStrippedOrDeliberatelyKept() {
    val covered = MetadataScrubber.STRIPPED_TAGS.toSet() + MetadataScrubber.SURVIVING_TAGS
    val uncovered = knownTagNames() - covered
    assertEquals(
      "these tags would survive a strip; add them to STRIPPED_TAGS or SURVIVING_TAGS: $uncovered",
      emptySet<String>(),
      uncovered,
    )
  }

  @Test fun nothingIsBothStrippedAndKept() {
    val overlap = MetadataScrubber.STRIPPED_TAGS.toSet() intersect MetadataScrubber.SURVIVING_TAGS
    assertEquals(emptySet<String>(), overlap)
  }

  @Test fun orientationIsTheOnlySurvivor() {
    assertEquals(setOf(ExifInterface.TAG_ORIENTATION), MetadataScrubber.SURVIVING_TAGS)
    assertFalse(MetadataScrubber.STRIPPED_TAGS.contains(ExifInterface.TAG_ORIENTATION))
  }

  @Test fun everyGpsTagIsStripped() {
    val gps = knownTagNames().filter { it.startsWith("GPS") }
    assertTrue("expected androidx to expose GPS tags", gps.size > 20)
    assertEquals(emptyList<String>(), gps - MetadataScrubber.STRIPPED_TAGS)
  }

  @Test fun theIdentifyingTagsAreStripped() {
    val identifying = listOf(
      ExifInterface.TAG_GPS_LATITUDE,
      ExifInterface.TAG_GPS_LONGITUDE,
      ExifInterface.TAG_GPS_DEST_LATITUDE,
      ExifInterface.TAG_GPS_DEST_LONGITUDE,
      ExifInterface.TAG_GPS_SPEED,
      ExifInterface.TAG_GPS_TRACK,
      ExifInterface.TAG_GPS_IMG_DIRECTION,
      ExifInterface.TAG_GPS_SATELLITES,
      ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
      ExifInterface.TAG_CAMERA_OWNER_NAME,
      ExifInterface.TAG_BODY_SERIAL_NUMBER,
      ExifInterface.TAG_LENS_SERIAL_NUMBER,
      ExifInterface.TAG_IMAGE_UNIQUE_ID,
      ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
      ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
      ExifInterface.TAG_MAKE,
      ExifInterface.TAG_MODEL,
      ExifInterface.TAG_DATETIME_ORIGINAL,
    )
    assertEquals(emptyList<String>(), identifying - MetadataScrubber.STRIPPED_TAGS.toSet())
  }
}
