import Foundation

enum CameraFacing: String {
  case back
  case front

  static func from(rawValue: String) -> CameraFacing {
    CameraFacing(rawValue: rawValue) ?? .back
  }
}

enum CameraMediaType: String {
  case photo
  case video

  static func from(rawValue: String) -> CameraMediaType {
    CameraMediaType(rawValue: rawValue) ?? .photo
  }
}

enum VideoQuality: String {
  case low
  case medium
  case high

  static func from(rawValue: String) -> VideoQuality {
    VideoQuality(rawValue: rawValue) ?? .high
  }
}

struct LibraryOptions: Equatable {
  let selectionLimit: Int
  let maxWidth: Int
  let maxHeight: Int
  let quality: Double
  let includeBase64: Bool
  let format: RequestedFormat
  let mediaType: RequestedMediaType
  let includeThumbnail: Bool
  let includeExif: Bool
  let stripMetadata: Bool
}

struct CameraOptions: Equatable {
  let facing: CameraFacing
  let mediaType: CameraMediaType
  let maxWidth: Int
  let maxHeight: Int
  let quality: Double
  let includeBase64: Bool
  let format: RequestedFormat
  let maxDuration: Int
  let videoQuality: VideoQuality
  let includeThumbnail: Bool
  let includeExif: Bool
  let stripMetadata: Bool
}

enum RequestedFormat: String {
  case original
  case jpeg
  case png

  static func from(rawValue: String) -> RequestedFormat {
    RequestedFormat(rawValue: rawValue) ?? .original
  }
}

enum RequestedMediaType: String {
  case photo
  case video
  case mixed

  static func from(rawValue: String) -> RequestedMediaType {
    RequestedMediaType(rawValue: rawValue) ?? .photo
  }
}
