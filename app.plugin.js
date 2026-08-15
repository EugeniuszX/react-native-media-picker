const {
  AndroidConfig,
  createRunOncePlugin,
  withInfoPlist,
} = require('expo/config-plugins');

const pkg = require('./package.json');

const DEFAULT_CAMERA_PERMISSION =
  '$(PRODUCT_NAME) needs access to your camera to take photos.';

const DEFAULT_MICROPHONE_PERMISSION =
  '$(PRODUCT_NAME) needs access to your microphone to record video.';

const withMediaPicker = (config, props) => {
  const {
    cameraPermission,
    microphonePermission,
    enableAndroidCameraPermission = false,
  } = props ?? {};

  if (cameraPermission !== false) {
    config = withInfoPlist(config, (iosConfig) => {
      iosConfig.modResults.NSCameraUsageDescription =
        cameraPermission ??
        iosConfig.modResults.NSCameraUsageDescription ??
        DEFAULT_CAMERA_PERMISSION;
      return iosConfig;
    });
  }

  // Recording video with UIImagePickerController captures audio, so a missing
  // NSMicrophoneUsageDescription crashes the app the moment the camera opens.
  if (microphonePermission !== false) {
    config = withInfoPlist(config, (iosConfig) => {
      iosConfig.modResults.NSMicrophoneUsageDescription =
        microphonePermission ??
        iosConfig.modResults.NSMicrophoneUsageDescription ??
        DEFAULT_MICROPHONE_PERMISSION;
      return iosConfig;
    });
  }

  if (enableAndroidCameraPermission) {
    config = AndroidConfig.Permissions.withPermissions(config, [
      'android.permission.CAMERA',
    ]);
  }

  return config;
};

module.exports = createRunOncePlugin(withMediaPicker, pkg.name, pkg.version);
