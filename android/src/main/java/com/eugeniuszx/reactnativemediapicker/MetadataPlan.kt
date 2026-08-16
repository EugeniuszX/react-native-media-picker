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
    preserveSource: Boolean,
    canScrub: Boolean,
  ): MetadataAction = when {
    !stripMetadata -> MetadataAction.SKIP
    willTransform -> MetadataAction.SKIP
    canScrub -> MetadataAction.SCRUB
    preserveSource -> MetadataAction.SKIP
    else -> MetadataAction.FORCE_REENCODE
  }

  /**
   * Whether a source must not be re-encoded, which is what [resolve]'s `preserveSource` expects.
   * Two independent reasons: an animated source would lose its frames, and a GIF re-encodes to
   * JPEG — flattening any transparency onto black to remove metadata a GIF never carries in the
   * first place. So a GIF is preserved whether animated or not.
   *
   * This does not stop a scrub, which rewrites the container without touching the pixels.
   */
  fun preservesSource(mime: String, preserveAnimation: Boolean): Boolean =
    preserveAnimation || mime == "image/gif"

  /**
   * The containers [androidx.exifinterface.media.ExifInterface.saveAttributes] can write.
   * HEIC is read-only there, so stripping it means re-encoding to JPEG.
   */
  fun canScrub(mime: String): Boolean =
    mime == "image/jpeg" || mime == "image/png" || mime == "image/webp"
}
