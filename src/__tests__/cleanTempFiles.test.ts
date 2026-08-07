import NativeMediaPicker from '../NativeReactNativeMediaPicker';
import { cleanTempFiles } from '../index';

describe('cleanTempFiles', () => {
  it('delegates to the native module', async () => {
    await cleanTempFiles();
    expect(NativeMediaPicker.cleanTempFiles).toHaveBeenCalledTimes(1);
  });
});
