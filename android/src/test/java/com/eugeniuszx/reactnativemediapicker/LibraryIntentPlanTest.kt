package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
