package dev.ranzlappen.gadget.feature.youtubedownloader.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadConfig
import dev.ranzlappen.gadget.feature.youtubedownloader.MediaKind
import dev.ranzlappen.gadget.feature.youtubedownloader.PlaylistScope
import dev.ranzlappen.gadget.feature.youtubedownloader.R
import dev.ranzlappen.gadget.feature.youtubedownloader.YoutubeDlEngine
import dev.ranzlappen.gadget.feature.youtubedownloader.service.DownloadLauncher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposes the downloader to the cross-feature automation engine, satisfying
 * the Module Authoring Contract's automation requirement. Rules can kick off a
 * URL/playlist download or cancel everything in flight.
 */
@Singleton
class DownloadActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: YoutubeDlEngine,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_DOWNLOAD_URL,
            label = context.getString(R.string.ytdl_action_download_url),
            params = listOf(
                ActionParam(PARAM_URL, ActionParamType.Text),
                ActionParam(PARAM_AUDIO_ONLY, ActionParamType.Bool, "false"),
            ),
        ),
        ModuleAction(
            key = ACTION_DOWNLOAD_PLAYLIST,
            label = context.getString(R.string.ytdl_action_download_playlist),
            params = listOf(
                ActionParam(PARAM_URL, ActionParamType.Text),
                ActionParam(PARAM_AUDIO_ONLY, ActionParamType.Bool, "false"),
            ),
        ),
        ModuleAction(
            key = ACTION_CANCEL_ALL,
            label = context.getString(R.string.ytdl_action_cancel_all),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_DOWNLOAD_URL -> enqueue(params, PlaylistScope.SINGLE)
            ACTION_DOWNLOAD_PLAYLIST -> enqueue(params, PlaylistScope.PLAYLIST)
            ACTION_CANCEL_ALL -> {
                engine.cancelAll()
                ActionResult.Success
            }
            else -> ActionResult.Unsupported
        }

    private fun enqueue(params: Map<String, String>, scope: PlaylistScope): ActionResult {
        val url = params[PARAM_URL]?.trim().orEmpty()
        if (url.isEmpty()) {
            return ActionResult.Failure(context.getString(R.string.ytdl_action_error_no_url))
        }
        val audioOnly = params[PARAM_AUDIO_ONLY]?.toBooleanStrictOrNull() ?: false
        val config = DownloadConfig(
            url = url,
            kind = if (audioOnly) MediaKind.AUDIO else MediaKind.VIDEO,
            scope = scope,
        )
        DownloadLauncher.enqueue(context, config)
        return ActionResult.Success
    }

    companion object {
        const val FEATURE_ID = "youtube_downloader"
        const val ACTION_DOWNLOAD_URL = "download_url"
        const val ACTION_DOWNLOAD_PLAYLIST = "download_playlist"
        const val ACTION_CANCEL_ALL = "cancel_all"
        const val PARAM_URL = "url"
        const val PARAM_AUDIO_ONLY = "audio_only"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DownloadActionModule {
    @Binds
    @IntoMap
    @StringKey(DownloadActionHandler.FEATURE_ID)
    fun bindDownloadActionHandler(handler: DownloadActionHandler): ActionHandler
}
