package com.gadget.gps.spoof

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.ranzlappen.gadget.core.root.core.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies the bundled LSPosed module APK from `assets/lsposed-spoofer.apk`
 * into `/data/local/tmp/`, then `pm install --replace`s it via libsu.
 * Verifies install via `pm path`. Does NOT enable the module — that step
 * happens in LSPosed Manager (the user is deep-linked there from the UI).
 *
 * Requires `enableLsposedModule=true` to have been passed to Gradle when
 * building the rooted APK; without that, the asset isn't bundled and
 * [installLsposedModule] returns Unsupported.
 */
@Singleton
class LsposedModuleInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: RootShell,
) {

    suspend fun isAssetBundled(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(ASSET_NAME).use { /* can open */ }
            true
        }.getOrElse { false }
    }

    suspend fun isModuleInstalled(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getPackageInfo(MODULE_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Returns versionCode of the installed LSPosed module, or 0 if not installed. */
    suspend fun installedVersion(): Int = withContext(Dispatchers.IO) {
        try {
            val info = context.packageManager.getPackageInfo(MODULE_PACKAGE, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
        } catch (_: PackageManager.NameNotFoundException) {
            0
        }
    }

    /**
     * Reads the bundled APK's versionCode from `assets/`. Returns 0 if the
     * asset isn't bundled (built with `-PenableLsposedModule=true`?).
     */
    suspend fun bundledVersion(): Int = withContext(Dispatchers.IO) {
        if (!isAssetBundled()) return@withContext 0
        // Copy asset to a temporary file so PackageManager can introspect it.
        val tmp = java.io.File(context.cacheDir, "$ASSET_NAME.peek")
        try {
            context.assets.open(ASSET_NAME).use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            }
            val info = context.packageManager.getPackageArchiveInfo(tmp.absolutePath, 0) ?: return@withContext 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
        } finally {
            tmp.delete()
        }
    }

    /**
     * Copies the bundled APK to /data/local/tmp/ via app I/O, then
     * `pm install --replace`s it via libsu. Returns Ok on success or
     * Failed with the relevant stderr.
     */
    suspend fun install(): SpoofResult = withContext(Dispatchers.IO) {
        if (!isAssetBundled()) {
            return@withContext SpoofResult.Unsupported(
                reason = "LSPosed module not bundled in this build (rebuild with -PenableLsposedModule=true)",
            )
        }

        val target = "/data/local/tmp/$ASSET_NAME"
        val staged = java.io.File(context.cacheDir, ASSET_NAME)
        try {
            context.assets.open(ASSET_NAME).use { input ->
                staged.outputStream().use { input.copyTo(it) }
            }
        } catch (t: Throwable) {
            return@withContext SpoofResult.Failed("Could not stage LSPosed APK", t)
        }

        // Copy via libsu — `cp` from cache dir which is private to our uid.
        val cp = shell.exec("cp ${staged.absolutePath} $target", timeoutMillis = 30_000L)
        if (!cp.isSuccess) {
            staged.delete()
            return@withContext SpoofResult.Failed(
                "cp to /data/local/tmp failed: ${cp.stderr.firstOrNull().orEmpty()}",
            )
        }

        val install = shell.exec("pm install --replace $target", timeoutMillis = 60_000L)
        staged.delete()
        // Best-effort cleanup of the staged copy.
        shell.exec("rm -f $target")

        if (!install.isSuccess) {
            return@withContext SpoofResult.Failed(
                "pm install failed: ${install.stderr.firstOrNull() ?: install.stdout.lastOrNull().orEmpty()}",
            )
        }

        // Verify with `pm path`.
        val verify = shell.exec("pm path $MODULE_PACKAGE", timeoutMillis = 5_000L)
        if (!verify.isSuccess || verify.stdout.none { it.startsWith("package:") }) {
            return@withContext SpoofResult.Failed("pm path did not confirm the install")
        }

        SpoofResult.Ok
    }

    suspend fun uninstall(): SpoofResult = withContext(Dispatchers.IO) {
        val r = shell.exec("pm uninstall $MODULE_PACKAGE", timeoutMillis = 30_000L)
        if (r.isSuccess) SpoofResult.Ok
        else SpoofResult.Failed("pm uninstall failed: ${r.stderr.firstOrNull().orEmpty()}")
    }

    companion object {
        const val MODULE_PACKAGE = "com.gadget.spoofer.xposed"
        const val ASSET_NAME = "lsposed-spoofer.apk"

        /** Lookup name of the LSPosed Manager package. */
        const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"
    }
}
