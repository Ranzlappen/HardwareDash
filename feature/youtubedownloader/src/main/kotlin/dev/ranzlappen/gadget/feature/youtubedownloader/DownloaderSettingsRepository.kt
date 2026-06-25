package dev.ranzlappen.gadget.feature.youtubedownloader

import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's last-used [DownloadConfig] so the form restores across
 * launches. Backed by a single-record [dev.ranzlappen.gadget.core.datastore.FeaturePreferences]
 * collection keyed at [KEY].
 */
@Singleton
class DownloaderSettingsRepository @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val prefs = factory.create(
        fileName = "youtube_downloader",
        keyPrefix = "config_",
        serializer = DownloadConfig.serializer(),
    )

    /** The last saved config, or defaults when nothing has been stored yet. */
    val lastConfig: Flow<DownloadConfig> = prefs.all.map { it[KEY] ?: DownloadConfig() }

    suspend fun save(config: DownloadConfig) = prefs.save(KEY, config)

    private companion object {
        const val KEY = 0
    }
}
