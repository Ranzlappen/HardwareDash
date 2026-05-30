package dev.ranzlappen.gadget.core.root.core

/**
 * Thin holder around libsu's `Shell.RootService` binder. Batch 2 only declares
 * the bind/unbind contract — typed AIDL-equivalent methods (file ops,
 * privileged getters, etc.) are added by later batches that route specific
 * operations through the service. The standard flavor returns null on bind.
 */
interface RootService {
    suspend fun bind(): RootServiceHandle?
    suspend fun unbind()
}

/**
 * Opaque handle returned by [RootService.bind]. Future batches replace the
 * marker interface with a typed remote facade.
 */
interface RootServiceHandle {
    val isAlive: Boolean
}
