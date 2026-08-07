# @eugeniuszx/react-native-media-picker

[![npm version](https://img.shields.io/npm/v/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![npm downloads](https://img.shields.io/npm/dm/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![license](https://img.shields.io/npm/l/@eugeniuszx/react-native-media-picker.svg)](./LICENSE)

Cross-platform media picker for React Native (New Architecture). Gallery access
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
| `maxWidth` | `number` | `0` | `0` = no resize |
| `maxHeight` | `number` | `0` | `0` = no resize |
| `quality` | `number` | `1` | Re-encode quality 0..1 (JPEG/WebP/HEIC); ignored for lossless PNG and for animated images |
| `includeBase64` | `boolean` | `false` | adds `base64` to each asset |

Only photos are picked. Video support is not implemented yet.

On Android, "unlimited" is capped by the system Photo Picker:
`selectionLimit: 0` requests `MediaStore.getPickImagesMaxLimit()`, and any larger
explicit limit is clamped to it. iOS has no such cap.

## `Asset`

Each picked item resolves to: `uri` (a `file://` path to a temp file), `type`
(the source mime — `image/jpeg`, `image/png`, `image/heic`, `image/gif`, or
`image/webp`), `fileName` (extension matches `type`), `fileSize`, `width`,
`height`, and `base64` (only when `includeBase64` is true).

`uri` and `type` are the only fields declared non-optional. `fileName`,
`fileSize`, `width` and `height` are typed `?:`, so TypeScript makes you narrow
them. Both native modules do populate all four today, but `width` and `height`
are reported as `0` when the image metadata cannot be read — treat `0` as
"unknown", not as a real dimension.

`width` and `height` are the dimensions **as displayed** — the EXIF orientation
is already applied. For an image whose orientation is a quarter turn they are
therefore swapped relative to the stored pixel buffer when the original bytes are
passed through untouched; when a resize forces a re-encode the rotation is baked
into the output, so buffer and reported size agree.

### Format handling

The original format is preserved:

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
- **Camera captures** are always `image/jpeg`. On iOS the capture arrives already
  decoded, so it is always re-encoded; on Android the capture file is passed
  through untouched when no resize is needed.

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
| `quality` | `number` | `1` | JPEG quality 0..1. Applies whenever the capture is re-encoded — always on iOS; on Android only when a resize is needed |
| `includeBase64` | `boolean` | `false` | adds `base64` to the captured asset |

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

When several images are picked and only some of them fail to load, the successful
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

Upgrading from 0.2.x: temp files written by earlier versions went straight into
the cache/temp directory rather than the `rn-media-picker` subdirectory, so
neither `cleanTempFiles` nor the 24-hour sweep touches them. They are left to the
OS to reclaim.

## Permissions

Gallery picking needs **no** permissions. Camera capture may require permissions — see
[Camera permissions](#camera-permissions) above.

## Migrating from 0.2.x

- `LibraryOptions.mediaType` is removed. It was never read by either native
  module — every pick was a photo pick regardless of the value. It will return
  when video support lands.
- `Asset.duration` is removed. It was never populated.
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
