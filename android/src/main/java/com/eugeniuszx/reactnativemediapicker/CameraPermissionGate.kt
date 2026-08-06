package com.eugeniuszx.reactnativemediapicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

/**
 * Requests CAMERA at runtime, but only if the host app declares it. An app that
 * does not declare CAMERA can still launch the system camera app, which handles
 * its own permissions.
 */
internal class CameraPermissionGate(private val context: Context) {
  /**
   * Calls [onGranted] when the camera may be opened, or [onDenied] with a
   * reason. Exactly one of the two runs.
   */
  fun ensure(activity: Activity, onGranted: () -> Unit, onDenied: (PickerError, String) -> Unit) {
    if (!isDeclared() || isGranted()) {
      onGranted()
      return
    }
    val permissionActivity = activity as? PermissionAwareActivity
    if (permissionActivity == null) {
      onDenied(PickerError.OTHERS, "Host activity does not support runtime permission requests")
      return
    }
    val listener = PermissionListener { requestCode, _, grantResults ->
      if (requestCode != REQUEST_CODE) return@PermissionListener false
      val granted =
        grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
      if (granted) {
        onGranted()
      } else {
        onDenied(PickerError.PERMISSION, "Camera permission denied")
      }
      true
    }
    permissionActivity.requestPermissions(
      arrayOf(Manifest.permission.CAMERA),
      REQUEST_CODE,
      listener,
    )
  }

  private fun isDeclared(): Boolean = try {
    val info = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_PERMISSIONS,
    )
    info.requestedPermissions?.contains(Manifest.permission.CAMERA) == true
  } catch (e: PackageManager.NameNotFoundException) {
    false
  }

  private fun isGranted(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED

  private companion object {
    const val REQUEST_CODE = 48213
  }
}
