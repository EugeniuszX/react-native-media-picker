package com.eugeniuszx.reactnativemediapicker

import com.facebook.react.bridge.ReactApplicationContext

class ReactNativeMediaPickerModule(reactContext: ReactApplicationContext) :
  NativeReactNativeMediaPickerSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeReactNativeMediaPickerSpec.NAME
  }
}
