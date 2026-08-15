package com.eugeniuszx.reactnativemediapicker

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

internal object ResponseFactory {
  fun success(assets: List<AssetPayload>): WritableMap {
    val array = Arguments.createArray()
    assets.forEach { array.pushMap(assetMap(it)) }
    return Arguments.createMap().apply {
      putBoolean("didCancel", false)
      putArray("assets", array)
    }
  }

  fun cancelled(): WritableMap = Arguments.createMap().apply {
    putBoolean("didCancel", true)
  }

  fun failure(error: PickerError, message: String): WritableMap =
    Arguments.createMap().apply {
      putBoolean("didCancel", false)
      putString("errorCode", error.code)
      putString("errorMessage", message)
    }

  private fun assetMap(asset: AssetPayload): WritableMap = Arguments.createMap().apply {
    putString("uri", asset.uri)
    putString("type", asset.mime)
    putString("fileName", asset.fileName)
    putDouble("fileSize", asset.fileSize.toDouble())
    putInt("width", asset.width)
    putInt("height", asset.height)
    asset.base64?.let { putString("base64", it) }
    asset.durationSeconds?.let { putDouble("duration", it) }
    asset.thumbnail?.let {
      putString("thumbnailUri", it.uri)
      putInt("thumbnailWidth", it.width)
      putInt("thumbnailHeight", it.height)
    }
    asset.exif?.let { putMap("exif", exifMap(it)) }
  }

  private fun exifMap(exif: ExifPayload): WritableMap = Arguments.createMap().apply {
    exif.dateTimeOriginal?.let { putString("dateTimeOriginal", it) }
    exif.latitude?.let { putDouble("latitude", it) }
    exif.longitude?.let { putDouble("longitude", it) }
    exif.altitude?.let { putDouble("altitude", it) }
    exif.make?.let { putString("make", it) }
    exif.model?.let { putString("model", it) }
    exif.orientation?.let { putInt("orientation", it) }
    exif.iso?.let { putInt("iso", it) }
    exif.fNumber?.let { putDouble("fNumber", it) }
    exif.exposureTime?.let { putDouble("exposureTime", it) }
    exif.focalLength?.let { putDouble("focalLength", it) }
  }
}
