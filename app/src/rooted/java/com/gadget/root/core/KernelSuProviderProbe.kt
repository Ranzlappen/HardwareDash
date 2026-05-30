package com.gadget.root.core

import dev.ranzlappen.gadget.core.root.core.*
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val KERNELSU_PACKAGE = "me.weishu.kernelsu"

@Singleton
class KernelSuProviderProbe @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun probe(): RootProviderInfo? {
        val pkg = runCatching {
            context.packageManager.getPackageInfo(KERNELSU_PACKAGE, 0)
        }.getOrNull() ?: return null
        return RootProviderInfo(
            provider = RootProvider.KernelSu,
            versionName = pkg.versionName,
            versionCode = pkg.longVersionCodeCompat(),
        )
    }
}
