# Permissions

Gallery picking needs **no** permissions. Camera capture may require
permissions — see [Camera permissions](#camera-permissions) below.

## Camera permissions

- **iOS:** add `NSCameraUsageDescription` to your app's `Info.plist`. iOS shows the permission prompt automatically; the app crashes at launch of the camera if the key is missing. If the user denies access, `launchCamera` resolves `{ didCancel: false, errorCode: 'permission' }`.
- **Android:** if your app declares `android.permission.CAMERA` in its manifest, this library requests it at runtime before opening the camera (a denial resolves `{ didCancel: false, errorCode: 'permission' }`). If your app does **not** declare `CAMERA`, the system camera app is launched without any runtime permission.

> **Recording video on iOS also needs `NSMicrophoneUsageDescription`.** The
> system camera controller starts audio capture as soon as it opens in movie
> mode, and iOS terminates the process when the key is missing — exactly as it
> does without `NSCameraUsageDescription`, and before any code of yours runs.
> Add both keys if you use `mediaType: 'video'`, or let the
> [config plugin](expo.md) do it.

Android needs no equivalent. `ACTION_VIDEO_CAPTURE` hands the recording to a
separate camera app, which holds its own microphone permission; this library
never records audio in your process and never asks for `RECORD_AUDIO`.

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
