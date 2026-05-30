package dev.ranzlappen.gadget.core.root.core

/**
 * Snapshot of the active root provider at probe time. `versionName` /
 * `versionCode` are best-effort — providers that don't expose a version
 * string surface as null.
 */
data class RootProviderInfo(
    val provider: RootProvider,
    val versionName: String?,
    val versionCode: Long?,
)
