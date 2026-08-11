import AVFoundation
import Foundation

final class VideoProcessor {
  private let tempFiles: TempFileStore

  init(tempFiles: TempFileStore) {
    self.tempFiles = tempFiles
  }

  func process(sourceURL: URL, uti: String) -> AssetPayload? {
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
      fileName: destination.lastPathComponent,
      fileSize: fileSize,
      width: width,
      height: height,
      base64: nil,
      duration: seconds.isFinite && seconds > 0 ? seconds : nil
    )
  }
}
