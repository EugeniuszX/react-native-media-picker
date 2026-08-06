import UIKit

/// Finds the view controller a picker should be presented from. Must be called
/// on the main thread — `UIApplication.shared` is main-actor state.
enum ViewControllerLocator {
  static func topViewController() -> UIViewController? {
    guard let root = keyWindow()?.rootViewController else { return nil }
    var top = root
    // Skip controllers that are on their way out; presenting from one of those
    // silently does nothing.
    while let presented = top.presentedViewController, !presented.isBeingDismissed {
      top = presented
    }
    return top
  }

  private static func keyWindow() -> UIWindow? {
    let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
    let ordered = scenes.sorted { lhs, rhs in
      activationRank(lhs.activationState) < activationRank(rhs.activationState)
    }
    for scene in ordered {
      if let key = scene.windows.first(where: { $0.isKeyWindow }) {
        return key
      }
    }
    // No key window yet (e.g. very early in launch): any visible window will do.
    return ordered.first?.windows.first { !$0.isHidden }
  }

  private static func activationRank(_ state: UIScene.ActivationState) -> Int {
    switch state {
    case .foregroundActive: return 0
    case .foregroundInactive: return 1
    default: return 2
    }
  }
}
