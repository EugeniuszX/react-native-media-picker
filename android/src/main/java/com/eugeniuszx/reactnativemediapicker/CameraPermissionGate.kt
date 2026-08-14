package com.eugeniuszx.reactnativemediapicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.util.concurrent.atomic.AtomicBoolean

internal class CameraPermissionGate(private val context: Context) {
  private val requesting = AtomicBoolean(false)

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
    markAsked()
    permissionActivity.requestPermissions(
      arrayOf(Manifest.permission.CAMERA),
      REQUEST_CODE,
      listener,
    )
  }

  fun status(activity: Activity?): CameraPermission = CameraPermission.resolve(
    hasCamera = hasCamera(),
    declared = isDeclared(),
    granted = isGranted(),
    hasAsked = hasAsked(),
    shouldShowRationale = shouldShowRationale(activity),
  )

  /**
   * Prompts only when the status can still be changed by asking. Every other case — including no
   * activity to prompt from, and a prompt already in flight — settles with the current status
   * instead of queueing a second dialog.
   */
  fun request(activity: Activity?, onResult: (CameraPermission) -> Unit) {
    val current = status(activity)
    if (current != CameraPermission.NOT_DETERMINED && current != CameraPermission.DENIED) {
      onResult(current)
      return
    }
    val permissionActivity = activity as? PermissionAwareActivity
    if (permissionActivity == null) {
      onResult(current)
      return
    }
    if (!requesting.compareAndSet(false, true)) {
      onResult(current)
      return
    }
    val listener = PermissionListener { requestCode, _, _ ->
      if (requestCode != STATUS_REQUEST_CODE) return@PermissionListener false
      requesting.set(false)
      onResult(status(activity))
      true
    }
    markAsked()
    try {
      permissionActivity.requestPermissions(
        arrayOf(Manifest.permission.CAMERA),
        STATUS_REQUEST_CODE,
        listener,
      )
    } catch (e: Throwable) {
      requesting.set(false)
      onResult(status(activity))
    }
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

  private fun hasCamera(): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

  /**
   * Without a foreground activity the rationale flag cannot be read. Reporting `false` then lands
   * an already-refused permission on `blocked`, which points the app at Settings — the harmless way
   * to be wrong, since Settings can grant the permission either way.
   */
  private fun shouldShowRationale(activity: Activity?): Boolean =
    activity != null &&
      ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

  private fun hasAsked(): Boolean = preferences().getBoolean(KEY_ASKED, false)

  private fun markAsked() {
    preferences().edit().putBoolean(KEY_ASKED, true).apply()
  }

  private fun preferences() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  private companion object {
    const val REQUEST_CODE = 48213
    const val STATUS_REQUEST_CODE = 48214
    const val PREFERENCES_NAME = "com.eugeniuszx.reactnativemediapicker.permissions"
    const val KEY_ASKED = "camera_permission_asked"
  }
}
