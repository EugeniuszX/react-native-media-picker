const NativeReactNativeMediaPicker = {
  launchImageLibrary: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  launchCamera: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
  cleanTempFiles: jest.fn(() => Promise.resolve()),
};

export default NativeReactNativeMediaPicker;
