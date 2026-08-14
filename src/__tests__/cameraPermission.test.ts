import NativeMediaPicker from '../NativeReactNativeMediaPicker';
import { getCameraPermissionStatus, requestCameraPermission } from '../index';

describe('getCameraPermissionStatus', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('resolves the status reported by the native module', async () => {
    (
      NativeMediaPicker.getCameraPermissionStatus as jest.Mock
    ).mockResolvedValueOnce('blocked');
    await expect(getCameraPermissionStatus()).resolves.toBe('blocked');
  });

  it('never prompts on its own', async () => {
    await getCameraPermissionStatus();
    expect(NativeMediaPicker.requestCameraPermission).not.toHaveBeenCalled();
  });
});

describe('requestCameraPermission', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('resolves the status the native module settles on', async () => {
    (
      NativeMediaPicker.requestCameraPermission as jest.Mock
    ).mockResolvedValueOnce('granted');
    await expect(requestCameraPermission()).resolves.toBe('granted');
  });
});
