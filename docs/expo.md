# Expo

Using the library in an Expo project, and what the bundled config plugin writes
for you.

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
| `microphonePermission` | `string \| false` | a generic sentence | Text for `NSMicrophoneUsageDescription`, required to record video on iOS — see [Camera permissions](permissions.md#camera-permissions). `false` leaves `Info.plist` alone |
| `enableAndroidCameraPermission` | `boolean` | `false` | Adds `android.permission.CAMERA` to the manifest |

Both permission keys are written by default, since a missing
`NSMicrophoneUsageDescription` terminates the app when the camera opens in video
mode. Pass `false` for either one to keep it out of `Info.plist`.

Leave `enableAndroidCameraPermission` off unless you need the permission for
something else: declaring `CAMERA` is what makes this library ask for it at
runtime, and without it the system camera app needs no permission at all — see
[Camera permissions](permissions.md#camera-permissions).

The plugin adds no runtime dependency; it resolves `expo/config-plugins` from
your app, and non-Expo projects never load it.
