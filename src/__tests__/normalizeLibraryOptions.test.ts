import { normalizeLibraryOptions } from '../index';

describe('normalizeLibraryOptions', () => {
  it('applies defaults for an empty object', () => {
    expect(normalizeLibraryOptions({})).toEqual({
      selectionLimit: 1,
      maxWidth: 0,
      maxHeight: 0,
      quality: 1,
      includeBase64: false,
      format: 'original',
      mediaType: 'photo',
      includeThumbnail: false,
      includeExif: false,
      stripMetadata: false,
    });
  });

  it('passes through includeThumbnail', () => {
    expect(
      normalizeLibraryOptions({ includeThumbnail: true }).includeThumbnail
    ).toBe(true);
  });

  it('clamps quality into the 0..1 range', () => {
    expect(normalizeLibraryOptions({ quality: 5 }).quality).toBe(1);
    expect(normalizeLibraryOptions({ quality: -2 }).quality).toBe(0);
    expect(normalizeLibraryOptions({ quality: 0.7 }).quality).toBe(0.7);
  });

  it('clamps negative selectionLimit / dimensions to 0', () => {
    const r = normalizeLibraryOptions({
      selectionLimit: -3,
      maxWidth: -10,
      maxHeight: -1,
    });
    expect(r.selectionLimit).toBe(0);
    expect(r.maxWidth).toBe(0);
    expect(r.maxHeight).toBe(0);
  });

  it('truncates fractional selectionLimit / dimensions to integers', () => {
    const r = normalizeLibraryOptions({
      selectionLimit: 2.9,
      maxWidth: 640.7,
      maxHeight: 480.5,
    });
    expect(r.selectionLimit).toBe(2);
    expect(r.maxWidth).toBe(640);
    expect(r.maxHeight).toBe(480);
  });

  it('falls back to "original" for an invalid format', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeLibraryOptions({ format: 'webp' }).format).toBe('original');
  });

  it('passes through "jpeg" and "png" formats', () => {
    expect(normalizeLibraryOptions({ format: 'jpeg' }).format).toBe('jpeg');
    expect(normalizeLibraryOptions({ format: 'png' }).format).toBe('png');
  });

  it('defaults mediaType to "photo"', () => {
    expect(normalizeLibraryOptions({}).mediaType).toBe('photo');
  });

  it('passes through valid media types', () => {
    expect(normalizeLibraryOptions({ mediaType: 'video' }).mediaType).toBe(
      'video'
    );
    expect(normalizeLibraryOptions({ mediaType: 'mixed' }).mediaType).toBe(
      'mixed'
    );
    expect(normalizeLibraryOptions({ mediaType: 'photo' }).mediaType).toBe(
      'photo'
    );
  });

  it('falls back to "photo" for an invalid mediaType', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeLibraryOptions({ mediaType: 'audio' }).mediaType).toBe(
      'photo'
    );
  });

  it('defaults the new metadata options to false', () => {
    const r = normalizeLibraryOptions({});
    expect(r.includeExif).toBe(false);
    expect(r.stripMetadata).toBe(false);
  });

  it('passes through the new metadata options', () => {
    const r = normalizeLibraryOptions({
      includeExif: true,
      stripMetadata: true,
    });
    expect(r.includeExif).toBe(true);
    expect(r.stripMetadata).toBe(true);
  });
});
