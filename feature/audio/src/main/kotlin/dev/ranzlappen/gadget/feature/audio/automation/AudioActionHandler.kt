package dev.ranzlappen.gadget.feature.audio.automation

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.audio.AudioRecordService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_START_RECORDING, label = "Start audio recording"),
        ModuleAction(key = ACTION_STOP_RECORDING, label = "Stop audio recording"),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        return when (actionKey) {
            ACTION_START_RECORDING -> {
                val intent = Intent(AudioRecordService.ACTION_START_RECORD).setPackage(context.packageName)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                    else context.startService(intent)
                    ActionResult.Success
                } catch (e: Exception) {
                    ActionResult.Failure(e.message ?: "Failed to start recording")
                }
            }
            ACTION_STOP_RECORDING -> {
                val intent = Intent(AudioRecordService.ACTION_STOP_RECORD).setPackage(context.packageName)
                context.startService(intent)
                ActionResult.Success
            }
            else -> ActionResult.Unsupported
        }
    }

    companion object {
        const val FEATURE_ID = "audio"
        const val ACTION_START_RECORDING = "audio_start_recording"
        const val ACTION_STOP_RECORDING = "audio_stop_recording"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(AudioActionHandler.FEATURE_ID)
    abstract fun bindAudioActionHandler(impl: AudioActionHandler): ActionHandler
}
