package com.eugeniuszx.reactnativemediapicker

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
// NOTE: `Log` is imported by ReactNativeMediaPickerModule.kt, not here.
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Turns a content URI into an [AssetPayload]. Stateless — every call carries its
 * own options, so concurrent calls cannot interfere.
 *
 * Metadata (bounds, mime, EXIF) is read up front so images that need no work are
 * never decoded, and each piece of metadata is read exactly once.
 */
internal class ImageProcessor(
  private val resolver: ContentResolver,
  private val tempFiles: TempFileStore,
) {
  fun process(
    uri: Uri,
    maxWidth: Int,
    maxHeight: Int,
    quality: Int,
    includeBase64: Boolean,
  ): AssetPayload {
    val srcMime = MediaFormat.normalizeMime(resolver.getType(uri))
    val bounds = readBounds(uri)
    val orientation = readOrientation(uri)
    // GIF is always treated as animated; WebP needs a header probe.
    val isAnimated = srcMime == "image/gif" ||
      (srcMime == "image/webp" && isAnimatedWebp(uri))

    val plan = DecodePlan.compute(
      pixelWidth = bounds.first,
      pixelHeight = bounds.second,
      orientation = orientation,
      maxWidth = maxWidth,
      maxHeight = maxHeight,
      isAnimated = isAnimated,
    )

    return if (plan.needsTransform) {
      transform(uri, srcMime, plan, orientation, quality, includeBase64)
    } else {
      passthrough(uri, srcMime, plan, includeBase64)
    }
  }

  // MARK: - Metadata

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

  // MARK: - Passthrough

  /** Copies the original encoded bytes verbatim, preserving format and EXIF. */
  private fun passthrough(
    uri: Uri,
    mime: String,
    plan: DecodePlan,
    includeBase64: Boolean,
  ): AssetPayload {
    val outFile = tempFiles.createFile(MediaFormat.extensionForMime(mime))
    // Stream-copy by default; only hold bytes in memory when base64 is asked for.
    var base64: String? = null
    resolver.openInputStream(uri).use { input ->
      input ?: throw IllegalStateException("Failed to open image stream")
      if (includeBase64) {
        val bytes = input.readBytes()
        outFile.writeBytes(bytes)
        base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      } else {
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
      }
    }
    return payload(outFile, mime, plan.displayWidth, plan.displayHeight, base64)
  }

  // MARK: - Transform

  /** Decodes, orients, scales and re-encodes in the source format. */
  private fun transform(
    uri: Uri,
    srcMime: String,
    plan: DecodePlan,
    orientation: ExifOrientation,
    quality: Int,
    includeBase64: Boolean,
  ): AssetPayload {
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = plan.sampleSize }
    var bitmap = resolver.openInputStream(uri).use {
      BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: throw IllegalStateException("Failed to decode image")

    bitmap = applyOrientation(bitmap, orientation)
    bitmap = scaleTo(bitmap, plan.targetWidth, plan.targetHeight)

    val outFormat = MediaFormat.reencodeFormat(srcMime)
    val outMime = MediaFormat.reencodeMime(outFormat)
    val outFile = tempFiles.createFile(MediaFormat.extensionForMime(outMime))
    // Compress straight to disk — the encoded bytes only enter memory when
    // base64 was requested. A false return means nothing usable was written, so
    // throw rather than hand JS a 0-byte asset; the caller drops the item.
    FileOutputStream(outFile).use { output ->
      if (!bitmap.compress(compressFormat(outFormat), quality, output)) {
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
    return payload(outFile, outMime, width, height, base64)
  }

  /**
   * Applies rotation and mirroring in one matrix pass. The mirror is posted
   * *after* the rotation, which is the order [ExifOrientation.isMirrored]
   * documents — reversing it silently swaps EXIF 5 and EXIF 7.
   */
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
  ) = AssetPayload(
    uri = Uri.fromFile(file).toString(),
    mime = mime,
    fileName = file.name,
    fileSize = file.length(),
    width = width,
    height = height,
    base64 = base64,
  )
}
