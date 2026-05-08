package com.gadget.root.core

/**
 * Outcome of a one-shot root probe. `Available` carries provider metadata so
 * UI can surface "Magisk 30.6 detected" without re-querying. `AvailableButDenied`
 * means a known root manager is installed but actively denied this app's
 * request — the user can fix this by visiting their root manager's settings.
 */
sealed class RootDetection {
    data object None : RootDetection()
    data class AvailableButDenied(val provider: RootProvider) : RootDetection()
    data class Available(
        val info: RootProviderInfo,
    ) : RootDetection()
}
