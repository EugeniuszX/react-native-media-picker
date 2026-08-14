const NativeReactNativeMediaPicker = {
  launchImageLibrary: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  launchCamera: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  cleanTempFiles: jest.fn(() => Promise.resolve(0)),
  releaseAssets: jest.fn((_uris: string[]) => Promise.resolve(0)),
};

export default NativeReactNativeMediaPicker;
