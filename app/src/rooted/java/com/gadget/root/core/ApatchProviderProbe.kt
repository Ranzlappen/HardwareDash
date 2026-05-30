package com.gadget.root.core

import dev.ranzlappen.gadget.core.root.core.*
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val APATCH_PACKAGE = "me.bmax.apatch"

@Singleton
class ApatchProviderProbe @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun probe(): RootProviderInfo? {
        val pkg = runCatching {
            context.packageManager.getPackageInfo(APATCH_PACKAGE, 0)
        }.getOrNull() ?: return null
        return RootProviderInfo(
            provider = RootProvider.APatch,
            versionName = pkg.versionName,
            versionCode = pkg.longVersionCodeCompat(),
        )
    }
}
