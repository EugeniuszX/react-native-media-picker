package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputPlanTest {
  @Test fun parsesKnownValuesAndFallsBackToOriginal() {
    assertEquals(RequestedFormat.ORIGINAL, RequestedFormat.from("original"))
    assertEquals(RequestedFormat.JPEG, RequestedFormat.from("jpeg"))
    assertEquals(RequestedFormat.PNG, RequestedFormat.from("png"))
    assertEquals(RequestedFormat.ORIGINAL, RequestedFormat.from("webp"))
    assertEquals(RequestedFormat.ORIGINAL, RequestedFormat.from(null))
  }

  @Test fun originalKeepsCurrentBehavior() {
    assertEquals(
      OutputPlan(forceReencode = false, preserveAnimation = false, target = MediaFormat.OutputFormat.JPEG),
      OutputPlan.resolve("image/jpeg", RequestedFormat.ORIGINAL, isAnimatedSource = false),
    )
    assertEquals(
      OutputPlan(forceReencode = false, preserveAnimation = false, target = MediaFormat.OutputFormat.JPEG),
      OutputPlan.resolve("image/heic", RequestedFormat.ORIGINAL, isAnimatedSource = false),
    )
    assertEquals(
      OutputPlan(forceReencode = false, preserveAnimation = false, target = MediaFormat.OutputFormat.WEBP),
      OutputPlan.resolve("image/webp", RequestedFormat.ORIGINAL, isAnimatedSource = false),
    )
    assertEquals(
      OutputPlan(forceReencode = false, preserveAnimation = true, target = MediaFormat.OutputFormat.JPEG),
      OutputPlan.resolve("image/gif", RequestedFormat.ORIGINAL, isAnimatedSource = true),
    )
  }

  @Test fun explicitFormatMatchingStaticSourcePassesThrough() {
    assertEquals(
      OutputPlan(forceReencode = false, preserveAnimation = false, target = MediaFormat.OutputFormat.JPEG),
      OutputPlan.resolve("image/jpeg", RequestedFormat.JPEG, isAnimatedSource = false),
    )
    assertEquals(
      OutputPlan(forceReencode = false, preserveAnimation = false, target = MediaFormat.OutputFormat.PNG),
      OutputPlan.resolve("image/png", RequestedFormat.PNG, isAnimatedSource = false),
    )
  }

  @Test fun explicitFormatMismatchForcesReencode() {
    assertEquals(
      OutputPlan(forceReencode = true, preserveAnimation = false, target = MediaFormat.OutputFormat.JPEG),
      OutputPlan.resolve("image/heic", RequestedFormat.JPEG, isAnimatedSource = false),
    )
    assertEquals(
      OutputPlan(forceReencode = true, preserveAnimation = false, target = MediaFormat.OutputFormat.PNG),
      OutputPlan.resolve("image/jpeg", RequestedFormat.PNG, isAnimatedSource = false),
    )
  }

  @Test fun explicitFormatDropsAnimationProtection() {
    assertEquals(
      OutputPlan(forceReencode = true, preserveAnimation = false, target = MediaFormat.OutputFormat.JPEG),
      OutputPlan.resolve("image/gif", RequestedFormat.JPEG, isAnimatedSource = true),
    )
    assertEquals(
      OutputPlan(forceReencode = true, preserveAnimation = false, target = MediaFormat.OutputFormat.PNG),
      OutputPlan.resolve("image/webp", RequestedFormat.PNG, isAnimatedSource = true),
    )
  }
}
