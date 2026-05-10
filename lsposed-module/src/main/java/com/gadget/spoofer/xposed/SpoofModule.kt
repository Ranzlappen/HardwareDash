package com.gadget.spoofer.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Single-class LSPosed module entry point. Listed in `assets/xposed_init`
 * as `com.gadget.spoofer.xposed.SpoofModule`.
 *
 * Hooks installed (per-process, every loaded package the user has scoped to
 * us in LSPosed Manager):
 *
 *   1. `Location.isFromMockProvider()` → returns `false`.
 *   2. `Location.isMock()` (API 31+) → returns `false`.
 *   3. `Location.getExtras()` → strips the `mockLocation` extra HardwareDash
 *      sets so consumers that read raw extras don't see the bit either.
 *   4. `AppOpsManager.checkOp(MOCK_LOCATION, ...)` and friends → return
 *      MODE_IGNORED so apps that enumerate AppOps to detect mock-location
 *      apps see nothing.
 *   5. `Settings.Secure.getStringForUser("mock_location", …)` → returns "0".
 *   6. (optional, future) GnssStatus.Callback hook synthesizing satellites.
 *
 * Also writes the LsposedHandshake sentinel into HardwareDash's process so
 * the app can detect "module is loaded".
 */
class SpoofModule : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Handshake — when our hook initializer runs in the HardwareDash
        // process, write the sentinel into LsposedHandshake.loadedSentinel
        // so the app knows we're alive.
        if (lpparam.packageName == TARGET_APP_STANDARD ||
            lpparam.packageName == TARGET_APP_ROOTED
        ) {
            try {
                val handshake = XposedHelpers.findClass(HANDSHAKE_FQN, lpparam.classLoader)
                XposedHelpers.setStaticObjectField(handshake, "loadedSentinel", EXPECTED_SENTINEL)
            } catch (t: Throwable) {
                XposedBridge.log("HardwareDash spoofer: handshake write failed: $t")
            }
        }

        installLocationHooks(lpparam.classLoader)
        installAppOpsHooks(lpparam.classLoader)
        installSettingsSecureHooks(lpparam.classLoader)
    }

    private fun installLocationHooks(loader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.location.Location", loader,
                "isFromMockProvider",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                },
            )
        } catch (t: Throwable) {
            XposedBridge.log("HardwareDash spoofer: hook isFromMockProvider failed: $t")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.location.Location", loader,
                    "isMock",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = false
                        }
                    },
                )
            } catch (t: Throwable) {
                XposedBridge.log("HardwareDash spoofer: hook isMock failed: $t")
            }
        }

        // Strip the "mockLocation" extra HardwareDash sets in LocationAdapter.
        try {
            XposedHelpers.findAndHookMethod(
                "android.location.Location", loader,
                "getExtras",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val extras = param.result as? android.os.Bundle ?: return
                        if (extras.containsKey("mockLocation")) {
                            extras.remove("mockLocation")
                        }
                    }
                },
            )
        } catch (t: Throwable) {
            XposedBridge.log("HardwareDash spoofer: hook getExtras failed: $t")
        }
    }

    private fun installAppOpsHooks(loader: ClassLoader) {
        // checkOp / unsafeCheckOp / checkOpNoThrow / unsafeCheckOpNoThrow.
        // Different API levels expose different methods; iterate and hook
        // any that match the (String, Int, String) signature.
        val opStrings = setOf("android:mock_location", "MOCK_LOCATION")
        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val firstArg = param.args.firstOrNull()?.toString() ?: return
                if (opStrings.any { firstArg.contains(it, ignoreCase = true) }) {
                    // MODE_IGNORED = 1 in AppOpsManager.
                    param.result = 1
                }
            }
        }
        for (name in arrayOf("checkOp", "checkOpNoThrow", "unsafeCheckOp", "unsafeCheckOpNoThrow")) {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.AppOpsManager", loader, name,
                    String::class.java, Int::class.javaPrimitiveType, String::class.java,
                    hook,
                )
            } catch (_: NoSuchMethodError) {
            } catch (_: Throwable) {
                // Other overloads exist on some platforms; skip silently.
            }
        }
    }

    private fun installSettingsSecureHooks(loader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Secure", loader,
                "getStringForUser",
                "android.content.ContentResolver", String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val name = param.args[1] as? String ?: return
                        if (name == "mock_location") {
                            param.result = "0"
                        }
                    }
                },
            )
        } catch (_: Throwable) {
            // Method missing on some platforms; non-fatal.
        }
    }

    companion object {
        // Standard / rooted package names — both are valid HardwareDash
        // installs and we hook the handshake into either.
        private const val TARGET_APP_STANDARD = "com.gadget"
        private const val TARGET_APP_ROOTED = "com.gadget.root"

        // Must match LsposedHandshake.EXPECTED_SENTINEL in the app.
        private const val HANDSHAKE_FQN = "com.gadget.gps.spoof.LsposedHandshake"
        private const val EXPECTED_SENTINEL = "hwd-spoofer-v1-loaded"
    }
}
