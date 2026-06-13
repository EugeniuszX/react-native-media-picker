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
| `mediaType` | `'photo' \| 'video' \| 'mixed'` | `'photo'` | Phase 1 implements `'photo'` only; `'video'`/`'mixed'` currently behave as `'photo'` |
| `selectionLimit` | `number` | `1` | `0` = unlimited |
| `maxWidth` | `number` | `0` | `0` = no resize |
| `maxHeight` | `number` | `0` | `0` = no resize |
| `quality` | `number` | `1` | Re-encode quality 0..1 (JPEG/WebP; ignored for lossless PNG) |
| `includeBase64` | `boolean` | `false` | adds `base64` to each asset |

## `Asset`

Each picked item resolves to: `uri` (a `file://` path to a temp file), `type`
(the source mime — `image/jpeg`, `image/png`, `image/heic`, `image/gif`, or
`image/webp`), `fileName` (extension matches `type`), `fileSize`, `width`,
`height`, and `base64` (only when `includeBase64` is true).

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
- **Camera captures** are always `image/jpeg`.

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
| `quality` | `number` | `1` | Re-encode quality 0..1 (JPEG/WebP; ignored for lossless PNG) |
| `includeBase64` | `boolean` | `false` | adds `base64` to the captured asset |

### Camera permissions

- **iOS:** add `NSCameraUsageDescription` to your app's `Info.plist`. iOS shows the permission prompt automatically; the app crashes at launch of the camera if the key is missing. If the user denies access, `launchCamera` resolves `{ didCancel: false, errorCode: 'permission' }`.
- **Android:** if your app declares `android.permission.CAMERA` in its manifest, this library requests it at runtime before opening the camera (a denial resolves `{ didCancel: false, errorCode: 'permission' }`). If your app does **not** declare `CAMERA`, the system camera app is launched without any runtime permission.

## Response & error handling

`launchImageLibrary` and `launchCamera` **never reject** — they always resolve a `PickerResponse`:

- **Success:** `{ didCancel: false, assets: Asset[] }`
- **Cancelled:** `{ didCancel: true }`
- **Error:** `{ didCancel: false, errorCode, errorMessage }` — `errorCode` is `'permission'` (camera permission denied), `'camera_unavailable'` (no camera / source unavailable), or `'others'`.

Check `didCancel`, then `errorCode`, then read `assets`.

## Permissions

Gallery picking needs **no** permissions. Camera capture may require permissions — see
[Camera permissions](#camera-permissions) above.

## License

MIT
