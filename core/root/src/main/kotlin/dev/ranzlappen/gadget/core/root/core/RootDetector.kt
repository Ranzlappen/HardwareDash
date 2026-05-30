package dev.ranzlappen.gadget.core.root.core

/**
 * Probes the device for an active root provider. Implementations MUST be
 * idempotent and cache their result — the rooted flavor calls libsu's
 * `Shell.isAppGrantedRoot()` at most once per process to avoid repeatedly
 * triggering the user's root-manager prompt.
 */
interface RootDetector {
    suspend fun detect(): RootDetection
}
