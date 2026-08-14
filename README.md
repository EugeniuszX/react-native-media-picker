# @eugeniuszx/react-native-media-picker

[![npm version](https://img.shields.io/npm/v/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![npm downloads](https://img.shields.io/npm/dm/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![license](https://img.shields.io/npm/l/@eugeniuszx/react-native-media-picker.svg)](./LICENSE)

Cross-platform media picker for React Native (New Architecture). Picks photos and
videos from the gallery, and captures photos with the camera. Gallery access
requires **no runtime permissions** on iOS (PHPicker) or Android (Photo Picker).

📦 **npm:** https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker

> Requires React Native 0.76+ with the New Architecture enabled.

## Install

```sh
npm install @eugeniuszx/react-native-media-picker
cd ios && pod install
```

## Usage

```ts
import { launchImageLibrary } from '@eugeniuszx/react-native-media-picker';

const result = await launchImageLibrary({
  selectionLimit: 1, // 0 = unlimited
  mediaType: 'photo', // 'photo' | 'video' | 'mixed'
  maxWidth: 640,
  maxHeight: 640,
  quality: 0.8, // 0..1
  includeBase64: false,
});

if (!result.didCancel && result.assets) {
  console.log(result.assets[0].uri);
}
```

## `LibraryOptions`

| Option | Type | Default | Notes |
|---|---|---|---|
| `selectionLimit` | `number` | `1` | `0` = unlimited (see note below) |
| `mediaType` | `'photo' \| 'video' \| 'mixed'` | `'photo'` | What the picker offers; videos are returned as-is (see [Video assets](#video-assets)) |
| `maxWidth` | `number` | `0` | `0` = no resize; ignored for video assets |
| `maxHeight` | `number` | `0` | `0` = no resize; ignored for video assets |
| `quality` | `number` | `1` | Re-encode quality 0..1 (JPEG/WebP/HEIC); ignored for lossless PNG, for video assets, and for animated images (unless an explicit `format` forces a first-frame re-encode) |
| `format` | `'original' \| 'jpeg' \| 'png'` | `'original'` | Guarantee the output file type; `'original'` preserves the source format (see below); ignored for video assets |
| `includeBase64` | `boolean` | `false` | adds `base64` to each asset; ignored for video assets |
| `includeThumbnail` | `boolean` | `false` | adds `thumbnailUri` to each **video** asset (see [Video thumbnails](#video-thumbnails)); ignored for photos |

Videos can be picked with `mediaType: 'video'` (or alongside photos with
`'mixed'`) — see [Video assets](#video-assets).

On Android, "unlimited" is capped by the system Photo Picker:
`selectionLimit: 0` requests `MediaStore.getPickImagesMaxLimit()`, and any larger
explicit limit is clamped to it. iOS has no such cap.

## `Asset`

Each picked item resolves to: `uri` (a `file://` path to a temp file), `type`
(the output mime, matching the file actually written — `image/jpeg`,
`image/png`, `image/heic`, `image/gif`, or `image/webp` for photos, and
`video/mp4`, `video/quicktime`, `video/webm` or `video/3gpp` for videos; see
[Format handling](#format-handling) and [Video assets](#video-assets) for how
it is derived), `fileName` (extension matches `type`), `fileSize`, `width`,
`height`, `duration` (seconds, video assets only — absent for photos),
`base64` (only when `includeBase64` is true, photos only), and
`thumbnailUri`/`thumbnailWidth`/`thumbnailHeight` (only when
`includeThumbnail` is true, video assets only — see
[Video thumbnails](#video-thumbnails)).

`uri` and `type` are the only fields declared non-optional. `fileName`,
`fileSize`, `width` and `height` are typed `?:`, so TypeScript makes you narrow
them. Both native modules do populate all four today, but `width` and `height`
are reported as `0` when the image or video metadata cannot be read — treat `0`
as "unknown", not as a real dimension.

`width` and `height` are the dimensions **as displayed** — the EXIF orientation
is already applied. For an image whose orientation is a quarter turn they are
therefore swapped relative to the stored pixel buffer when the original bytes are
passed through untouched; when a resize forces a re-encode the rotation is baked
into the output, so buffer and reported size agree. For videos the rotation
metadata of the video track is applied the same way — see
[Video assets](#video-assets).

### Format handling

With the default `format: 'original'` the source format is preserved:

- **No resize needed** (no `maxWidth`/`maxHeight`, or the image is already within
  bounds): the original bytes are returned unchanged. `quality` only applies when
  a resize forces a re-encode.
- **Resize needed:** the image is re-encoded in its source format — PNG stays PNG
  (transparency preserved), JPEG stays JPEG. **Exceptions:** HEIC stays HEIC on
  iOS but becomes `image/jpeg` on Android (no system HEIC encoder), and also falls
  back to `image/jpeg` on iOS Simulator / older devices without a HEIC encoder;
  WebP becomes `image/jpeg` on iOS (no system WebP encoder).
- **Animated images** (GIF, animated WebP): always returned untouched;
  `maxWidth`/`maxHeight`/`quality` are ignored so the animation survives.
- **Camera captures** are `image/jpeg` unless `format: 'png'` is requested. On iOS
  the capture arrives already decoded, so it is always re-encoded; on Android the
  capture file is passed through untouched when no resize is needed. With
  `format: 'png'` on Android the capture file is re-encoded even when no resize is
  needed.
- **Explicit `format: 'jpeg'` / `'png'`:** guarantees the output type. A source
  already in the requested format (and not animated) is passed through
  untouched — `quality` is not applied to it. Anything else is re-encoded even
  when no resize is needed. Animated images (GIF, animated WebP) are converted
  from their **first frame** — the animation is lost, and since it is lost
  anyway, `maxWidth`/`maxHeight` apply to that frame. `quality` still applies
  only when a re-encode happens, and is ignored for lossless PNG output.
  Converting a source with transparency (PNG/HEIC alpha) to `'jpeg'` composites
  it onto a black background — JPEG has no alpha channel; use `'png'` when
  transparency must survive. File types the library does not recognize (for
  example AVIF) are currently treated as JPEG, so `format: 'jpeg'` returns them
  unchanged rather than transcoding them; the guarantee covers the formats
  listed in the [`Asset`](#asset) section.

### Video assets

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
types under [Format handling](#format-handling). iOS only ever reports
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

Temp-file handling is identical to photos: the same `rn-media-picker`
directory, the same 24-hour sweep, and `cleanTempFiles()` deletes video temp
files too.

### Video thumbnails

`includeThumbnail: true` writes a poster frame for every **video** asset and
adds `thumbnailUri`, `thumbnailWidth` and `thumbnailHeight` to it:

```ts
const result = await launchImageLibrary({
  mediaType: 'video',
  includeThumbnail: true,
});

// <Image source={{ uri: result.assets?.[0].thumbnailUri }} />
```

The thumbnail is a **JPEG** taken from the first renderable frame, fitted
inside 512×512 (never scaled up), with the video's rotation metadata already
applied. Its size is not configurable — `maxWidth`/`maxHeight`/`quality`/
`format` describe the asset, not its preview. Photo assets never get one, and
neither do camera captures.

The thumbnail is a temp file in the same `rn-media-picker` directory, so it is
covered by the 24-hour sweep and by `cleanTempFiles()`. Passing the asset to
[`releaseAssets`](#releasing-individual-assets) releases the thumbnail with it.

Generating a thumbnail never fails an asset: if the frame cannot be decoded,
the asset comes back without the three `thumbnail*` fields. Always narrow
`thumbnailUri` before using it.

## Camera

```ts
import { launchCamera } from '@eugeniuszx/react-native-media-picker';

const result = await launchCamera({
  cameraType: 'back', // 'back' | 'front'
  maxWidth: 1280,
  maxHeight: 1280,
  quality: 0.8,
  includeBase64: false,
});
```

### `CameraOptions`

| Option | Type | Default | Notes |
|---|---|---|---|
| `cameraType` | `'back' \| 'front'` | `'back'` | Honored on iOS; **best-effort on Android** (the system camera app may ignore it) |
| `maxWidth` | `number` | `0` | `0` = no resize |
| `maxHeight` | `number` | `0` | `0` = no resize |
| `quality` | `number` | `1` | JPEG quality 0..1. Applies whenever the capture is re-encoded — always on iOS; on Android only when a resize is needed; ignored when `format: 'png'` (PNG is lossless) |
| `format` | `'original' \| 'jpeg' \| 'png'` | `'original'` | Output type of the capture; `'original'` = JPEG, as before |
| `includeBase64` | `boolean` | `false` | adds `base64` to the captured asset |

`launchCamera` captures still photos; there is no `mediaType` option and no
video recording. Videos come from the library picker — see
[Video assets](#video-assets).

### Camera permissions

- **iOS:** add `NSCameraUsageDescription` to your app's `Info.plist`. iOS shows the permission prompt automatically; the app crashes at launch of the camera if the key is missing. If the user denies access, `launchCamera` resolves `{ didCancel: false, errorCode: 'permission' }`.
- **Android:** if your app declares `android.permission.CAMERA` in its manifest, this library requests it at runtime before opening the camera (a denial resolves `{ didCancel: false, errorCode: 'permission' }`). If your app does **not** declare `CAMERA`, the system camera app is launched without any runtime permission.

## Response & error handling

`launchImageLibrary` and `launchCamera` **never reject** — they always resolve a `PickerResponse`:

- **Success:** `{ didCancel: false, assets: Asset[] }`
- **Cancelled:** `{ didCancel: true }`
- **Error:** `{ didCancel: false, errorCode, errorMessage }`. `errorCode` is the
  union `ErrorCode = 'permission' | 'camera_unavailable' | 'others'`, exported
  from the package:
  - `'permission'` — camera permission denied or restricted
  - `'camera_unavailable'` — the device has no camera, or (Android) no installed
    app can handle the capture intent
  - `'others'` — everything else (no activity or view controller to present from,
    load/decode/encode failure, a pick already in flight)

Check `didCancel`, then `errorCode`, then read `assets`.

When several items are picked and only some of them fail to load, the successful
ones are returned; in that case `errorCode` is set only if **every** item failed.

Only one pick may be in flight at a time. A `launchImageLibrary` or
`launchCamera` call made while another is still running resolves immediately with
`errorCode: 'others'` and `errorMessage: 'Already waiting for a pick.'` — the
running pick is left untouched.

## Temp files

Every picked or captured asset is written to a temp file inside a dedicated
`rn-media-picker` directory (in `NSTemporaryDirectory()` on iOS, in the app's
`cacheDir` on Android). `Asset.uri` points at that file.

The library sweeps files older than 24 hours when the native module initializes,
so leftovers from previous runs do not accumulate. Copy or upload what you need,
then release the rest explicitly:

```ts
import { cleanTempFiles } from '@eugeniuszx/react-native-media-picker';

await cleanTempFiles();
```

`cleanTempFiles` deletes every temp file this library has produced, so it
invalidates every `uri` handed out earlier — call it once you are done with them.
It never rejects. The returned promise resolves as soon as the sweep is
scheduled, not once the files are gone, so treat it as "these URIs are now
unusable" rather than as a completed deletion.

**Do not call it while a pick is in flight.** The sweep empties the whole
directory, including files the running pick is still using. On Android it
deletes the file the camera app is writing into, and the capture then resolves
`{ didCancel: true }` as though the user had backed out; on either platform it
can delete a just-written asset before its `uri` reaches you. Call it after the
`launchImageLibrary` / `launchCamera` promise has settled.

### Releasing individual assets

`cleanTempFiles()` is all-or-nothing. When you keep some assets and are done
with others — the user deselected one, an upload finished, a preview was
dismissed — release just those:

```ts
import { releaseAssets } from '@eugeniuszx/react-native-media-picker';

await releaseAssets(result.assets ?? []); // whole batch
await releaseAssets(asset); // one asset
await releaseAssets(asset.uri); // one uri
```

It accepts an asset, a `uri` string, or an array mixing both. Passing an asset
also releases its `thumbnailUri`, which a bare `uri` string would leave behind.
Duplicates and empty entries are dropped, and an empty list never reaches the
native module.

Only files inside the library's own `rn-media-picker` directory can be deleted:
a `uri` pointing anywhere else — or one this library did not hand out — is
ignored rather than acted on. Non-`file://` uris are ignored too.

Like `cleanTempFiles`, it never rejects, and the promise resolves once the
deletion is scheduled rather than once the files are gone. The same warning
applies: do not call it while a pick is in flight.

Upgrading from 0.2.x: temp files written by earlier versions went straight into
the cache/temp directory rather than the `rn-media-picker` subdirectory, so
neither `cleanTempFiles` nor the 24-hour sweep touches them. They are left to the
OS to reclaim.

## Permissions

Gallery picking needs **no** permissions. Camera capture may require permissions — see
[Camera permissions](#camera-permissions) above.

## Migrating from 0.2.x

- `LibraryOptions.mediaType` is back (it was removed because it was never
  read): `'photo' | 'video' | 'mixed'`, default `'photo'`. `Asset.duration`
  is populated for video assets.
- `PickerResponse.errorCode` is now the `ErrorCode` union instead of `string`.
  `ErrorCode` is exported from the package root.
- New: `cleanTempFiles()`. Assets now live in a dedicated `rn-media-picker`
  subdirectory of the temp/cache directory, and files older than 24 hours are
  swept when the native module initializes — see [Temp files](#temp-files).
- On Android, a multi-image pick where some items fail now returns the
  successful ones instead of failing the whole batch, matching iOS.
- On Android, `maxWidth`/`maxHeight` are now honoured for images whose EXIF
  orientation is a quarter turn. Previously the bounds were tested against the
  stored pixel buffer rather than the displayed axes, so such an image could skip
  the resize entirely and come back **larger than the bound you asked for** — a
  3000×4000 buffer that displays as 4000×3000 was returned untouched at 4000px
  wide under `maxWidth: 3500`. (Reported `width`/`height` were never wrong; they
  always described the file that was returned.) Those images are now re-encoded
  where they previously were not, so for them `type` can change (HEIC becomes
  `image/jpeg` on Android), `fileSize` changes, and `quality` starts applying.
- On iOS, a `launchImageLibrary` call rejected because another pick is in flight
  now reports `errorMessage: 'Already waiting for a pick.'` instead of
  `'Already waiting for an image pick.'`. Both platforms and both entry points
  now use the one string. `errorCode` is unchanged (`'others'`), so code matching
  on the old message fails silently — match on `errorCode` instead.
- On Android, the single-pick-in-flight window now extends through image
  processing instead of ending the moment the picker closes. A pick started while
  the previous batch is still decoding resolves `errorCode: 'others'` with
  `'Already waiting for a pick.'` rather than running concurrently — matching
  iOS. Chaining the next pick off the previous promise is unaffected.

## License

MIT
