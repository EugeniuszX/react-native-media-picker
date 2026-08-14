/* global jest */

const cancelled = () => Promise.resolve({ didCancel: true });

module.exports = {
  launchImageLibrary: jest.fn(cancelled),
  launchCamera: jest.fn(cancelled),
  cleanTempFiles: jest.fn(() => Promise.resolve(0)),
  releaseAssets: jest.fn(() => Promise.resolve(0)),
  getCameraPermissionStatus: jest.fn(() => Promise.resolve('granted')),
  requestCameraPermission: jest.fn(() => Promise.resolve('granted')),
};
