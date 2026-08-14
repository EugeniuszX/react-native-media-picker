const NativeReactNativeMediaPicker = {
  launchImageLibrary: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  launchCamera: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  cleanTempFiles: jest.fn(() => Promise.resolve(0)),
  releaseAssets: jest.fn((_uris: string[]) => Promise.resolve(0)),
  getCameraPermissionStatus: jest.fn(() => Promise.resolve('granted')),
  requestCameraPermission: jest.fn(() => Promise.resolve('granted')),
};

export default NativeReactNativeMediaPicker;
