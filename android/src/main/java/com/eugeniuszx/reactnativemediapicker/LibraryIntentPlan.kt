package com.eugeniuszx.reactnativemediapicker

internal object LibraryIntentPlan {
  fun pickImagesMimeType(mediaType: RequestedMediaType): String? = when (mediaType) {
    RequestedMediaType.PHOTO -> "image/*"
    RequestedMediaType.VIDEO -> "video/*"
    RequestedMediaType.MIXED -> null
  }

  fun getContentMimeTypes(mediaType: RequestedMediaType): List<String> = when (mediaType) {
    RequestedMediaType.PHOTO -> listOf("image/*")
    RequestedMediaType.VIDEO -> listOf("video/*")
    RequestedMediaType.MIXED -> listOf("image/*", "video/*")
  }
}
