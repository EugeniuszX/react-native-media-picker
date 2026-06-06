const NativeReactNativeMediaPicker = {
  launchImageLibrary: jest.fn(() =>
    Promise.resolve({ didCancel: false, assets: [] })
  ),
};

export default NativeReactNativeMediaPicker;
