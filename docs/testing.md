# Testing

Mocking the package in Jest.

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
