package com.eugeniuszx.reactnativemediapicker

import androidx.core.content.FileProvider

/**
 * The manifest merger keys `provider` elements by android:name, so declaring
 * androidx.core.content.FileProvider here collides with host apps that ship their own
 * FileProvider. This subclass gives the library provider its own merge key.
 */
class MediaPickerFileProvider : FileProvider()
