# Migration

What changed in each release that can break code, and what is merely new.

## Upgrading from 1.4.x

Everything new is opt-in, and no runtime behaviour changed for code that does
not ask for it. Two things can break a build, both in the type layer.

- **`PickerResponse` is now a discriminated union.** Reading `assets`,
  `errorCode` or `errorMessage` on an un-narrowed value compiles exactly as it
  did, so code that only *consumes* a response is unaffected; the gain is that
  `if (didCancel) … if (errorCode) …` now narrows `assets` to `Asset[]` instead
  of `Asset[] | undefined`.

  **Code that *constructs* the type is the half that breaks** — a typed test
  mock, a fixture in your own test utilities, a helper declared to return
  `PickerResponse`. `{ didCancel: false }` with no `assets` and no `errorCode`
  matches no member of the union and no longer compiles. Say which case you
  meant:

  ```ts
  import type { Asset, PickerResponse } from '@eugeniuszx/react-native-media-picker';

  const cancelled: PickerResponse = { didCancel: true };
  const picked = (assets: Asset[]): PickerResponse => ({ didCancel: false, assets });
  const failed: PickerResponse = {
    didCancel: false,
    errorCode: 'busy',
    errorMessage: 'Already waiting for a pick.',
  };
  ```

  The [Jest mock](testing.md) that ships with the package is untyped, so
  `mockResolvedValueOnce` on it is unaffected either way.

- **`ErrorCode` gained `'busy'`**, returned where `'others'` used to be for
  `'Already waiting for a pick.'`. The message itself is unchanged, and no other
  code moved. An exhaustive `switch` over `ErrorCode` needs a new arm — the one
  break the compiler will point at.

  Less visibly, **code comparing `errorCode === 'others'` as a catch-all** — the
  "something went wrong" branch, the fallback toast — stops matching the
  double-tap case, which was probably the most common thing reaching it.
  TypeScript says nothing, because that comparison is still valid. Handle
  `'busy'` deliberately: see
  [Response & error handling](../README.md#response--error-handling)
  for what it covers and, just as importantly, what it does not.

New, all opt-in:

- `mediaType: 'video'` for `launchCamera`, with `maxDuration`, `videoQuality`
  and `includeThumbnail` — see [Recording video](video.md#recording-video).
- `includeExif` and `stripMetadata` for photos — see [Metadata](metadata.md).

One thing to add before you ship the first of those: **recording video on iOS
requires `NSMicrophoneUsageDescription` in `Info.plist`**, or the app is
terminated the moment the camera opens. The Expo config plugin writes the key
for you now; a bare React Native app has to add it by hand. See
[Camera permissions](permissions.md#camera-permissions).

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
  need JPEG — see [Format handling](formats.md).
- **`cleanTempFiles()` and `releaseAssets()` resolve `Promise<number>`** — the
  number of files deleted — and only once the files are actually gone, instead of
  as soon as the sweep was scheduled. Awaiting them is now meaningful; code that
  ignored the result keeps working.

New: [`getCameraPermissionStatus()` and `requestCameraPermission()`](permissions.md#camera-permissions).
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
  swept when the native module initializes — see [Temp files](temp-files.md).
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
  now use the one string, so code matching on the old message fails silently —
  match on `errorCode` instead, which is `'busy'` as of 1.5.0 and was `'others'`
  before it.
- On Android, the single-pick-in-flight window now extends through image
  processing instead of ending the moment the picker closes. A pick started while
  the previous batch is still decoding resolves `errorCode: 'busy'` with
  `'Already waiting for a pick.'` rather than running concurrently — matching
  iOS. Chaining the next pick off the previous promise is unaffected.
