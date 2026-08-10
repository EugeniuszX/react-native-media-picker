package com.eugeniuszx.reactnativemediapicker

internal enum class RequestedFormat {
  ORIGINAL,
  JPEG,
  PNG,
  ;

  companion object {
    fun from(raw: String?): RequestedFormat = when (raw) {
      "jpeg" -> JPEG
      "png" -> PNG
      else -> ORIGINAL
    }
  }
}

internal data class OutputPlan(
  val forceReencode: Boolean,
  val preserveAnimation: Boolean,
  val target: MediaFormat.OutputFormat,
) {
  companion object {
    fun resolve(
      sourceMime: String,
      requested: RequestedFormat,
      isAnimatedSource: Boolean,
    ): OutputPlan = when (requested) {
      RequestedFormat.ORIGINAL -> OutputPlan(
        forceReencode = false,
        preserveAnimation = isAnimatedSource,
        target = MediaFormat.reencodeFormat(sourceMime),
      )
      RequestedFormat.JPEG -> explicit(sourceMime, MediaFormat.OutputFormat.JPEG)
      RequestedFormat.PNG -> explicit(sourceMime, MediaFormat.OutputFormat.PNG)
    }

    private fun explicit(
      sourceMime: String,
      target: MediaFormat.OutputFormat,
    ): OutputPlan = OutputPlan(
      forceReencode = sourceMime != MediaFormat.reencodeMime(target),
      preserveAnimation = false,
      target = target,
    )
  }
}
