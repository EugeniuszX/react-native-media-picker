package com.eugeniuszx.reactnativemediapicker

/**
 * Builds the `fileName` an asset is reported with.
 *
 * The name the system hands over for a picked item is a label, not a path — the bytes always live
 * in a temp file under a generated name — so it is sanitized before it leaves the module, and its
 * extension is replaced with that of the file actually written. [fallback] (the temp file's own
 * name) is used whenever nothing usable survives sanitizing.
 */
internal object AssetFileName {
  const val MAX_BASE_LENGTH = 100

  fun resolve(suggested: String?, fallback: String, extension: String): String {
    val base = sanitizedBase(suggested) ?: return fallback
    return if (extension.isEmpty()) base else "$base.$extension"
  }

  private fun sanitizedBase(suggested: String?): String? {
    if (suggested == null) return null
    var name = suggested.split('/', '\\').lastOrNull { it.isNotEmpty() } ?: return null
    name = name.filterNot { it.category == CharCategory.CONTROL || it.category == CharCategory.FORMAT }
    name = name.trim()
    name = withoutExtension(name)
    name = name.trimEdges()
    if (name.length > MAX_BASE_LENGTH) {
      name = name.take(MAX_BASE_LENGTH).trimEdges()
    }
    return name.ifEmpty { null }
  }

  private fun String.trimEdges(): String = trim { it.isWhitespace() || it == '.' }

  /**
   * Drops a trailing `.ext` only when it looks like a file extension: at most five ASCII
   * alphanumerics. Keeps names that merely contain dots, like `report.final`.
   */
  private fun withoutExtension(name: String): String {
    val dot = name.lastIndexOf('.')
    if (dot < 0) return name
    val extension = name.substring(dot + 1)
    if (extension.isEmpty() || extension.length > 5) return name
    if (!extension.all { it.code < 128 && (it.isLetter() || it.isDigit()) }) return name
    return name.substring(0, dot)
  }
}
