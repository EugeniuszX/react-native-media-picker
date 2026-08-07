import UIKit

enum ViewControllerLocator {
  static func topViewController() -> UIViewController? {
    guard let root = keyWindow()?.rootViewController else { return nil }
    var top = root
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
