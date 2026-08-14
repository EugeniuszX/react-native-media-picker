package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataPlanTest {
  @Test fun stripDisabledAlwaysSkips() {
    for (willTransform in listOf(true, false)) {
      for (isAnimated in listOf(true, false)) {
        for (canScrub in listOf(true, false)) {
          assertEquals(
            "transform $willTransform animated $isAnimated canScrub $canScrub",
            MetadataAction.SKIP,
            MetadataPlan.resolve(false, willTransform, isAnimated, canScrub),
          )
        }
      }
    }
  }

  @Test fun transformAlreadyDropsMetadata() {
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, true, false, true))
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, true, false, false))
  }

  @Test fun animatedSourcesAreNeverTouched() {
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, false, true, true))
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, false, true, false))
  }

  @Test fun scrubbableStillImageIsScrubbed() {
    assertEquals(MetadataAction.SCRUB, MetadataPlan.resolve(true, false, false, true))
  }

  @Test fun unscrubbableStillImageIsReencoded() {
    assertEquals(MetadataAction.FORCE_REENCODE, MetadataPlan.resolve(true, false, false, false))
  }

  @Test fun canScrubCoversTheContainersExifInterfaceCanWrite() {
    assertTrue(MetadataPlan.canScrub("image/jpeg"))
    assertTrue(MetadataPlan.canScrub("image/png"))
    assertTrue(MetadataPlan.canScrub("image/webp"))
    assertFalse(MetadataPlan.canScrub("image/heic"))
    assertFalse(MetadataPlan.canScrub("image/gif"))
  }
}
