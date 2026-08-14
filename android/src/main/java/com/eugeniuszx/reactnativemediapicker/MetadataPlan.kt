package com.eugeniuszx.reactnativemediapicker

/** What to do with the metadata of an asset that is otherwise passed through untouched. */
internal enum class MetadataAction {
  /** Leave the bytes alone. */
  SKIP,

  /** Rewrite the container without its metadata; the pixel data is copied, not re-encoded. */
  SCRUB,

  /** The container cannot be rewritten on this platform, so re-encode to guarantee the strip. */
  FORCE_REENCODE,
}

internal object MetadataPlan {
  fun resolve(
    stripMetadata: Boolean,
    willTransform: Boolean,
    isAnimated: Boolean,
    canScrub: Boolean,
  ): MetadataAction = when {
    !stripMetadata -> MetadataAction.SKIP
    // A re-encode decodes to a bitmap and writes fresh bytes, so no metadata survives it.
    willTransform -> MetadataAction.SKIP
    // Animated sources are never modified — re-encoding them would lose the animation.
    isAnimated -> MetadataAction.SKIP
    canScrub -> MetadataAction.SCRUB
    else -> MetadataAction.FORCE_REENCODE
  }

  /**
   * The containers [androidx.exifinterface.media.ExifInterface.saveAttributes] can write.
   * HEIC is read-only there, so stripping it means re-encoding to JPEG.
   */
  fun canScrub(mime: String): Boolean =
    mime == "image/jpeg" || mime == "image/png" || mime == "image/webp"
}
