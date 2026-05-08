package com.gadget.root

import javax.inject.Qualifier

/**
 * Qualifies the [androidx.datastore.core.DataStore] used by the rooted-features
 * safety framework. Lives in `src/main` so both flavors compile against the
 * annotation; only the rooted flavor actually provides a binding for it.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RootSafetyPrefs
