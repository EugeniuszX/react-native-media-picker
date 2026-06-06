import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type Asset = {
  uri: string;
  type: string;
  fileName?: string;
  fileSize?: number;
  width?: number;
  height?: number;
  // Forward-compat for video (Phase 3); never set for photos.
  duration?: number;
  base64?: string;
};

export type PickerResponse = {
  didCancel: boolean;
  errorCode?: string;
  errorMessage?: string;
  assets?: Asset[];
};

// Fully-populated options object. The TS wrapper fills every field with a
// concrete value before calling native, so codegen never sees optionals here.
export type NativeLibraryOptions = {
  mediaType: string;
  selectionLimit: number;
  maxWidth: number;
  maxHeight: number;
  quality: number;
  includeBase64: boolean;
};

export interface Spec extends TurboModule {
  launchImageLibrary(options: NativeLibraryOptions): Promise<PickerResponse>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeMediaPicker');
