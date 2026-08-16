# @eugeniuszx/react-native-media-picker

[![npm version](https://img.shields.io/npm/v/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![npm downloads](https://img.shields.io/npm/dm/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![license](https://img.shields.io/npm/l/@eugeniuszx/react-native-media-picker.svg)](./LICENSE)

Cross-platform media picker for React Native (New Architecture). Picks photos and
videos from the gallery, and captures photos or records video with the camera.
Gallery access requires **no runtime permissions** on iOS (PHPicker) or Android
(Photo Picker).

📦 **npm:** https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker

## Comparison

| | this library | react-native-image-picker | expo-image-picker |
|---|---|---|---|
| Gallery runtime permission | None (PHPicker / Photo Picker) | None (PHPicker / Photo Picker), but `includeExtra` (EXIF) "requires library permissions" | None to launch, but its config plugin adds `NSPhotoLibraryUsageDescription` and Android `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE` by default |
| New Architecture | Required — TurboModule only, RN 0.76+ | Supported, with a legacy-architecture fallback | Supported (Expo SDK 53+; SDK 55+ is New Architecture only) |
| Video poster frame | `includeThumbnail` writes one per video asset | No | No |
| Temp-file lifecycle API | `cleanTempFiles()`, `releaseAssets()`, 24-hour sweep | No such API | No such API |
| Strip EXIF/GPS from the written file | `stripMetadata` | No — reads EXIF (`includeExtra`) only | No — reads EXIF (`exif`) only |
| Guaranteed output format | `format: 'original' \| 'jpeg' \| 'png'` | No such option | No such option |

Checked against react-native-image-picker 8.2.1 and expo-image-picker 57.0.10.

## Requirements

React Native 0.76+ with the New Architecture enabled.

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

if (!result.didCancel && !result.errorCode) {
  // `assets` is `Asset[]` here, not `Asset[] | undefined`.
  console.log(result.assets[0]?.uri);
}
```

## `LibraryOptions`

| Option | Type | Default | Notes |
|---|---|---|---|
| `selectionLimit` | `number` | `1` | `0` = unlimited (see note below) |
| `mediaType` | `'photo' \| 'video' \| 'mixed'` | `'photo'` | What the picker offers; videos are returned as-is (see [Video assets](docs/video.md#video-assets)) |
| `maxWidth` | `number` | `0` | `0` = no resize; ignored for video assets |
| `maxHeight` | `number` | `0` | `0` = no resize; ignored for video assets |
| `quality` | `number` | `1` | Re-encode quality 0..1 (JPEG/WebP/HEIC); ignored for lossless PNG, for video assets, and for animated images (unless an explicit `format` forces a first-frame re-encode) |
| `format` | `'original' \| 'jpeg' \| 'png'` | `'original'` | Guarantee the output file type; `'original'` preserves the source format (see [Format handling](docs/formats.md)); ignored for video assets |
| `includeBase64` | `boolean` | `false` | adds `base64` to each asset; ignored for video assets |
| `includeThumbnail` | `boolean` | `false` | adds `thumbnailUri` to each **video** asset (see [Video thumbnails](docs/video.md#video-thumbnails)); ignored for photos |
| `includeExif` | `boolean` | `false` | adds `exif` to each **photo** asset, read from the source (see [Metadata](docs/metadata.md)); ignored for video assets |
| `stripMetadata` | `boolean` | `false` | removes EXIF/GPS from each **photo** written (see [Metadata](docs/metadata.md)); ignored for video assets |

Videos can be picked with `mediaType: 'video'` (or alongside photos with
`'mixed'`) — see [Video assets](docs/video.md#video-assets).

On Android, "unlimited" is capped by the system Photo Picker:
`selectionLimit: 0` requests `MediaStore.getPickImagesMaxLimit()`, and any larger
explicit limit is clamped to it. iOS has no such cap.

On iOS, `assets` follows the order the user tapped the items in. On Android the
order is the one the system picker reports.

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

Camera capture needs an `Info.plist` key on iOS and may need a runtime
permission on Android — see [Camera permissions](docs/permissions.md#camera-permissions).

### `CameraOptions`

| Option | Type | Default | Notes |
|---|---|---|---|
| `cameraType` | `'back' \| 'front'` | `'back'` | Honored on iOS; **best-effort on Android** (the system camera app may ignore it) |
| `mediaType` | `'photo' \| 'video'` | `'photo'` | `'video'` records a movie instead of capturing a still (see [Recording video](docs/video.md#recording-video)) |
| `maxWidth` | `number` | `0` | `0` = no resize |
| `maxHeight` | `number` | `0` | `0` = no resize |
| `quality` | `number` | `1` | JPEG quality 0..1. Applies whenever the capture is re-encoded — always on iOS; on Android only when a resize is needed; ignored when `format: 'png'` (PNG is lossless) |
| `format` | `'original' \| 'jpeg' \| 'png'` | `'original'` | Output type of the capture; `'original'` = JPEG, as before |
| `includeBase64` | `boolean` | `false` | adds `base64` to the captured asset |
| `includeExif` | `boolean` | `false` | photos only (see [Metadata](docs/metadata.md)) |
| `stripMetadata` | `boolean` | `false` | photos only; **a no-op on iOS**, where a capture is always re-encoded and therefore already carries no metadata |
| `maxDuration` | `number` | `0` | Recording limit in seconds. `0` leaves the **platform's own** limit, which is 10 minutes on iOS and whatever the camera app defaults to on Android — not "unlimited". **Best-effort on Android** |
| `videoQuality` | `'low' \| 'medium' \| 'high'` | `'high'` | Honored on iOS; **best-effort on Android** (`EXTRA_VIDEO_QUALITY` carries only low/high, so `'medium'` is sent as high, and most camera apps ignore it anyway) |
| `includeThumbnail` | `boolean` | `false` | adds `thumbnailUri` to a recorded **video** (see [Video thumbnails](docs/video.md#video-thumbnails)); ignored for photos |

## `Asset`

Each picked item resolves to:

| Field | Type | Present | Notes |
|---|---|---|---|
| `uri` | `string` | always | A `file://` path to a temp file (see [Temp files](docs/temp-files.md)) |
| `type` | `string` | always | The output mime, matching the file actually written — `image/jpeg`, `image/png`, `image/heic`, `image/gif`, or `image/webp` for photos, and `video/mp4`, `video/quicktime`, `video/webm` or `video/3gpp` for videos; see [Format handling](docs/formats.md) and [Video assets](docs/video.md#video-assets) for how it is derived |
| `fileName` | `string` | typed `?:` | Extension matches `type` (see below) |
| `fileSize` | `number` | typed `?:` | Size of the file at `uri` |
| `width` / `height` | `number` | typed `?:` | Dimensions **as displayed** (see below) |
| `duration` | `number` | video assets only | Seconds — absent for photos |
| `base64` | `string` | `includeBase64`, photos only | |
| `thumbnailUri` / `thumbnailWidth` / `thumbnailHeight` | `string` / `number` | `includeThumbnail`, video assets only | See [Video thumbnails](docs/video.md#video-thumbnails) |
| `exif` | `object` | `includeExif`, photos only | See [Metadata](docs/metadata.md) |

`uri` and `type` are the only fields declared non-optional. `fileName`,
`fileSize`, `width` and `height` are typed `?:`, so TypeScript makes you narrow
them. Both native modules do populate all four today, but `width` and `height`
are reported as `0` when the image or video metadata cannot be read — treat `0`
as "unknown", not as a real dimension.

`fileName` is the name the item carries in the gallery, carrying the extension of
the file actually written — `IMG_4821.HEIC` picked with `format: 'jpeg'` comes
back as `IMG_4821.jpg`. It is a label for uploads and UI, never a path: the bytes
always live at `uri`, under a generated name. It arrives sanitized (path
separators, control characters and leading dots are dropped, and the name is
capped at 100 characters plus the extension), and when nothing usable survives —
or the system reports no name at all, which is always the case for **camera
captures** — it falls back to the temp file's own `media_picker_<uuid>.<ext>`.
Two picked items can carry the same `fileName`, so use `uri` as the identity.

`width` and `height` are the dimensions **as displayed** — the EXIF orientation
is already applied. For an image whose orientation is a quarter turn they are
therefore swapped relative to the stored pixel buffer when the original bytes are
passed through untouched; when a resize forces a re-encode the rotation is baked
into the output, so buffer and reported size agree. For videos the rotation
metadata of the video track is applied the same way — see
[Video assets](docs/video.md#video-assets).

## Response & error handling

`launchImageLibrary` and `launchCamera` **never reject** — they always resolve a `PickerResponse`:

- **Success:** `{ didCancel: false, assets: Asset[] }`
- **Cancelled:** `{ didCancel: true }`
- **Error:** `{ didCancel: false, errorCode, errorMessage }`. `errorCode` is the
  union
  `ErrorCode = 'permission' | 'camera_unavailable' | 'busy' | 'others'`,
  exported from the package:
  - `'permission'` — camera permission denied or restricted
  - `'camera_unavailable'` — the device has no camera; or (iOS) it has one that
    cannot record movies and `mediaType: 'video'` was asked for, which is what
    the simulator reports; or (Android) no installed app can handle the capture
    intent
  - `'busy'` — **another pick from this library is already in flight**, and
    nothing else
  - `'others'` — everything else (no activity or view controller to present from,
    load/decode/encode failure)

`PickerResponse` is a discriminated union, so those three cases narrow: check
`didCancel`, then `errorCode`, and `assets` is `Asset[]` on what is left.

```ts
const result = await launchImageLibrary();

if (result.didCancel) return;
if (result.errorCode) {
  console.warn(result.errorCode, result.errorMessage);
  return;
}
console.log(result.assets.length); // Asset[], no `?.` needed
```

Reading `result.assets` or `result.errorMessage` on an un-narrowed value still
compiles exactly as it did before the union.

When several items are picked and only some of them fail to load, the successful
ones are returned; in that case `errorCode` is set only if **every** item failed.

Only one pick may be in flight at a time. A `launchImageLibrary` or
`launchCamera` call made while another is still running resolves immediately with
`errorCode: 'busy'` and `errorMessage: 'Already waiting for a pick.'` — the
running pick is left untouched. A double-tapped button is the usual way to see
it, and it is the code to match on when you want to ignore that quietly rather
than surface an error.

`'busy'` is narrow on purpose: it means *this library's own pick is already
running*, not "the picker is unavailable right now". Presenting on top of a
modal your app put up fails a separate check and still resolves `'others'`
(`'A view controller is already being presented'` on iOS), because that is your
UI in the way, not our pick.

## Documentation

- [Metadata](docs/metadata.md) — `includeExif` and `stripMetadata`: what is read, what is removed, and what each costs.
- [Video](docs/video.md) — video assets, poster frames, and recording with the camera.
- [Format handling](docs/formats.md) — which file type comes back, and when the bytes are passed through untouched.
- [Permissions](docs/permissions.md) — gallery needs none; the camera keys, the runtime request, and the status API.
- [Temp files](docs/temp-files.md) — where assets live, `cleanTempFiles()` and `releaseAssets()`.
- [Expo](docs/expo.md) — development builds and the bundled config plugin.
- [Testing](docs/testing.md) — the Jest mock that ships with the package.
- [Migration](docs/migration.md) — upgrading from 1.4.x, 1.3.x and 0.2.x.

## License

MIT
