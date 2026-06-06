import NativeMediaPicker, {
  type Asset,
  type NativeLibraryOptions,
  type PickerResponse,
} from './NativeReactNativeMediaPicker';

export type { Asset, PickerResponse };

export type MediaType = 'photo' | 'video' | 'mixed';

export interface LibraryOptions {
  /** What to allow picking. Phase 1 implements 'photo' natively. Default 'photo'. */
  mediaType?: MediaType;
  /** Max number of items. 1 = single (default), 0 = unlimited. */
  selectionLimit?: number;
  /** Resize bound in px. 0 = no resize. */
  maxWidth?: number;
  maxHeight?: number;
  /** JPEG quality 0..1. Default 1. */
  quality?: number;
  /** Also return base64 of each asset. Default false. */
  includeBase64?: boolean;
}

const VALID_MEDIA_TYPES: ReadonlyArray<MediaType> = ['photo', 'video', 'mixed'];

const clamp = (value: number, min: number, max: number): number =>
  Math.min(Math.max(value, min), max);

const floorAtZero = (value: number): number => (value < 0 ? 0 : value);

export const normalizeLibraryOptions = (
  options: LibraryOptions
): NativeLibraryOptions => {
  const mediaType =
    options.mediaType && VALID_MEDIA_TYPES.includes(options.mediaType)
      ? options.mediaType
      : 'photo';

  return {
    mediaType,
    selectionLimit: floorAtZero(options.selectionLimit ?? 1),
    maxWidth: floorAtZero(options.maxWidth ?? 0),
    maxHeight: floorAtZero(options.maxHeight ?? 0),
    quality: clamp(options.quality ?? 1, 0, 1),
    includeBase64: options.includeBase64 ?? false,
  };
};

export const launchImageLibrary = (
  options: LibraryOptions = {}
): Promise<PickerResponse> =>
  NativeMediaPicker.launchImageLibrary(normalizeLibraryOptions(options));
