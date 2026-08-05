import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type Asset = {
  uri: string;
  type: string;
  fileName?: string;
  fileSize?: number;
  width?: number;
  height?: number;
  base64?: string;
};

/** Closed set of failure codes the natives can report. */
export type ErrorCode = 'permission' | 'camera_unavailable' | 'others';

export type PickerResponse = {
  didCancel: boolean;
  errorCode?: ErrorCode;
  errorMessage?: string;
  assets?: Asset[];
};

// Fully-populated options object. The TS wrapper fills every field with a
// concrete value before calling native, so codegen never sees optionals here.
export type NativeLibraryOptions = {
  selectionLimit: number;
  maxWidth: number;
  maxHeight: number;
  quality: number;
  includeBase64: boolean;
};

// Fully-populated camera options; the TS wrapper fills every field before the call.
export type NativeCameraOptions = {
  cameraType: string;
  maxWidth: number;
  maxHeight: number;
  quality: number;
  includeBase64: boolean;
};

export interface Spec extends TurboModule {
  launchImageLibrary(options: NativeLibraryOptions): Promise<PickerResponse>;
  launchCamera(options: NativeCameraOptions): Promise<PickerResponse>;
  cleanTempFiles(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeMediaPicker');
