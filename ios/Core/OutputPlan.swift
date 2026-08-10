import Foundation

struct OutputPlan: Equatable {
  let forceReencode: Bool
  let preserveAnimation: Bool
  let target: ImageFormat

  static func resolve(
    source: ImageFormat,
    requested: RequestedFormat,
    isAnimatedSource: Bool
  ) -> OutputPlan {
    switch requested {
    case .original:
      return OutputPlan(
        forceReencode: false,
        preserveAnimation: isAnimatedSource,
        target: source.reencodeFormat
      )
    case .jpeg:
      return explicit(source: source, target: .jpeg)
    case .png:
      return explicit(source: source, target: .png)
    }
  }

  private static func explicit(source: ImageFormat, target: ImageFormat) -> OutputPlan {
    OutputPlan(
      forceReencode: source != target,
      preserveAnimation: false,
      target: target
    )
  }
}
