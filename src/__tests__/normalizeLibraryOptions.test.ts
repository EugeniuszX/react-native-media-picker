import { normalizeLibraryOptions } from '../index';

describe('normalizeLibraryOptions', () => {
  it('applies defaults for an empty object', () => {
    expect(normalizeLibraryOptions({})).toEqual({
      mediaType: 'photo',
      selectionLimit: 1,
      maxWidth: 0,
      maxHeight: 0,
      quality: 1,
      includeBase64: false,
    });
  });

  it('clamps quality into the 0..1 range', () => {
    expect(normalizeLibraryOptions({ quality: 5 }).quality).toBe(1);
    expect(normalizeLibraryOptions({ quality: -2 }).quality).toBe(0);
    expect(normalizeLibraryOptions({ quality: 0.7 }).quality).toBe(0.7);
  });

  it('floors negative selectionLimit / dimensions to 0', () => {
    const r = normalizeLibraryOptions({
      selectionLimit: -3,
      maxWidth: -10,
      maxHeight: -1,
    });
    expect(r.selectionLimit).toBe(0);
    expect(r.maxWidth).toBe(0);
    expect(r.maxHeight).toBe(0);
  });

  it('falls back to "photo" for an invalid mediaType', () => {
    // @ts-expect-error testing runtime guard
    expect(normalizeLibraryOptions({ mediaType: 'banana' }).mediaType).toBe(
      'photo'
    );
  });

  it('passes through valid mediaType values', () => {
    expect(normalizeLibraryOptions({ mediaType: 'video' }).mediaType).toBe(
      'video'
    );
  });
});
