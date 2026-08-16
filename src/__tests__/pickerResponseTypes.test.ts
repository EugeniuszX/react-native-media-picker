import type { Asset, ErrorCode, PickerResponse } from '../index';

describe('PickerResponse narrowing', () => {
  it('narrows to a non-optional asset list once cancel and error are ruled out', () => {
    const response = {
      didCancel: false,
      assets: [{ uri: 'file:///tmp/a.jpg', type: 'image/jpeg' }],
    } as PickerResponse;

    if (response.didCancel) throw new Error('unreachable');
    if (response.errorCode) throw new Error('unreachable');

    // The point of the union: `assets` is Asset[], not Asset[] | undefined.
    const assets: Asset[] = response.assets;
    expect(assets).toHaveLength(1);
  });

  it('narrows errorMessage to a string on the error member', () => {
    const response = {
      didCancel: false,
      errorCode: 'busy',
      errorMessage: 'Already waiting for a pick.',
    } as PickerResponse;

    if (response.didCancel) throw new Error('unreachable');
    if (!response.errorCode) throw new Error('unreachable');

    const message: string = response.errorMessage;
    expect(message).toBe('Already waiting for a pick.');
  });

  it('keeps reading optional fields on an un-narrowed value compiling', () => {
    const response = { didCancel: true } as PickerResponse;
    expect(response.assets?.[0]).toBeUndefined();
    expect(response.errorMessage).toBeUndefined();
  });

  it('accepts busy as an ErrorCode', () => {
    const code: ErrorCode = 'busy';
    expect(code).toBe('busy');
  });
});
