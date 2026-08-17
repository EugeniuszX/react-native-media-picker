[![react-native-media-picker](https://raw.githubusercontent.com/EugeniuszX/react-native-media-picker/main/docs/static/banner.png)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)

Pick photos and videos in React Native — from the gallery **without asking for a single runtime permission**, or straight from the camera. Built as a TurboModule for the New Architecture.

[![npm version](https://img.shields.io/npm/v/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![npm downloads](https://img.shields.io/npm/dm/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![license](https://img.shields.io/npm/l/@eugeniuszx/react-native-media-picker.svg)](./LICENSE)
![platforms](https://img.shields.io/badge/platforms-iOS%20%7C%20Android-lightgrey)
![New Architecture](https://img.shields.io/badge/New%20Architecture-required-F0A93B)

## 🔍 Features

| Feature                                                                                      | Status       |
| -------------------------------------------------------------------------------------------- | ------------ |
| 🔓 Gallery picking with **no runtime permission** — PHPicker on iOS, Photo Picker on Android | ✅ Available |
| 🖼️ Photos, 🎬 videos, or both in one pick (`mediaType: 'photo' \| 'video' \| 'mixed'`)       | ✅ Available |
| 📷 Camera capture and video recording ([`launchCamera`](docs/api.md#launchcameraoptions))    | ✅ Available |
| 📐 Resize and re-encode on the native side (`maxWidth` / `maxHeight` / `quality`)            | ✅ Available |
| 🗂️ Guaranteed output format — `original`, `jpeg` or `png` ([details](docs/formats.md))       | ✅ Available |
| 🎞️ Poster frame per video asset (`includeThumbnail`)                                         | ✅ Available |
| 🧭 Read EXIF, or **strip EXIF/GPS** from what you write ([details](docs/metadata.md))        | ✅ Available |
| 🧹 Temp-file lifecycle API — `cleanTempFiles()`, `releaseAssets()`, 24-hour sweep            | ✅ Available |
| 🧩 Expo config plugin, no manual `Info.plist` edits ([details](docs/expo.md))                | ✅ Available |
| 🧪 Jest mock shipped with the package ([details](docs/testing.md))                           | ✅ Available |
| 🛡️ Fully typed, discriminated-union responses — the API **never rejects**                    | ✅ Available |

## ⚖️ Comparison

|                                      | @eugeniuszx/react-native-media-picker                | react-native-image-picker                                                                | expo-image-picker                                                                                                                                   |
| ------------------------------------ | ---------------------------------------------------- | ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Gallery runtime permission           | None (PHPicker / Photo Picker)                       | None (PHPicker / Photo Picker), but `includeExtra` (EXIF) "requires library permissions" | None to launch, but its config plugin adds `NSPhotoLibraryUsageDescription` and Android `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE` by default |
| New Architecture                     | Required — TurboModule only, RN 0.76+                | Supported, with a legacy-architecture fallback                                           | Supported (Expo SDK 53+; SDK 55+ is New Architecture only)                                                                                          |
| Video poster frame                   | `includeThumbnail` writes one per video asset        | No                                                                                       | No                                                                                                                                                  |
| Temp-file lifecycle API              | `cleanTempFiles()`, `releaseAssets()`, 24-hour sweep | No such API                                                                              | No such API                                                                                                                                         |
| Strip EXIF/GPS from the written file | `stripMetadata`                                      | No — reads EXIF (`includeExtra`) only                                                    | No — reads EXIF (`exif`) only                                                                                                                       |
| Guaranteed output format             | `format: 'original' \| 'jpeg' \| 'png'`              | No such option                                                                           | No such option                                                                                                                                      |

Checked against react-native-image-picker 8.2.1 and expo-image-picker 57.0.10.

---

## 🚀 Quick Start

### Requirements

React Native **0.76+** with the New Architecture enabled. There is no legacy-architecture fallback.

### Install

```sh
npm install @eugeniuszx/react-native-media-picker
cd ios && pod install
```

Expo projects need a development build — see [Expo](docs/expo.md).

### Pick from the gallery

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

if (result.didCancel) return;
if (result.errorCode) {
  console.warn(result.errorCode, result.errorMessage);
  return;
}
console.log(result.assets[0]?.uri); // `assets` is `Asset[]` here, not `Asset[] | undefined`
```

No permission request, no `Info.plist` key — the system picker runs out of process and hands back only what the user chose.

### Capture with the camera

```ts
import { launchCamera } from '@eugeniuszx/react-native-media-picker';

const result = await launchCamera({
  cameraType: 'back', // 'back' | 'front'
  maxWidth: 1280,
  maxHeight: 1280,
  quality: 0.8,
});
```

Camera capture _does_ need an `Info.plist` key on iOS and may need a runtime permission on Android — see [Camera permissions](docs/permissions.md#camera-permissions).

---

## 📚 Documentation

- 📖 [**API reference**](docs/api.md) — every option, the `Asset` shape, and how responses narrow.
- 🎬 [Video](docs/video.md) — video assets, poster frames, and recording with the camera.
- 🗂️ [Format handling](docs/formats.md) — which file type comes back, and when the bytes are passed through untouched.
- 🧭 [Metadata](docs/metadata.md) — `includeExif` and `stripMetadata`: what is read, what is removed, and what each costs.
- 🔐 [Permissions](docs/permissions.md) — gallery needs none; the camera keys, the runtime request, and the status API.
- 🧹 [Temp files](docs/temp-files.md) — where assets live, `cleanTempFiles()` and `releaseAssets()`.
- 🧩 [Expo](docs/expo.md) — development builds and the bundled config plugin.
- 🧪 [Testing](docs/testing.md) — the Jest mock that ships with the package.
- ⬆️ [Migration](docs/migration.md) — upgrading from 1.4.x, 1.3.x and 0.2.x.
- 📦 [Example app](example) — a runnable project exercising the whole API.

## 🤝 Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, and the [Code of Conduct](CODE_OF_CONDUCT.md).

- 🐛 [Report a bug](https://github.com/EugeniuszX/react-native-media-picker/issues)
- 📝 [Changelog](CHANGELOG.md)

## 📄 License

MIT © [Eugene Masyuk](https://github.com/EugeniuszX)
