const {
  AndroidConfig,
  createRunOncePlugin,
  withInfoPlist,
} = require('expo/config-plugins');

const pkg = require('./package.json');

const DEFAULT_CAMERA_PERMISSION =
  '$(PRODUCT_NAME) needs access to your camera to take photos.';

const withMediaPicker = (config, props) => {
  const { cameraPermission, enableAndroidCameraPermission = false } =
    props ?? {};

  if (cameraPermission !== false) {
    config = withInfoPlist(config, (iosConfig) => {
      iosConfig.modResults.NSCameraUsageDescription =
        cameraPermission ??
        iosConfig.modResults.NSCameraUsageDescription ??
        DEFAULT_CAMERA_PERMISSION;
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
