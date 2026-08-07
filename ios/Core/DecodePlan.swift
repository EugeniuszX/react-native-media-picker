import Foundation

struct DecodePlan: Equatable {
  let displayWidth: Int
  let displayHeight: Int
  let targetWidth: Int
  let targetHeight: Int
  let sampleSize: Int
  let needsTransform: Bool

  static func compute(
    pixelWidth: Int,
    pixelHeight: Int,
    orientation: ImageOrientation,
    maxWidth: Int,
    maxHeight: Int,
    isAnimated: Bool
  ) -> DecodePlan {
    let displayWidth = orientation.swapsAxes ? pixelHeight : pixelWidth
    let displayHeight = orientation.swapsAxes ? pixelWidth : pixelHeight

    func passthrough() -> DecodePlan {
      DecodePlan(
        displayWidth: max(0, displayWidth),
        displayHeight: max(0, displayHeight),
        targetWidth: max(0, displayWidth),
        targetHeight: max(0, displayHeight),
        sampleSize: 1,
        needsTransform: false
      )
    }

    guard displayWidth > 0, displayHeight > 0, !isAnimated else {
      return passthrough()
    }

    let boundWidth = maxWidth > 0 ? maxWidth : displayWidth
    let boundHeight = maxHeight > 0 ? maxHeight : displayHeight
    guard displayWidth > boundWidth || displayHeight > boundHeight else {
      return passthrough()
    }

    let cappedWidth = min(boundWidth, displayWidth)
    let cappedHeight = min(boundHeight, displayHeight)
    let widthBinds = cappedWidth * displayHeight <= cappedHeight * displayWidth
    let targetWidth =
      widthBinds
      ? cappedWidth
      : max(1, cappedHeight * displayWidth / displayHeight)
    let targetHeight =
      widthBinds
      ? max(1, cappedWidth * displayHeight / displayWidth)
      : cappedHeight

    var sampleSize = 1
    while displayWidth / (sampleSize * 2) >= targetWidth,
      displayHeight / (sampleSize * 2) >= targetHeight
    {
      sampleSize *= 2
    }

    return DecodePlan(
      displayWidth: displayWidth,
      displayHeight: displayHeight,
      targetWidth: targetWidth,
      targetHeight: targetHeight,
      sampleSize: sampleSize,
      needsTransform: true
    )
  }
}
