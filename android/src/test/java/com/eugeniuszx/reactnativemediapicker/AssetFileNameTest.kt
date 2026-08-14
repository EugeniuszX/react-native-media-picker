package com.eugeniuszx.reactnativemediapicker

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetFileNameTest {
  private val fallback = "media_picker_ABC.jpg"

  @Test fun keepsTheSourceNameAndAppliesTheOutputExtension() {
    assertEquals("IMG_4821.jpg", AssetFileName.resolve("IMG_4821", fallback, "jpg"))
  }

  @Test fun replacesTheSourceExtensionWithTheOutputOne() {
    assertEquals("IMG_4821.jpg", AssetFileName.resolve("IMG_4821.HEIC", fallback, "jpg"))
  }

  @Test fun fallsBackWhenNoNameIsAvailable() {
    assertEquals(fallback, AssetFileName.resolve(null, fallback, "jpg"))
    assertEquals(fallback, AssetFileName.resolve("   ", fallback, "jpg"))
    assertEquals(fallback, AssetFileName.resolve(".HEIC", fallback, "jpg"))
  }

  @Test fun keepsOnlyTheLastPathComponent() {
    assertEquals("passwd.jpg", AssetFileName.resolve("../../etc/passwd", fallback, "jpg"))
    assertEquals("IMG_1.jpg", AssetFileName.resolve("DCIM\\Camera\\IMG_1.jpg", fallback, "jpg"))
    assertEquals(fallback, AssetFileName.resolve("..", fallback, "jpg"))
  }

  @Test fun stripsControlCharacters() {
    assertEquals("IMG_4821.jpg", AssetFileName.resolve("IMG\u0000_\n48\t21", fallback, "jpg"))
  }

  @Test fun keepsInnerSpacesAndUnicode() {
    assertEquals("Отпуск 2026.jpg", AssetFileName.resolve(" Отпуск 2026.png ", fallback, "jpg"))
  }

  @Test fun truncatesLongNames() {
    val long = "a".repeat(240)
    assertEquals(
      "a".repeat(AssetFileName.MAX_BASE_LENGTH) + ".jpeg",
      AssetFileName.resolve(long, fallback, "jpeg"),
    )
  }

  @Test fun onlyStripsAnExtensionThatLooksLikeOne() {
    assertEquals("report.final.jpg", AssetFileName.resolve("report.final.v2", fallback, "jpg"))
    assertEquals(
      "shot 2026.08.14 18.22.jpg",
      AssetFileName.resolve("shot 2026.08.14 18.22.31", fallback, "jpg"),
    )
  }

  @Test fun returnsTheBareNameWhenNoExtensionIsGiven() {
    assertEquals("IMG_4821", AssetFileName.resolve("IMG_4821.HEIC", fallback, ""))
  }
}
