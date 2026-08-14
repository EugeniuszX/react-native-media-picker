import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type Exif = {
  dateTimeOriginal?: string;
  latitude?: number;
  longitude?: number;
  altitude?: number;
  make?: string;
  model?: string;
  orientation?: number;
  iso?: number;
  fNumber?: number;
  exposureTime?: number;
  focalLength?: number;
};

export type Asset = {
  uri: string;
  type: string;
  fileName?: string;
  fileSize?: number;
  width?: number;
  height?: number;
  duration?: number;
  base64?: string;
  thumbnailUri?: string;
  thumbnailWidth?: number;
  thumbnailHeight?: number;
  exif?: Exif;
};

export type ErrorCode = 'permission' | 'camera_unavailable' | 'busy' | 'others';

export type NativePickerResponse = {
  didCancel: boolean;
  errorCode?: ErrorCode;
  errorMessage?: string;
  assets?: Asset[];
};

export type NativeLibraryOptions = {
  selectionLimit: number;
  maxWidth: number;
  maxHeight: number;
  quality: number;
  includeBase64: boolean;
  format: string;
  mediaType: string;
  includeThumbnail: boolean;
};

export type NativeCameraOptions = {
  cameraType: string;
  maxWidth: number;
  maxHeight: number;
  quality: number;
  includeBase64: boolean;
  format: string;
};

export interface Spec extends TurboModule {
  launchImageLibrary(
    options: NativeLibraryOptions
  ): Promise<NativePickerResponse>;
  launchCamera(options: NativeCameraOptions): Promise<NativePickerResponse>;
  cleanTempFiles(): Promise<number>;
  releaseAssets(uris: Array<string>): Promise<number>;
  getCameraPermissionStatus(): Promise<string>;
  requestCameraPermission(): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeMediaPicker');
