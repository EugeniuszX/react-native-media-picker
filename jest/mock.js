/* global jest */

const cancelled = () => Promise.resolve({ didCancel: true });

module.exports = {
  launchImageLibrary: jest.fn(cancelled),
  launchCamera: jest.fn(cancelled),
  cleanTempFiles: jest.fn(() => Promise.resolve()),
  releaseAssets: jest.fn(() => Promise.resolve()),
};
