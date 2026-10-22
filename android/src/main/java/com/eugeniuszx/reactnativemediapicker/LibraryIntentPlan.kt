package com.eugeniuszx.reactnativemediapicker

internal enum class LibraryStrategy {
  SYSTEM_PICK_IMAGES,
  GET_CONTENT,
}

internal sealed class LibraryRequest {
  data class SystemPicker(val mimeType: String?, val maxItems: Int?) : LibraryRequest()

  data class GetContent(val mimeTypes: List<String>, val allowMultiple: Boolean) : LibraryRequest()
}

internal object LibraryIntentPlan {
  fun strategy(sdkInt: Int, extensionR: Int?): LibraryStrategy =
    if (sdkInt >= 33 || (sdkInt >= 30 && extensionR != null && extensionR >= 2)) {
      LibraryStrategy.SYSTEM_PICK_IMAGES
    } else {
      LibraryStrategy.GET_CONTENT
    }

  fun request(
    strategy: LibraryStrategy,
    mediaType: RequestedMediaType,
    selectionLimit: Int,
  ): LibraryRequest = when (strategy) {
    LibraryStrategy.SYSTEM_PICK_IMAGES -> systemPicker(mediaType, selectionLimit)
    LibraryStrategy.GET_CONTENT -> getContent(mediaType, selectionLimit)
  }

  fun systemPicker(mediaType: RequestedMediaType, selectionLimit: Int) =
    LibraryRequest.SystemPicker(
      mimeType = pickImagesMimeType(mediaType),
      maxItems = selectionLimit.takeIf { it != 1 },
    )

  fun getContent(mediaType: RequestedMediaType, selectionLimit: Int) =
    LibraryRequest.GetContent(
      mimeTypes = getContentMimeTypes(mediaType),
      allowMultiple = selectionLimit != 1,
    )

  fun pickImagesMax(requested: Int, systemMax: Int): Int =
    if (requested == 0) systemMax else requested.coerceAtMost(systemMax)

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
