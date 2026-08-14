import NativeMediaPicker, {
  type Asset,
  type ErrorCode,
  type NativeCameraOptions,
  type NativeLibraryOptions,
  type PickerResponse,
} from './NativeReactNativeMediaPicker';

export type { Asset, ErrorCode, PickerResponse };

export type OutputFormat = 'original' | 'jpeg' | 'png';

export type MediaType = 'photo' | 'video' | 'mixed';

export interface LibraryOptions {
  selectionLimit?: number;
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
  includeBase64?: boolean;
  format?: OutputFormat;
  mediaType?: MediaType;
  includeThumbnail?: boolean;
}

export type CameraType = 'back' | 'front';

export interface CameraOptions {
  cameraType?: CameraType;
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
  includeBase64?: boolean;
  format?: OutputFormat;
}

const VALID_CAMERA_TYPES: ReadonlyArray<CameraType> = ['back', 'front'];

const VALID_FORMATS: ReadonlyArray<OutputFormat> = ['original', 'jpeg', 'png'];

const VALID_MEDIA_TYPES: ReadonlyArray<MediaType> = ['photo', 'video', 'mixed'];

const clamp = (value: number, min: number, max: number): number =>
  Math.min(Math.max(value, min), max);

const clampMin0 = (value: number): number => Math.max(value, 0);

const normalizeFormat = (format: OutputFormat | undefined): OutputFormat =>
  format && VALID_FORMATS.includes(format) ? format : 'original';

const normalizeMediaType = (mediaType: MediaType | undefined): MediaType =>
  mediaType && VALID_MEDIA_TYPES.includes(mediaType) ? mediaType : 'photo';

export const normalizeLibraryOptions = (
  options: LibraryOptions
): NativeLibraryOptions => ({
  selectionLimit: Math.trunc(clampMin0(options.selectionLimit ?? 1)),
  maxWidth: Math.trunc(clampMin0(options.maxWidth ?? 0)),
  maxHeight: Math.trunc(clampMin0(options.maxHeight ?? 0)),
  quality: clamp(options.quality ?? 1, 0, 1),
  includeBase64: options.includeBase64 ?? false,
  format: normalizeFormat(options.format),
  mediaType: normalizeMediaType(options.mediaType),
  includeThumbnail: options.includeThumbnail ?? false,
});

export const launchImageLibrary = (
  options: LibraryOptions = {}
): Promise<PickerResponse> =>
  NativeMediaPicker.launchImageLibrary(normalizeLibraryOptions(options));

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
    format: normalizeFormat(options.format),
  };
};

export const launchCamera = (
  options: CameraOptions = {}
): Promise<PickerResponse> =>
  NativeMediaPicker.launchCamera(normalizeCameraOptions(options));

export const cleanTempFiles = (): Promise<void> =>
  NativeMediaPicker.cleanTempFiles();
