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
    // A re-encode decodes to a bitmap and writes fresh bytes, so no metadata survives it.
    willTransform -> MetadataAction.SKIP
    // Checked before `preserveSource`, because a scrub is a container rewrite and not a
    // re-encode: no pixel is decoded, so an animated WebP keeps every frame while losing the
    // EXIF chunk its camera wrote. Only the re-encode is destructive.
    canScrub -> MetadataAction.SCRUB
    // Left with the choice between re-encoding and doing nothing, and this source cannot be
    // re-encoded: an animated one would lose its frames, and a GIF would be flattened into a
    // JPEG to remove metadata it never had.
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
