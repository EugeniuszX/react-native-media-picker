import NativeMediaPicker from '../NativeReactNativeMediaPicker';
import { cleanTempFiles } from '../index';

describe('cleanTempFiles', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('delegates to the native module', async () => {
    await cleanTempFiles();
    expect(NativeMediaPicker.cleanTempFiles).toHaveBeenCalledTimes(1);
  });

  it('resolves the number of files the native module removed', async () => {
    (NativeMediaPicker.cleanTempFiles as jest.Mock).mockResolvedValueOnce(7);
    await expect(cleanTempFiles()).resolves.toBe(7);
  });
});
