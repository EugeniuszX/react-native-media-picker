import AVFoundation
import Foundation
import UIKit

final class VideoProcessor {
  private let tempFiles: TempFileStore

  init(tempFiles: TempFileStore) {
    self.tempFiles = tempFiles
  }

  func process(
    sourceURL: URL,
    uti: String,
    includeThumbnail: Bool = false,
    suggestedName: String? = nil
  ) -> AssetPayload? {
    let format = VideoFormat.from(uti: uti)
    guard let destination = try? tempFiles.makeFileURL(fileExtension: format.fileExtension)
    else { return nil }
    do {
      try FileManager.default.copyItem(at: sourceURL, to: destination)
    } catch {
      NSLog(
        "[ReactNativeMediaPicker] failed to copy video: %@", error.localizedDescription)
      try? FileManager.default.removeItem(at: destination)
      return nil
    }

    let asset = AVURLAsset(url: destination)
    let seconds = CMTimeGetSeconds(asset.duration)
    var width = 0
    var height = 0
    if let track = asset.tracks(withMediaType: .video).first {
      let size = VideoMetadata.displayedSize(
        naturalSize: track.naturalSize,
        preferredTransform: track.preferredTransform
      )
      width = size.width
      height = size.height
    }
    let fileSize =
      (try? destination.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0

    return AssetPayload(
      uri: destination.absoluteString,
      mime: format.mime,
      fileName: AssetFileName.resolve(
        suggested: suggestedName,
        fallback: destination.lastPathComponent,
        fileExtension: destination.pathExtension
      ),
      fileSize: fileSize,
      width: width,
      height: height,
      base64: nil,
      duration: seconds.isFinite && seconds > 0 ? seconds : nil,
      thumbnail: includeThumbnail ? makeThumbnail(for: asset) : nil
    )
  }

  private func makeThumbnail(for asset: AVURLAsset) -> Thumbnail? {
    let generator = AVAssetImageGenerator(asset: asset)
    generator.appliesPreferredTrackTransform = true
    generator.maximumSize = CGSize(
      width: ThumbnailPlan.maxDimension,
      height: ThumbnailPlan.maxDimension
    )
    generator.requestedTimeToleranceBefore = .zero
    generator.requestedTimeToleranceAfter = CMTime(seconds: 1, preferredTimescale: 600)

    guard let frame = try? generator.copyCGImage(at: .zero, actualTime: nil) else {
      NSLog("[ReactNativeMediaPicker] failed to generate a video thumbnail")
      return nil
    }

    guard
      let data = UIImage(cgImage: frame).jpegData(
        compressionQuality: ThumbnailPlan.jpegQuality),
      let destination = try? tempFiles.makeFileURL(fileExtension: "jpg")
    else { return nil }

    do {
      try data.write(to: destination)
    } catch {
      NSLog(
        "[ReactNativeMediaPicker] failed to write a video thumbnail: %@",
        error.localizedDescription)
      return nil
    }

    return Thumbnail(
      uri: destination.absoluteString,
      width: frame.width,
      height: frame.height
    )
  }
}
