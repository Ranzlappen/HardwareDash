package dev.ranzlappen.gadget.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The centralized grant-state authority (W5). Aggregates every feature's
 * `@IntoMap`-contributed [FeaturePermissions] with a built-in **app-wide
 * baseline catalog** (the common runtime + special permissions the manifest
 * declares), so the Permissions dashboard shows a complete picture even
 * before individual features contribute their own entries. Grant checks are
 * pure `ContextCompat.checkSelfPermission` / [SpecialPermissions] reads —
 * cheap, so callers re-query on `ON_RESUME`.
 */
@Singleton
class PermissionRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contributions: Map<String, @JvmSuppressWildcards FeaturePermissions>,
) {

    /**
     * The feature permission groups to display: the built-in baseline plus
     * every contributed group, sorted by display name. Runtime permissions
     * are filtered to the ones that apply to the current SDK.
     */
    fun featurePermissions(): List<FeaturePermissions> {
        val all = (contributions.values + baselineCatalog())
        return all
            .map { group ->
                group.copy(
                    runtime = group.runtime.filter { it.appliesToCurrentSdk() },
                )
            }
            .filter { it.runtime.isNotEmpty() || it.special.isNotEmpty() }
            .sortedBy { it.displayName }
    }

    fun isRuntimeGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isSpecialGranted(special: SpecialPermission): Boolean =
        SpecialPermissions.isGranted(context, special)

    /** granted / total across every applicable runtime + special permission. */
    fun grantSummary(): GrantSummary {
        val groups = featurePermissions()
        val runtime = groups.flatMap { it.runtime }.map { it.permission }.distinct()
        val special = groups.flatMap { it.special }.distinct()
        val granted = runtime.count { isRuntimeGranted(it) } + special.count { isSpecialGranted(it) }
        return GrantSummary(granted = granted, total = runtime.size + special.size)
    }

    private fun RuntimePermission.appliesToCurrentSdk(): Boolean =
        Build.VERSION.SDK_INT in minSdk..maxSdk

    /**
     * The app-wide baseline the dashboard always shows — the common
     * dangerous runtime permissions the manifest declares plus the special
     * permissions the app can request. Deliberately data-only so it's
     * testable without a live graph.
     */
    private fun baselineCatalog(): List<FeaturePermissions> = listOf(
        FeaturePermissions(
            featureId = "app",
            displayName = "App-wide",
            runtime = listOf(
                RuntimePermission(
                    Manifest.permission.CAMERA,
                    "Camera",
                    "Barcode scanning and camera diagnostics.",
                ),
                RuntimePermission(
                    Manifest.permission.RECORD_AUDIO,
                    "Microphone",
                    "dB meter and voice recording.",
                ),
                RuntimePermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    "Location",
                    "GPS position, speed, and altitude.",
                ),
                RuntimePermission(
                    Manifest.permission.READ_PHONE_STATE,
                    "Phone state",
                    "SIM and cellular network readouts.",
                ),
                RuntimePermission(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    "Bluetooth",
                    "Bonded-device and GATT readouts.",
                    minSdk = Build.VERSION_CODES.S,
                ),
                RuntimePermission(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    "Activity recognition",
                    "Step counter and motion detection.",
                    minSdk = Build.VERSION_CODES.Q,
                ),
                RuntimePermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "Notifications",
                    "Monitoring, automation, and reminder notifications.",
                    minSdk = Build.VERSION_CODES.TIRAMISU,
                ),
            ),
            special = listOf(
                SpecialPermission.Overlay,
                SpecialPermission.ExactAlarm,
                SpecialPermission.WriteSettings,
            ),
        ),
    )
}

/** granted / total permission tally for the dashboard summary. */
data class GrantSummary(val granted: Int, val total: Int) {
    val allGranted: Boolean get() = granted >= total
    val missing: Int get() = (total - granted).coerceAtLeast(0)
}
