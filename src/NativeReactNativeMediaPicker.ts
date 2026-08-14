import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

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
};

export type ErrorCode = 'permission' | 'camera_unavailable' | 'others';

export type PickerResponse = {
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
  launchImageLibrary(options: NativeLibraryOptions): Promise<PickerResponse>;
  launchCamera(options: NativeCameraOptions): Promise<PickerResponse>;
  cleanTempFiles(): Promise<number>;
  releaseAssets(uris: Array<string>): Promise<number>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeMediaPicker');
