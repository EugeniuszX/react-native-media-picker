package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionTest {
  @Test fun reportsUnavailableWithoutACamera() {
    assertEquals(
      CameraPermission.UNAVAILABLE,
      CameraPermission.resolve(
        hasCamera = false,
        declared = true,
        granted = true,
        hasAsked = true,
        shouldShowRationale = false,
      ),
    )
  }

  @Test fun reportsGrantedWhenThePermissionIsHeld() {
    assertEquals(
      CameraPermission.GRANTED,
      CameraPermission.resolve(
        hasCamera = true,
        declared = true,
        granted = true,
        hasAsked = true,
        shouldShowRationale = false,
      ),
    )
  }

  @Test fun reportsNotRequiredWhenTheAppDoesNotDeclareThePermission() {
    assertEquals(
      CameraPermission.NOT_REQUIRED,
      CameraPermission.resolve(
        hasCamera = true,
        declared = false,
        granted = false,
        hasAsked = true,
        shouldShowRationale = true,
      ),
    )
  }

  @Test fun reportsNotDeterminedBeforeTheFirstPrompt() {
    assertEquals(
      CameraPermission.NOT_DETERMINED,
      CameraPermission.resolve(
        hasCamera = true,
        declared = true,
        granted = false,
        hasAsked = false,
        shouldShowRationale = false,
      ),
    )
  }

  @Test fun tellsAReaskableRefusalFromAPermanentOne() {
    assertEquals(
      CameraPermission.DENIED,
      CameraPermission.resolve(
        hasCamera = true,
        declared = true,
        granted = false,
        hasAsked = true,
        shouldShowRationale = true,
      ),
    )
    assertEquals(
      CameraPermission.BLOCKED,
      CameraPermission.resolve(
        hasCamera = true,
        declared = true,
        granted = false,
        hasAsked = true,
        shouldShowRationale = false,
      ),
    )
  }

  @Test fun valuesMatchTheJsUnion() {
    assertEquals("granted", CameraPermission.GRANTED.value)
    assertEquals("not_determined", CameraPermission.NOT_DETERMINED.value)
    assertEquals("denied", CameraPermission.DENIED.value)
    assertEquals("blocked", CameraPermission.BLOCKED.value)
    assertEquals("not_required", CameraPermission.NOT_REQUIRED.value)
    assertEquals("unavailable", CameraPermission.UNAVAILABLE.value)
  }
}
