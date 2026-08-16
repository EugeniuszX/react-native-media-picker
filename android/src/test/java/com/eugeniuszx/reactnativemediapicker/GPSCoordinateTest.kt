package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GPSCoordinateTest {
  @Test fun convertsDmsRationalsToDecimalDegrees() {
    // 50° 27' 0.36" N  ->  50.4501
    assertEquals(50.4501, GPSCoordinate.decimal("50/1,27/1,36/100", "N")!!, 1e-6)
  }

  @Test fun appliesTheHemisphereSign() {
    assertEquals(-50.4501, GPSCoordinate.decimal("50/1,27/1,36/100", "S")!!, 1e-6)
    assertEquals(-30.5, GPSCoordinate.decimal("30/1,30/1,0/1", "W")!!, 1e-6)
    assertEquals(30.5, GPSCoordinate.decimal("30/1,30/1,0/1", "E")!!, 1e-6)
  }

  @Test fun treatsAMissingOrOddRefAsPositive() {
    assertEquals(1.0, GPSCoordinate.decimal("1/1,0/1,0/1", null)!!, 1e-9)
    assertEquals(1.0, GPSCoordinate.decimal("1/1,0/1,0/1", "?")!!, 1e-9)
    assertEquals(-1.0, GPSCoordinate.decimal("1/1,0/1,0/1", " s ")!!, 1e-9)
  }

  @Test fun rejectsMalformedRationals() {
    assertNull(GPSCoordinate.decimal(null, "N"))
    assertNull(GPSCoordinate.decimal("", "N"))
    assertNull(GPSCoordinate.decimal("50/1,27/1", "N"))
    assertNull(GPSCoordinate.decimal("50/1,27/1,36/100,1/1", "N"))
    assertNull(GPSCoordinate.decimal("50,27,36", "N"))
    assertNull(GPSCoordinate.decimal("50/0,27/1,36/100", "N"))
    assertNull(GPSCoordinate.decimal("a/1,27/1,36/100", "N"))
  }

  /** `toDoubleOrNull` accepts `Infinity` and `NaN`; a coordinate may never be either. */
  @Test fun rejectsNonFiniteDegrees() {
    assertNull(GPSCoordinate.decimal("Infinity/1,27/1,36/100", "N"))
    assertNull(GPSCoordinate.decimal("50/1,NaN/1,36/100", "N"))
    assertNull(GPSCoordinate.decimal("-Infinity/1,27/1,36/100", "S"))
  }

  @Test fun altitudeRefOneMeansBelowSeaLevel() {
    assertEquals(150.0, GPSCoordinate.altitude("150/1", "0")!!, 1e-9)
    assertEquals(-150.0, GPSCoordinate.altitude("150/1", "1")!!, 1e-9)
    assertEquals(150.0, GPSCoordinate.altitude("150/1", null)!!, 1e-9)
    assertEquals(12.5, GPSCoordinate.altitude("250/20", "0")!!, 1e-9)
  }

  @Test fun rejectsMalformedAltitude() {
    assertNull(GPSCoordinate.altitude(null, "0"))
    assertNull(GPSCoordinate.altitude("150", "0"))
    assertNull(GPSCoordinate.altitude("150/0", "0"))
  }

  @Test fun rejectsNonFiniteAltitude() {
    assertNull(GPSCoordinate.altitude("Infinity/1", "0"))
    assertNull(GPSCoordinate.altitude("NaN/1", "0"))
  }
}
