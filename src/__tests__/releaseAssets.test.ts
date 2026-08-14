import NativeMediaPicker from '../NativeReactNativeMediaPicker';
import { collectReleasableUris, releaseAssets } from '../index';

describe('collectReleasableUris', () => {
  it('accepts a bare uri string', () => {
    expect(collectReleasableUris('file:///tmp/a.jpg')).toEqual([
      'file:///tmp/a.jpg',
    ]);
  });

  it('accepts a single asset', () => {
    expect(collectReleasableUris({ uri: 'file:///tmp/a.jpg' })).toEqual([
      'file:///tmp/a.jpg',
    ]);
  });

  it('collects the thumbnail alongside the asset itself', () => {
    expect(
      collectReleasableUris({
        uri: 'file:///tmp/a.mp4',
        thumbnailUri: 'file:///tmp/a.jpg',
      })
    ).toEqual(['file:///tmp/a.mp4', 'file:///tmp/a.jpg']);
  });

  it('mixes assets and bare uris in one list', () => {
    expect(
      collectReleasableUris([
        { uri: 'file:///tmp/a.mp4', thumbnailUri: 'file:///tmp/a.jpg' },
        'file:///tmp/b.png',
      ])
    ).toEqual(['file:///tmp/a.mp4', 'file:///tmp/a.jpg', 'file:///tmp/b.png']);
  });

  it('drops duplicates, empty strings and nullish entries', () => {
    expect(
      collectReleasableUris([
        { uri: 'file:///tmp/a.jpg' },
        'file:///tmp/a.jpg',
        '',
        // @ts-expect-error testing runtime guard
        null,
        // @ts-expect-error testing runtime guard
        undefined,
      ])
    ).toEqual(['file:///tmp/a.jpg']);
  });
});

describe('releaseAssets', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('forwards the collected uris to the native module', async () => {
    await releaseAssets([
      { uri: 'file:///tmp/a.mp4', thumbnailUri: 'file:///tmp/a.jpg' },
    ]);
    expect(NativeMediaPicker.releaseAssets).toHaveBeenCalledWith([
      'file:///tmp/a.mp4',
      'file:///tmp/a.jpg',
    ]);
  });

  it('skips the native call when nothing is releasable', async () => {
    await releaseAssets([]);
    await releaseAssets('');
    expect(NativeMediaPicker.releaseAssets).not.toHaveBeenCalled();
  });
});
