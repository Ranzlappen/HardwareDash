package com.gadget.root.core

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MAGISK_PACKAGE = "com.topjohnwu.magisk"

/**
 * Detects Magisk by package presence (the Magisk app sticks around even on
 * "hidden" installs because the app is the user's only path to grant root).
 * Falls back to null when neither the package nor `/data/adb/magisk/` exists.
 */
@Singleton
class MagiskProviderProbe @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun probe(): RootProviderInfo? {
        val pkg = packageInfo() ?: return null
        return RootProviderInfo(
            provider = RootProvider.Magisk,
            versionName = pkg.versionName,
            versionCode = pkg.longVersionCodeCompat(),
        )
    }

    private fun packageInfo() = runCatching {
        context.packageManager.getPackageInfo(MAGISK_PACKAGE, 0)
    }.getOrNull()
}

fun PackageInfo.longVersionCodeCompat(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION") versionCode.toLong()
    }
