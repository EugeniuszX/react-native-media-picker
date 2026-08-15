import Foundation

struct Thumbnail: Equatable {
  let uri: String
  let width: Int
  let height: Int
}

struct AssetPayload: Equatable {
  let uri: String
  let mime: String
  let fileName: String
  let fileSize: Int
  let width: Int
  let height: Int
  let base64: String?
  var duration: Double? = nil
  var thumbnail: Thumbnail? = nil
  var exif: ExifPayload? = nil

  var dictionary: [String: Any] {
    var dict: [String: Any] = [
      "uri": uri,
      "type": mime,
      "fileName": fileName,
      "fileSize": fileSize,
      "width": width,
      "height": height,
    ]
    if let base64 {
      dict["base64"] = base64
    }
    if let duration {
      dict["duration"] = duration
    }
    if let thumbnail {
      dict["thumbnailUri"] = thumbnail.uri
      dict["thumbnailWidth"] = thumbnail.width
      dict["thumbnailHeight"] = thumbnail.height
    }
    if let exif {
      dict["exif"] = exif.dictionary
    }
    return dict
  }
}
