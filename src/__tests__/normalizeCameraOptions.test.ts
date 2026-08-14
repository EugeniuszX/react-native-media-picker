import { normalizeCameraOptions } from '../index';

describe('normalizeCameraOptions', () => {
  it('applies defaults for an empty object', () => {
    expect(normalizeCameraOptions({})).toEqual({
      cameraType: 'back',
      mediaType: 'photo',
      maxWidth: 0,
      maxHeight: 0,
      quality: 1,
      includeBase64: false,
      format: 'original',
      maxDuration: 0,
      videoQuality: 'high',
      includeThumbnail: false,
      includeExif: false,
      stripMetadata: false,
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

  it('defaults mediaType to "photo" and videoQuality to "high"', () => {
    const r = normalizeCameraOptions({});
    expect(r.mediaType).toBe('photo');
    expect(r.videoQuality).toBe('high');
    expect(r.maxDuration).toBe(0);
    expect(r.includeThumbnail).toBe(false);
  });

  it('passes through a video request', () => {
    const r = normalizeCameraOptions({
      mediaType: 'video',
      maxDuration: 30,
      videoQuality: 'low',
      includeThumbnail: true,
    });
    expect(r.mediaType).toBe('video');
    expect(r.maxDuration).toBe(30);
    expect(r.videoQuality).toBe('low');
    expect(r.includeThumbnail).toBe(true);
  });

  it('falls back to "photo" for an invalid mediaType', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeCameraOptions({ mediaType: 'audio' }).mediaType).toBe(
      'photo'
    );
  });

  it('falls back to "high" for an invalid videoQuality', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeCameraOptions({ videoQuality: 'ultra' }).videoQuality).toBe(
      'high'
    );
  });

  it('clamps and truncates maxDuration', () => {
    expect(normalizeCameraOptions({ maxDuration: -5 }).maxDuration).toBe(0);
    expect(normalizeCameraOptions({ maxDuration: 12.9 }).maxDuration).toBe(12);
  });
});
