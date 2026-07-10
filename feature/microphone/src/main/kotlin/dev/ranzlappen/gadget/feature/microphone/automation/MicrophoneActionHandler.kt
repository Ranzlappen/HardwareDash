package dev.ranzlappen.gadget.feature.microphone.automation

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
import dev.ranzlappen.gadget.feature.microphone.R
import dev.ranzlappen.gadget.feature.microphone.control.CustomRateConfig
import dev.ranzlappen.gadget.feature.microphone.control.DirectPcmConfig
import dev.ranzlappen.gadget.feature.microphone.control.GainBoostConfig
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneControllerResult
import dev.ranzlappen.gadget.feature.microphone.control.MultiMicConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Microphone's invocable-action surface for the future automation tool.
 *
 * [MicrophoneController] plays the same role here that `CameraController`
 * plays for `:feature:camera`: a single interface, standard-flavor-bound to
 * a no-op `StandardMicrophoneController` and rooted-flavor-bound to
 * `RootedMicrophoneController` via the app-level `:core:root` Hilt seam
 * (`RootBindings`), so this handler injects it directly and never branches
 * on `BuildConfig.IS_ROOTED`. Every one of its six actions carries
 * `requiresRoot = true` — there is genuinely zero standard-tier
 * functionality, matching `StandardMicrophoneController`'s all-`Unsupported`
 * contract — and [MicrophoneControllerResult] maps onto [ActionResult] with
 * the same shape `CameraActionHandler` uses for `CameraControllerResult`.
 *
 * The custom-sample-rate and system-audio-capture actions carry a mandatory
 * confirm dialog **in the screen UI** (kernel-lockup risk / call-recording
 * legality respectively) — that's a screen-level affordance, not an
 * automation-engine concept. An automation rule firing these two dispatches
 * straight through, exactly like `CameraActionHandler.ACTION_SET_SHUTTER_SOUND`
 * bypasses its own screen's confirm; the real gate for unattended rooted
 * calls is `RootSafetyGate` + each `RootFeatureDescriptor`'s
 * `requiresExplicitConfirm`-gated Settings opt-in, not a screen dialog no one
 * is present to see.
 */
@Singleton
class MicrophoneActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: MicrophoneController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_GAIN_BOOST,
            label = context.getString(R.string.microphone_action_gain_boost),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_BOOST_DB, ActionParamType.Int, "10", 0f, 30f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "5000", 0f, 60_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_DIRECT_PCM,
            label = context.getString(R.string.microphone_action_direct_pcm),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_SAMPLE_RATE, ActionParamType.Int, "48000"),
                ActionParam(PARAM_CHANNEL_COUNT, ActionParamType.Int, "1", 1f, 8f),
                ActionParam(PARAM_BITS_PER_SAMPLE, ActionParamType.Int, "16"),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "3000", 5f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_CUSTOM_SAMPLE_RATE,
            label = context.getString(R.string.microphone_action_custom_rate),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_TARGET_SAMPLE_RATE, ActionParamType.Int, "192000", 4_000f, 384_000f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "5000", 0f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_MULTI_MIC_RAW,
            label = context.getString(R.string.microphone_action_multi_mic),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "10000", 0f, 30_000f),
                ActionParam(PARAM_MAX_STREAMS, ActionParamType.Int, "3", 1f, 3f),
            ),
        ),
        ModuleAction(
            key = ACTION_DISABLE_EFFECTS,
            label = context.getString(R.string.microphone_action_disable_effects),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_SYSTEM_AUDIO_CAPTURE,
            label = context.getString(R.string.microphone_action_system_audio),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "60000", 5_000f, 300_000f)),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_GAIN_BOOST -> controller.gainBoost(
                GainBoostConfig(
                    boostDb = params.intOr(PARAM_BOOST_DB, 10),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 5_000),
                ),
            ).toActionResult()
            ACTION_DIRECT_PCM -> controller.directPcm(
                DirectPcmConfig(
                    sampleRate = params.intOr(PARAM_SAMPLE_RATE, 48_000),
                    channelCount = params.intOr(PARAM_CHANNEL_COUNT, 1),
                    bitsPerSample = params.intOr(PARAM_BITS_PER_SAMPLE, 16),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 3_000),
                ),
            ).toActionResult()
            ACTION_CUSTOM_SAMPLE_RATE -> controller.customSampleRate(
                CustomRateConfig(
                    targetSampleRate = params.intOr(PARAM_TARGET_SAMPLE_RATE, 192_000),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 5_000),
                ),
            ).toActionResult()
            ACTION_MULTI_MIC_RAW -> controller.multiMicRaw(
                MultiMicConfig(
                    durationMillis = params.longOr(PARAM_DURATION_MS, 10_000),
                    maxStreams = params.intOr(PARAM_MAX_STREAMS, 3),
                ),
            ).toActionResult()
            ACTION_DISABLE_EFFECTS -> controller.disableEffects().toActionResult()
            ACTION_SYSTEM_AUDIO_CAPTURE -> controller.systemAudioCapture(
                params.longOr(PARAM_DURATION_MS, 60_000),
            ).toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun MicrophoneControllerResult.toActionResult(): ActionResult = when (this) {
        MicrophoneControllerResult.Ok -> ActionResult.Success
        MicrophoneControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        MicrophoneControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is MicrophoneControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        is MicrophoneControllerResult.HardwareError -> ActionResult.Failure(message)
    }

    private fun Map<String, String>.intOr(key: String, fallback: Int): Int =
        this[key]?.toIntOrNull() ?: fallback

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long =
        this[key]?.toLongOrNull() ?: fallback

    companion object {
        const val FEATURE_ID = "microphone"
        const val ACTION_GAIN_BOOST = "gain_boost"
        const val ACTION_DIRECT_PCM = "direct_pcm"
        const val ACTION_CUSTOM_SAMPLE_RATE = "custom_sample_rate"
        const val ACTION_MULTI_MIC_RAW = "multi_mic_raw"
        const val ACTION_DISABLE_EFFECTS = "disable_effects"
        const val ACTION_SYSTEM_AUDIO_CAPTURE = "system_audio_capture"
        const val PARAM_BOOST_DB = "boost_db"
        const val PARAM_DURATION_MS = "duration_ms"
        const val PARAM_SAMPLE_RATE = "sample_rate"
        const val PARAM_CHANNEL_COUNT = "channel_count"
        const val PARAM_BITS_PER_SAMPLE = "bits_per_sample"
        const val PARAM_TARGET_SAMPLE_RATE = "target_sample_rate"
        const val PARAM_MAX_STREAMS = "max_streams"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface MicrophoneActionModule {

    @Binds
    @IntoMap
    @StringKey(MicrophoneActionHandler.FEATURE_ID)
    fun bindMicrophoneActionHandler(handler: MicrophoneActionHandler): ActionHandler
}
