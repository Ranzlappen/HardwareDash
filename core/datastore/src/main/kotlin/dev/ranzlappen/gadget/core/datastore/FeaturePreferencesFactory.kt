package dev.ranzlappen.gadget.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-provided factory that creates [FeaturePreferences] instances.
 *
 * Each feature module that needs to persist a small collection of
 * structured records does so by injecting this factory in its own
 * `@Module` and calling [create]. Example wiring from a feature
 * module:
 *
 * ```kotlin
 * @Module @InstallIn(SingletonComponent::class)
 * object TorchWidgetDataModule {
 *     @Provides @Singleton
 *     fun provideTorchWidgetPrefs(factory: FeaturePreferencesFactory) =
 *         factory.create(
 *             fileName = "torch_widgets",
 *             keyPrefix = "widget_",
 *             serializer = TorchWidgetConfig.serializer(),
 *         )
 * }
 * ```
 *
 * The factory caches one DataStore per `fileName` so multiple
 * `create(...)` calls with the same file name return the same
 * underlying store (idempotent — Hilt's `@Singleton` scope ensures
 * the factory itself is unique per process). The cache is keyed by
 * `fileName` only because per-AGP-AGP `preferencesDataStoreFile`
 * crashes if the same file path is opened by two different
 * DataStore instances.
 */
@Singleton
class FeaturePreferencesFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val stores: MutableMap<String, DataStore<Preferences>> = mutableMapOf()
    private val storesLock = Any()

    /** Shared scope for DataStore disk IO. SupervisorJob so one feature's
     *  IO error doesn't tear down sibling features' stores. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Build a [FeaturePreferences] backed by the named DataStore file.
     *
     * @param fileName Stable file name for the DataStore. Becomes
     *                 `<files>/datastore/<fileName>.preferences_pb`
     *                 on disk. Must be unique across features —
     *                 collision means two features clobbering each
     *                 other's writes.
     * @param keyPrefix Namespace prefix for keys inside the
     *                  DataStore. Lets one file host multiple
     *                  collections (e.g. `"widget_"` + `"shortcut_"`)
     *                  without overlap. Most callers use one file
     *                  per collection and pass a descriptive prefix
     *                  anyway for readable disk inspection.
     * @param serializer kotlinx.serialization serializer for the
     *                   `@Serializable` value type.
     */
    fun <T : Any> create(
        fileName: String,
        keyPrefix: String,
        serializer: KSerializer<T>,
    ): FeaturePreferences<T> = FeaturePreferences(
        dataStore = obtainDataStore(fileName),
        keyPrefix = keyPrefix,
        serializer = serializer,
        json = sharedJson,
    )

    private fun obtainDataStore(fileName: String): DataStore<Preferences> =
        synchronized(storesLock) {
            stores.getOrPut(fileName) {
                PreferenceDataStoreFactory.create(
                    scope = ioScope,
                    produceFile = { context.preferencesDataStoreFile(fileName) },
                )
            }
        }

    private companion object {
        /**
         * Shared [Json] configuration for every feature's encoder /
         * decoder. `ignoreUnknownKeys = true` so schema bumps that
         * add fields don't reject older on-disk values; the new
         * field falls back to its data-class default until rewritten.
         */
        val sharedJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
