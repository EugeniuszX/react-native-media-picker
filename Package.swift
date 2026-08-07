// swift-tools-version:5.9
//
// Dev-only manifest. It exists so `swift test` can exercise ios/Core on macOS
// without a simulator. The shipped library is built by CocoaPods from the
// podspec — this file is not part of the npm package.
import PackageDescription

let package = Package(
  name: "MediaPickerCore",
  platforms: [.macOS(.v12), .iOS(.v15)],
  targets: [
    .target(name: "MediaPickerCore", path: "ios/Core"),
    .testTarget(
      name: "MediaPickerCoreTests",
      dependencies: ["MediaPickerCore"],
      path: "ios/CoreTests"
    ),
  ]
)
