# @eugeniuszx/react-native-media-picker

Cross-platform media picker for React Native (New Architecture). Gallery access
requires **no runtime permissions** on iOS (PHPicker) or Android (Photo Picker).

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
| `mediaType` | `'photo' \| 'video' \| 'mixed'` | `'photo'` | Phase 1 implements `'photo'` |
| `selectionLimit` | `number` | `1` | `0` = unlimited |
| `maxWidth` | `number` | `0` | `0` = no resize |
| `maxHeight` | `number` | `0` | `0` = no resize |
| `quality` | `number` | `1` | JPEG quality 0..1 |
| `includeBase64` | `boolean` | `false` | adds `base64` to each asset |

## `Asset`

Each picked item resolves to: `uri` (a `file://` path to a temp JPEG), `type`
(`"image/jpeg"`), `fileName`, `fileSize`, `width`, `height`, and `base64` (only when
`includeBase64` is true).

## Permissions

Gallery picking needs **no** permissions. (Camera arrives in a later release and will
require `NSCameraUsageDescription` / the `CAMERA` Android permission.)

## License

MIT
