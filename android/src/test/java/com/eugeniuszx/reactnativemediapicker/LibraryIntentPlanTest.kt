package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIntentPlanTest {
  @Test fun photoPickerMimeTypePerMediaType() {
    assertEquals("image/*", LibraryIntentPlan.pickImagesMimeType(RequestedMediaType.PHOTO))
    assertEquals("video/*", LibraryIntentPlan.pickImagesMimeType(RequestedMediaType.VIDEO))
    assertNull(LibraryIntentPlan.pickImagesMimeType(RequestedMediaType.MIXED))
  }

  @Test fun getContentMimeTypesPerMediaType() {
    assertEquals(listOf("image/*"), LibraryIntentPlan.getContentMimeTypes(RequestedMediaType.PHOTO))
    assertEquals(listOf("video/*"), LibraryIntentPlan.getContentMimeTypes(RequestedMediaType.VIDEO))
    assertEquals(
      listOf("image/*", "video/*"),
      LibraryIntentPlan.getContentMimeTypes(RequestedMediaType.MIXED),
    )
  }

  @Test fun belowApi30UsesGetContent() {
    assertEquals(LibraryStrategy.GET_CONTENT, LibraryIntentPlan.strategy(24, null))
    assertEquals(LibraryStrategy.GET_CONTENT, LibraryIntentPlan.strategy(29, null))
  }

  @Test fun api30To32WithoutExtensionR2UsesGetContent() {
    for (sdkInt in 30..32) {
      assertEquals(LibraryStrategy.GET_CONTENT, LibraryIntentPlan.strategy(sdkInt, 0))
      assertEquals(LibraryStrategy.GET_CONTENT, LibraryIntentPlan.strategy(sdkInt, 1))
      assertEquals(LibraryStrategy.GET_CONTENT, LibraryIntentPlan.strategy(sdkInt, null))
    }
  }

  @Test fun api30To32WithExtensionR2UsesSystemPicker() {
    for (sdkInt in 30..32) {
      assertEquals(LibraryStrategy.SYSTEM_PICK_IMAGES, LibraryIntentPlan.strategy(sdkInt, 2))
      assertEquals(LibraryStrategy.SYSTEM_PICK_IMAGES, LibraryIntentPlan.strategy(sdkInt, 7))
    }
  }

  @Test fun api33AndAboveUsesSystemPicker() {
    assertEquals(LibraryStrategy.SYSTEM_PICK_IMAGES, LibraryIntentPlan.strategy(33, null))
    assertEquals(LibraryStrategy.SYSTEM_PICK_IMAGES, LibraryIntentPlan.strategy(34, 0))
    assertEquals(LibraryStrategy.SYSTEM_PICK_IMAGES, LibraryIntentPlan.strategy(36, 17))
  }

  @Test fun systemPickerRequestCarriesMaxLimitOnlyForMultiSelect() {
    val single = LibraryIntentPlan.request(
      LibraryStrategy.SYSTEM_PICK_IMAGES,
      RequestedMediaType.PHOTO,
      selectionLimit = 1,
    )
    assertEquals(LibraryRequest.SystemPicker(mimeType = "image/*", maxItems = null), single)

    val unlimited = LibraryIntentPlan.request(
      LibraryStrategy.SYSTEM_PICK_IMAGES,
      RequestedMediaType.MIXED,
      selectionLimit = 0,
    )
    assertEquals(LibraryRequest.SystemPicker(mimeType = null, maxItems = 0), unlimited)

    val limited = LibraryIntentPlan.request(
      LibraryStrategy.SYSTEM_PICK_IMAGES,
      RequestedMediaType.VIDEO,
      selectionLimit = 5,
    )
    assertEquals(LibraryRequest.SystemPicker(mimeType = "video/*", maxItems = 5), limited)
  }

  @Test fun getContentRequestAllowsMultipleOnlyForMultiSelect() {
    val single = LibraryIntentPlan.request(
      LibraryStrategy.GET_CONTENT,
      RequestedMediaType.PHOTO,
      selectionLimit = 1,
    )
    assertEquals(LibraryRequest.GetContent(listOf("image/*"), allowMultiple = false), single)

    val unlimited = LibraryIntentPlan.request(
      LibraryStrategy.GET_CONTENT,
      RequestedMediaType.MIXED,
      selectionLimit = 0,
    )
    assertEquals(
      LibraryRequest.GetContent(listOf("image/*", "video/*"), allowMultiple = true),
      unlimited,
    )

    val limited = LibraryIntentPlan.getContent(RequestedMediaType.VIDEO, selectionLimit = 5)
    assertTrue(limited.allowMultiple)
    assertFalse(LibraryIntentPlan.getContent(RequestedMediaType.VIDEO, 1).allowMultiple)
  }

  @Test fun pickImagesMaxResolvesAgainstTheSystemLimit() {
    assertEquals(100, LibraryIntentPlan.pickImagesMax(requested = 0, systemMax = 100))
    assertEquals(5, LibraryIntentPlan.pickImagesMax(requested = 5, systemMax = 100))
    assertEquals(100, LibraryIntentPlan.pickImagesMax(requested = 500, systemMax = 100))
  }
}
