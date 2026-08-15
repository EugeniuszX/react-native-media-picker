package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataPlanTest {
  @Test fun stripDisabledAlwaysSkips() {
    for (willTransform in listOf(true, false)) {
      for (preserveSource in listOf(true, false)) {
        for (canScrub in listOf(true, false)) {
          assertEquals(
            "transform $willTransform preserve $preserveSource canScrub $canScrub",
            MetadataAction.SKIP,
            MetadataPlan.resolve(false, willTransform, preserveSource, canScrub),
          )
        }
      }
    }
  }

  @Test fun transformAlreadyDropsMetadata() {
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, true, false, true))
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, true, false, false))
  }

  @Test fun preserveSourceGuardsOnlyTheReencode() {
    // A scrub rewrites the container without decoding a pixel, so a preserved source is still
    // scrubbed when its container can be rewritten — only the re-encode is held back.
    assertEquals(MetadataAction.SCRUB, MetadataPlan.resolve(true, false, true, true))
    assertEquals(MetadataAction.SKIP, MetadataPlan.resolve(true, false, true, false))
  }

  @Test fun animatedWebpIsScrubbedRatherThanLeftWithItsGps() {
    assertEquals(
      MetadataAction.SCRUB,
      MetadataPlan.resolve(
        stripMetadata = true,
        willTransform = false,
        preserveSource = MetadataPlan.preservesSource("image/webp", preserveAnimation = true),
        canScrub = MetadataPlan.canScrub("image/webp"),
      ),
    )
  }

  @Test fun gifIsLeftAloneWhetherAnimatedOrNot() {
    for (animated in listOf(true, false)) {
      assertEquals(
        "animated $animated",
        MetadataAction.SKIP,
        MetadataPlan.resolve(
          stripMetadata = true,
          willTransform = false,
          preserveSource = MetadataPlan.preservesSource("image/gif", preserveAnimation = animated),
          canScrub = MetadataPlan.canScrub("image/gif"),
        ),
      )
    }
  }

  @Test fun scrubbableStillImageIsScrubbed() {
    assertEquals(MetadataAction.SCRUB, MetadataPlan.resolve(true, false, false, true))
  }

  @Test fun unscrubbableStillImageIsReencoded() {
    assertEquals(MetadataAction.FORCE_REENCODE, MetadataPlan.resolve(true, false, false, false))
  }

  @Test fun animatedSourcesArePreserved() {
    assertTrue(MetadataPlan.preservesSource("image/webp", preserveAnimation = true))
  }

  @Test fun gifsArePreservedEvenWhenStatic() {
    assertTrue(MetadataPlan.preservesSource("image/gif", preserveAnimation = false))
    assertTrue(MetadataPlan.preservesSource("image/gif", preserveAnimation = true))
  }

  @Test fun staticNonGifSourcesAreNotPreserved() {
    for (mime in listOf("image/jpeg", "image/png", "image/heic", "image/webp")) {
      assertFalse(mime, MetadataPlan.preservesSource(mime, preserveAnimation = false))
    }
  }

  @Test fun canScrubCoversTheContainersExifInterfaceCanWrite() {
    assertTrue(MetadataPlan.canScrub("image/jpeg"))
    assertTrue(MetadataPlan.canScrub("image/png"))
    assertTrue(MetadataPlan.canScrub("image/webp"))
    assertFalse(MetadataPlan.canScrub("image/heic"))
    assertFalse(MetadataPlan.canScrub("image/gif"))
  }
}
