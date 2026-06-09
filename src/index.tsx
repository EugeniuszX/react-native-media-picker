import NativeMediaPicker, {
  type Asset,
  type NativeCameraOptions,
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

export type CameraType = 'back' | 'front';

export interface CameraOptions {
  /** Which camera to open. Honored on iOS; best-effort on Android. Default 'back'. */
  cameraType?: CameraType;
  /** Resize bound in px. 0 = no resize. */
  maxWidth?: number;
  maxHeight?: number;
  /** JPEG quality 0..1. Default 1. */
  quality?: number;
  /** Also return base64 of the captured asset. Default false. */
  includeBase64?: boolean;
}

const VALID_CAMERA_TYPES: ReadonlyArray<CameraType> = ['back', 'front'];

const VALID_MEDIA_TYPES: ReadonlyArray<MediaType> = ['photo', 'video', 'mixed'];

const clamp = (value: number, min: number, max: number): number =>
  Math.min(Math.max(value, min), max);

const clampMin0 = (value: number): number => Math.max(value, 0);

/**
 * @internal
 * Exported only for testing purposes. This function is not part of the public
 * API and may change between versions without a semver bump.
 */
export const normalizeLibraryOptions = (
  options: LibraryOptions
): NativeLibraryOptions => {
  const mediaType =
    options.mediaType && VALID_MEDIA_TYPES.includes(options.mediaType)
      ? options.mediaType
      : 'photo';

  return {
    mediaType,
    selectionLimit: Math.trunc(clampMin0(options.selectionLimit ?? 1)),
    maxWidth: Math.trunc(clampMin0(options.maxWidth ?? 0)),
    maxHeight: Math.trunc(clampMin0(options.maxHeight ?? 0)),
    quality: clamp(options.quality ?? 1, 0, 1),
    includeBase64: options.includeBase64 ?? false,
  };
};

export const launchImageLibrary = (
  options: LibraryOptions = {}
): Promise<PickerResponse> =>
  NativeMediaPicker.launchImageLibrary(normalizeLibraryOptions(options));

/** @internal exported for tests; normalization may change between versions. */
export const normalizeCameraOptions = (
  options: CameraOptions
): NativeCameraOptions => {
  const cameraType =
    options.cameraType && VALID_CAMERA_TYPES.includes(options.cameraType)
      ? options.cameraType
      : 'back';

  return {
    cameraType,
    maxWidth: Math.trunc(clampMin0(options.maxWidth ?? 0)),
    maxHeight: Math.trunc(clampMin0(options.maxHeight ?? 0)),
    quality: clamp(options.quality ?? 1, 0, 1),
    includeBase64: options.includeBase64 ?? false,
  };
};

export const launchCamera = (
  options: CameraOptions = {}
): Promise<PickerResponse> =>
  NativeMediaPicker.launchCamera(normalizeCameraOptions(options));
