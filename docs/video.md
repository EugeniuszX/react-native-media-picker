# Video

How videos picked from the gallery and recorded with the camera behave, what
`includeThumbnail` produces, and what `launchCamera({ mediaType: 'video' })`
returns.

## Video assets

Videos are always passed through untouched: the file the system hands over is
copied byte-for-byte into a temp file and returned as-is. `maxWidth`,
`maxHeight`, `quality`, `format` and `includeBase64` are ignored for video
assets — the library never transcodes, and base64 for videos is deliberately
unsupported. On iOS the copied bytes are the representation PHPicker exports
from the photo library, and the system may re-encode the asset while producing
it, so they are not guaranteed to be identical to the original file in the
library.

Each video asset carries `duration` (seconds), `width`/`height` (displayed
axes — rotation metadata is applied; `0` means the metadata could not be
read) and `fileSize`. A failure to read metadata does not fail the asset:
`duration` is then simply absent, and `width`/`height` come back as `0`.

`type` is derived from the mime the system reports for the picked item:
`video/mp4`, `video/quicktime`, `video/webm` or `video/3gpp`, and `fileName`'s
extension matches it (`mp4`, `mov`, `webm`, `3gp`). That is also why a mime the
library does not recognize — or a missing one — is labeled `video/mp4`; the
bytes are still passed through untouched, exactly as with unrecognized image
types under [Format handling](formats.md). iOS only ever reports
`video/mp4` or `video/quicktime`; `video/webm` and `video/3gpp` come from
Android.

Platform notes:

- **iOS:** videos stored in iCloud are downloaded by the system during the
  pick; large videos can take a while and there is no progress reporting. The
  pick stays in flight for the whole download — and, since only one pick may run
  at a time, new `launchImageLibrary`/`launchCamera` calls are rejected until it
  finishes. Slo-mo videos arrive as regular movies. A Live Photo is returned as
  its still image (also under `mediaType: 'mixed'`) — the motion part is not
  extracted.
- **Android:** on devices without the system Photo Picker the fallback
  `ACTION_GET_CONTENT` chooser is used, same as for photos. The provider's mime
  type is trusted when it reports one; when it reports none or a generic one,
  the library falls back to sniffing the file header (ISO-BMFF / Matroska) to
  tell a video from an image. With `mediaType: 'mixed'` that chooser is opened
  with type `*/*` (images and videos are only a hint, passed as
  `EXTRA_MIME_TYPES`), so it may also offer non-media files — a non-media pick
  fails to process and is dropped like any other failed item.

[Temp-file handling](temp-files.md) is identical to photos: the same
`rn-media-picker` directory, the same 24-hour sweep, and `cleanTempFiles()`
deletes video temp files too.

## Video thumbnails

`includeThumbnail: true` writes a poster frame for every **video** asset and
adds `thumbnailUri`, `thumbnailWidth` and `thumbnailHeight` to it:

```ts
const result = await launchImageLibrary({
  mediaType: 'video',
  includeThumbnail: true,
});

// <Image source={{ uri: result.assets?.[0]?.thumbnailUri }} />
```

The thumbnail is a **JPEG** taken from the first renderable frame, fitted
inside 512×512 (never scaled up), with the video's rotation metadata already
applied. Its size is not configurable — `maxWidth`/`maxHeight`/`quality`/
`format` describe the asset, not its preview. Photo assets never get one, and
neither do camera photo captures — but a video recorded with
[`launchCamera`](../README.md#camera) does, since it is a video asset like any
other.

The thumbnail is a temp file in the same `rn-media-picker` directory, so it is
covered by the 24-hour sweep and by `cleanTempFiles()`. Passing the asset to
[`releaseAssets`](temp-files.md#releasing-individual-assets) releases the
thumbnail with it.

Generating a thumbnail never fails an asset: if the frame cannot be decoded,
the asset comes back without the three `thumbnail*` fields. Always narrow
`thumbnailUri` before using it.

## Recording video

`mediaType: 'video'` opens the same system camera in movie mode and returns a
recorded movie instead of a still:

```ts
const result = await launchCamera({
  mediaType: 'video',
  maxDuration: 30,
  videoQuality: 'medium',
  includeThumbnail: true,
});
```

The result is the same shape as a video picked from the library — `duration`,
displayed `width`/`height`, `fileSize`, and `thumbnailUri` when you ask for it —
so [Video assets](#video-assets) describes it in full. This library never
transcodes the recording — it copies out what the camera wrote, which is where
`videoQuality` had its say — and `fileName` is the generated
`media_picker_<uuid>.<ext>`, since a fresh recording has no name in the gallery.
`type` is `video/quicktime` on iOS
and `video/mp4` on Android — though a camera app that ignores the output file
and hands back a content uri of its own can report a different video mime, which
is passed through exactly as a picked one would be.

The photo options do not apply to a recording and are ignored for it:
`maxWidth`, `maxHeight`, `quality`, `format`, `includeBase64`, `includeExif` and
`stripMetadata`. `videoQuality` and `maxDuration` take their place, with the
caveats in the [`CameraOptions`](../README.md#cameraoptions) table —
`cameraType` carries the same best-effort caveat on Android it always has.

`maxDuration: 0` does not mean "record until the user stops". It means the
library sets no limit of its own and leaves the platform's: a hard 10 minutes on
iOS, and on Android whatever the installed camera app decides.
