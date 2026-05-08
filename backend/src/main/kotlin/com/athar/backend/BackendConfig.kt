package com.athar.backend

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

internal object BackendConfig {
  private val localProperties: Properties by lazy(::loadLocalProperties)

  fun string(primaryKey: String, vararg fallbackKeys: String): String? {
    val keys = listOf(primaryKey) + fallbackKeys
    return keys.asSequence()
      .mapNotNull(::readRawValue)
      .map(String::trim)
      .firstOrNull { it.isNotEmpty() }
  }

  fun int(primaryKey: String, vararg fallbackKeys: String): Int? {
    return string(primaryKey, *fallbackKeys)?.toIntOrNull()
  }

  fun boolean(primaryKey: String, vararg fallbackKeys: String): Boolean? {
    return string(primaryKey, *fallbackKeys)?.let { raw ->
      when (raw.lowercase()) {
        "true", "1", "yes", "on" -> true
        "false", "0", "no", "off" -> false
        else -> null
      }
    }
  }

  private fun readRawValue(key: String): String? {
    return System.getenv(key)
      ?: System.getProperty(key)
      ?: localProperties.getProperty(key)
  }

  private fun loadLocalProperties(): Properties {
    val properties = Properties()
    val path = resolveLocalPropertiesPath() ?: return properties
    Files.newInputStream(path).use(properties::load)
    return properties
  }

  private fun resolveLocalPropertiesPath(): Path? {
    var current = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
    repeat(8) {
      val candidate = current.resolve("local.properties")
      if (Files.isRegularFile(candidate)) {
        return candidate
      }
      current = current.parent ?: return null
    }
    return null
  }
}
