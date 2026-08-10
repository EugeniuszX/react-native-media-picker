import { normalizeCameraOptions } from '../index';

describe('normalizeCameraOptions', () => {
  it('applies defaults for an empty object', () => {
    expect(normalizeCameraOptions({})).toEqual({
      cameraType: 'back',
      maxWidth: 0,
      maxHeight: 0,
      quality: 1,
      includeBase64: false,
      format: 'original',
    });
  });

  it('falls back to "back" for an invalid cameraType', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeCameraOptions({ cameraType: 'sideways' }).cameraType).toBe(
      'back'
    );
  });

  it('passes through "front"', () => {
    expect(normalizeCameraOptions({ cameraType: 'front' }).cameraType).toBe(
      'front'
    );
  });

  it('clamps quality into 0..1 and truncates dimensions', () => {
    const r = normalizeCameraOptions({
      quality: 9,
      maxWidth: 640.9,
      maxHeight: -5,
    });
    expect(r.quality).toBe(1);
    expect(r.maxWidth).toBe(640);
    expect(r.maxHeight).toBe(0);
  });

  it('falls back to "original" for an invalid format', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeCameraOptions({ format: 'heic' }).format).toBe('original');
  });

  it('passes through "jpeg" and "png" formats', () => {
    expect(normalizeCameraOptions({ format: 'jpeg' }).format).toBe('jpeg');
    expect(normalizeCameraOptions({ format: 'png' }).format).toBe('png');
  });
});
