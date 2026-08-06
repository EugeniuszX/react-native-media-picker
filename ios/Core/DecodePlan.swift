import Foundation

/// Everything decided from metadata alone, before a single pixel is decoded:
/// whether the original bytes can be handed back untouched and, if not, what
/// the output should look like.
///
/// All sizes are in *display* space — i.e. after the EXIF orientation has been
/// applied — because that is the space the caller's `maxWidth`/`maxHeight` are
/// expressed in.
struct DecodePlan: Equatable {
  /// Size the image occupies once its orientation is applied.
  let displayWidth: Int
  let displayHeight: Int
  /// Size after resizing. Equal to the display size when no resize is needed.
  let targetWidth: Int
  let targetHeight: Int
  /// Power-of-two downsample factor to feed the decoder. Always at least 1, and
  /// never so large that the decoded buffer falls below the target size.
  let sampleSize: Int
  /// When false, the original encoded bytes are returned verbatim.
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
        displayWidth: displayWidth,
        displayHeight: displayHeight,
        targetWidth: displayWidth,
        targetHeight: displayHeight,
        sampleSize: 1,
        needsTransform: false
      )
    }

    // Unreadable metadata, or a format we must not re-encode: hand the bytes back.
    guard displayWidth > 0, displayHeight > 0, !isAnimated else {
      return passthrough()
    }

    let boundWidth = maxWidth > 0 ? maxWidth : displayWidth
    let boundHeight = maxHeight > 0 ? maxHeight : displayHeight
    guard displayWidth > boundWidth || displayHeight > boundHeight else {
      return passthrough()
    }

    // Integer arithmetic on purpose. `(bound / display) * display` in Double is
    // not exactly `bound` for many real sensor sizes, and flooring the shortfall
    // costs a pixel: 5712x4284 bounded to 1000 yields 999. Comparing the two
    // aspect ratios as cross-products picks the binding axis exactly; the free
    // axis is then one integer division. The binding axis lands on its bound
    // exactly, so `maxWidth`/`maxHeight` stay a ceiling that is actually reached.
    //
    // Bounds are capped at the display size first. That is behaviour-neutral —
    // the guard above proved at least one axis exceeds its bound, and an axis
    // whose bound is looser than the source can never be the binding one — but
    // it caps both products at `displayWidth * displayHeight`, so a caller who
    // passes a sentinel like Number.MAX_SAFE_INTEGER instead of the documented
    // 0 gets a correct answer rather than an Int overflow trap.
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
