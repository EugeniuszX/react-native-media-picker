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

On iOS, `assets` follows the order the user tapped the items in. On Android the
order is the one the system picker reports.

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

On iOS the system is willing to transcode a photo while exporting it from the
library, so a HEIC can arrive already turned into JPEG. With `format: 'original'`
the library asks for the stored representation instead, which is both faster and
what "original" promises — expect `image/heic` for HEIC photos where an earlier
version sometimes handed you `image/jpeg`. Ask for `format: 'jpeg'` when your
backend needs JPEG; the export is then left to the system, which is free to pick
whichever representation it can produce fastest.

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

`launchCamera` handles all of this on its own. Reach for the two calls below when
you want to explain yourself before the system prompt appears, or to gate a
camera button on the current status:

```ts
import {
  getCameraPermissionStatus,
  requestCameraPermission,
} from '@eugeniuszx/react-native-media-picker';

if ((await getCameraPermissionStatus()) === 'blocked') {
  // Only Settings can undo this — send the user there instead of asking again.
} else {
  const status = await requestCameraPermission();
}
```

`getCameraPermissionStatus` never prompts. `requestCameraPermission` shows the
system prompt only when it can still be answered (`'not_determined'` or
`'denied'`) and resolves the status the user leaves it in; every other status is
returned as-is, without a dialog. Neither call rejects, and neither is needed to
use `launchCamera`.

`CameraPermissionStatus` is exported from the package:

| Status | Meaning |
|---|---|
| `'granted'` | Capture is allowed |
| `'not_determined'` | Never asked — asking will show the prompt |
| `'denied'` | Refused, but asking again still shows the prompt. **Android only** |
| `'blocked'` | Refused for good (or restricted by policy); only Settings can change it. iOS reports every refusal here, since it allows one ask per install |
| `'not_required'` | The app does not declare `android.permission.CAMERA`, so no runtime permission is involved. **Android only** |
| `'unavailable'` | The device has no camera |

Two details worth knowing on Android: distinguishing `'denied'` from `'blocked'`
relies on this library remembering that it has asked, so a status read after the
app's data is cleared reports `'not_determined'` again; and a status read while no
activity is in the foreground reports `'blocked'` rather than `'denied'` for a
refused permission, because the rationale flag cannot be read from the background.
`'unavailable'` covers missing camera hardware — a device that has a camera but no
app able to handle the capture intent still reports its real permission status
here and surfaces as `errorCode: 'camera_unavailable'` from `launchCamera`.

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
It never rejects, and resolves once the sweep has finished with the number of
files it deleted.

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
native module — it resolves `0` right away.

Only files inside the library's own `rn-media-picker` directory can be deleted:
a `uri` pointing anywhere else — or one this library did not hand out — is
ignored rather than acted on. Non-`file://` uris are ignored too.

Like `cleanTempFiles`, it never rejects, and resolves with the number of files
deleted once they are gone. A `uri` it declines to touch simply is not counted.
The same warning applies: do not call it while a pick is in flight.

Upgrading from 0.2.x: temp files written by earlier versions went straight into
the cache/temp directory rather than the `rn-media-picker` subdirectory, so
neither `cleanTempFiles` nor the 24-hour sweep touches them. They are left to the
OS to reclaim.

## Permissions

Gallery picking needs **no** permissions. Camera capture may require permissions — see
[Camera permissions](#camera-permissions) above.

## Expo

The library is not available in Expo Go (it ships native code), but it works in
any project using `expo prebuild` / a development build. A config plugin is
included so you do not have to edit `Info.plist` by hand:

```json
{
  "expo": {
    "plugins": [
      [
        "@eugeniuszx/react-native-media-picker",
        {
          "cameraPermission": "Let $(PRODUCT_NAME) take photos for your profile."
        }
      ]
    ]
  }
}
```

| Prop | Type | Default | Effect |
|---|---|---|---|
| `cameraPermission` | `string \| false` | a generic sentence | Text for `NSCameraUsageDescription`. `false` leaves `Info.plist` alone — use it if you only pick from the gallery, or set the key yourself |
| `enableAndroidCameraPermission` | `boolean` | `false` | Adds `android.permission.CAMERA` to the manifest |

Leave `enableAndroidCameraPermission` off unless you need the permission for
something else: declaring `CAMERA` is what makes this library ask for it at
runtime, and without it the system camera app needs no permission at all — see
[Camera permissions](#camera-permissions).

The plugin adds no runtime dependency; it resolves `expo/config-plugins` from
your app, and non-Expo projects never load it.

## Testing

Jest cannot load a TurboModule, so mock the package. A ready-made mock ships
with it:

```ts
jest.mock('@eugeniuszx/react-native-media-picker', () =>
  require('@eugeniuszx/react-native-media-picker/jest/mock')
);
```

Every entry point becomes a `jest.fn()`: `launchImageLibrary` and `launchCamera`
resolve `{ didCancel: true }`, `cleanTempFiles` and `releaseAssets` resolve `0`,
and `getCameraPermissionStatus`/`requestCameraPermission` resolve `'granted'`. So
stage a result per test:

```ts
import { launchImageLibrary } from '@eugeniuszx/react-native-media-picker';

(launchImageLibrary as jest.Mock).mockResolvedValueOnce({
  didCancel: false,
  assets: [{ uri: 'file:///tmp/a.jpg', type: 'image/jpeg' }],
});
```

The mock covers the entry points only — the pure option helpers
(`normalizeLibraryOptions`, `collectReleasableUris`) are not re-exported,
since they need no mocking.

## Upgrading from 1.3.x

Everything new is additive, but four behaviours changed. None of them alters a
type, so TypeScript will not point them out for you.

- **`Asset.fileName` is now the name from the gallery**, not the temp file's
  generated name — `IMG_4821.jpg` instead of `media_picker_<uuid>.jpg`. The
  extension still matches `type`, and `uri` is unchanged. Code that reconstructed
  a path from `fileName` was already relying on something it should not; use
  `uri`. Camera captures keep the generated name.
- **On iOS, picked assets now come back in tap order** rather than in library
  order. If you were re-sorting them yourself to get that, you can stop.
- **On iOS with `format: 'original'`, HEIC photos now stay HEIC more often.**
  Earlier the system was free to hand over a JPEG it transcoded itself; the
  library now asks for the stored representation. Pass `format: 'jpeg'` if you
  need JPEG — see [Format handling](#format-handling).
- **`cleanTempFiles()` and `releaseAssets()` resolve `Promise<number>`** — the
  number of files deleted — and only once the files are actually gone, instead of
  as soon as the sweep was scheduled. Awaiting them is now meaningful; code that
  ignored the result keeps working.

New: [`getCameraPermissionStatus()` and `requestCameraPermission()`](#camera-permissions).
`peerDependencies` now state the versions the library has always needed
(`react-native >= 0.76`, `react >= 18.2`), so a mismatched install warns instead
of failing at runtime.

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
