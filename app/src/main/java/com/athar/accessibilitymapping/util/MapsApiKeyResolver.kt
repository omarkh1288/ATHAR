package com.athar.accessibilitymapping.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun resolveMapsApiKey(context: Context): String {
  return try {
    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
      )
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    }
    appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
  } catch (_: Exception) {
    ""
  }
}
