const NativeReactNativeMediaPicker = {
  launchImageLibrary: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  launchCamera: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  cleanTempFiles: jest.fn(() => Promise.resolve()),
  releaseAssets: jest.fn((_uris: string[]) => Promise.resolve()),
};

export default NativeReactNativeMediaPicker;
