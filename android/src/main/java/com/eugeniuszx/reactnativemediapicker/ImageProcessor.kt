package com.eugeniuszx.reactnativemediapicker

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

internal class ImageProcessor(
  private val resolver: ContentResolver,
  private val tempFiles: TempFileStore,
) {
  fun process(
    uri: Uri,
    format: RequestedFormat,
    maxWidth: Int,
    maxHeight: Int,
    quality: Int,
    includeBase64: Boolean,
    stripMetadata: Boolean = false,
    includeExif: Boolean = false,
    suggestedName: String? = null,
  ): AssetPayload {
    val srcMime = MediaFormat.normalizeMime(resolver.getType(uri))
    val bounds = readBounds(uri)
    val orientation = readOrientation(uri)
    val exif = if (includeExif) ExifReader.read(resolver, uri) else null
    val isAnimated = srcMime == "image/gif" ||
      (srcMime == "image/webp" && isAnimatedWebp(uri))

    val output = OutputPlan.resolve(srcMime, format, isAnimated)
    val plan = DecodePlan.compute(
      pixelWidth = bounds.first,
      pixelHeight = bounds.second,
      orientation = orientation,
      maxWidth = maxWidth,
      maxHeight = maxHeight,
      isAnimated = output.preserveAnimation,
    )

    val willTransform = plan.needsTransform || output.forceReencode
    val preserveSource = MetadataPlan.preservesSource(srcMime, output.preserveAnimation)
    val action = MetadataPlan.resolve(
      stripMetadata = stripMetadata,
      willTransform = willTransform,
      preserveSource = preserveSource,
      canScrub = MetadataPlan.canScrub(srcMime),
    )

    if (willTransform || action == MetadataAction.FORCE_REENCODE) {
      return transform(
        uri, output.target, plan, orientation, quality, includeBase64, suggestedName, exif,
      )
    }

    if (action == MetadataAction.SCRUB) {
      val outFile = copyToTemp(uri, srcMime)
      // A source carrying an XMP packet is declined before the scrub is even attempted:
      // ExifInterface cannot remove XMP, and a packet can carry `exif:GPSLatitude`, `tiff:Make`
      // and `tiff:Model` of its own — so a scrubbed file would still leak exactly what the caller
      // asked to remove. The re-encode below is the deliberate price of a guarantee that holds
      // for every input rather than one that quietly leaks for files from an XMP-writing
      // pipeline (Lightroom, Google Photos).
      if (!XMPPacket.isPresent(outFile) && MetadataScrubber.scrub(outFile)) {
        return payloadFrom(outFile, srcMime, plan, includeBase64, suggestedName, exif)
      }
      outFile.delete()
      // The scrub could not be completed, so a re-encode is the fallback that keeps the strip
      // honest — unless this is a source we are not allowed to re-encode. The only route there
      // is an animated WebP carrying an XMP packet: a re-encode would flatten it to a single
      // frame, so the frames win and the file comes back untouched. That matches iOS, which
      // cannot rewrite a WebP at all and skips every one of them.
      if (!preserveSource) {
        return transform(
          uri, output.target, plan, orientation, quality, includeBase64, suggestedName, exif,
        )
      }
    }

    return payloadFrom(copyToTemp(uri, srcMime), srcMime, plan, includeBase64, suggestedName, exif)
  }

  private fun readBounds(uri: Uri): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
    return options.outWidth to options.outHeight
  }

  private fun readOrientation(uri: Uri): ExifOrientation {
    val value = try {
      resolver.openInputStream(uri).use { stream ->
        stream ?: return ExifOrientation.UPRIGHT
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL,
        )
      }
    } catch (e: Exception) {
      ExifInterface.ORIENTATION_NORMAL
    }
    return ExifOrientation.fromExifValue(value)
  }

  private fun isAnimatedWebp(uri: Uri): Boolean = try {
    resolver.openInputStream(uri).use { stream ->
      stream ?: return false
      val header = ByteArray(21)
      var total = 0
      while (total < header.size) {
        val read = stream.read(header, total, header.size - total)
        if (read == -1) break
        total += read
      }
      total >= header.size && MediaFormat.isAnimatedWebp(header)
    }
  } catch (e: Exception) {
    false
  }

  /** Copies the source bytes into a temp file untouched. */
  private fun copyToTemp(uri: Uri, mime: String): File {
    val outFile = tempFiles.createFile(MediaFormat.extensionForMime(mime))
    resolver.openInputStream(uri).use { input ->
      input ?: throw IllegalStateException("Failed to open image stream")
      FileOutputStream(outFile).use { output -> input.copyTo(output) }
    }
    return outFile
  }

  /**
   * Describes a file that was copied rather than re-encoded. The base64 is read back from the
   * file so it reflects a scrub, rather than the bytes that went in.
   */
  private fun payloadFrom(
    file: File,
    mime: String,
    plan: DecodePlan,
    includeBase64: Boolean,
    suggestedName: String?,
    exif: ExifPayload?,
  ): AssetPayload {
    val base64 = if (includeBase64) {
      Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    } else {
      null
    }
    return payload(file, mime, plan.displayWidth, plan.displayHeight, base64, suggestedName, exif)
  }

  private fun transform(
    uri: Uri,
    target: MediaFormat.OutputFormat,
    plan: DecodePlan,
    orientation: ExifOrientation,
    quality: Int,
    includeBase64: Boolean,
    suggestedName: String?,
    exif: ExifPayload?,
  ): AssetPayload {
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = plan.sampleSize }
    var bitmap = resolver.openInputStream(uri).use {
      BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: throw IllegalStateException("Failed to decode image")

    bitmap = applyOrientation(bitmap, orientation)
    bitmap = scaleTo(bitmap, plan.targetWidth, plan.targetHeight)

    val outMime = MediaFormat.reencodeMime(target)
    val outFile = tempFiles.createFile(MediaFormat.extensionForMime(outMime))
    FileOutputStream(outFile).use { output ->
      if (!bitmap.compress(compressFormat(target), quality, output)) {
        bitmap.recycle()
        outFile.delete()
        throw IllegalStateException("Failed to encode image as $outMime")
      }
    }

    val width = bitmap.width
    val height = bitmap.height
    bitmap.recycle()

    val base64 = if (includeBase64) {
      Base64.encodeToString(outFile.readBytes(), Base64.NO_WRAP)
    } else {
      null
    }
    return payload(outFile, outMime, width, height, base64, suggestedName, exif)
  }

  private fun applyOrientation(bitmap: Bitmap, orientation: ExifOrientation): Bitmap {
    if (orientation == ExifOrientation.UPRIGHT) return bitmap
    val matrix = Matrix().apply {
      if (orientation.rotationDegrees != 0) postRotate(orientation.rotationDegrees.toFloat())
      if (orientation.isMirrored) postScale(-1f, 1f)
    }
    val oriented =
      Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (oriented != bitmap) bitmap.recycle()
    return oriented
  }

  private fun scaleTo(bitmap: Bitmap, width: Int, height: Int): Bitmap {
    if (bitmap.width == width && bitmap.height == height) return bitmap
    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
  }

  @Suppress("DEPRECATION")
  private fun compressFormat(format: MediaFormat.OutputFormat): Bitmap.CompressFormat =
    when (format) {
      MediaFormat.OutputFormat.PNG -> Bitmap.CompressFormat.PNG
      MediaFormat.OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
      MediaFormat.OutputFormat.WEBP ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          Bitmap.CompressFormat.WEBP
        }
    }

  private fun payload(
    file: File,
    mime: String,
    width: Int,
    height: Int,
    base64: String?,
    suggestedName: String?,
    exif: ExifPayload?,
  ) = AssetPayload(
    uri = Uri.fromFile(file).toString(),
    mime = mime,
    fileName = AssetFileName.resolve(suggestedName, file.name, file.extension),
    fileSize = file.length(),
    width = width,
    height = height,
    base64 = base64,
    exif = exif,
  )
}
