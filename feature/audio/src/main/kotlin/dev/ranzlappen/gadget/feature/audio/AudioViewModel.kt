package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.audio.control.AudioRoutingController
import dev.ranzlappen.gadget.feature.audio.control.AudioRoutingControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the audio screen (W6 in-screen surface). */
data class AudioRootToolsState(
    val audioPolicy: RootActionState = RootActionState(),
)

@HiltViewModel
class AudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: AudioRecorder,
    private val dbMeter: DbMeterMetricSource,
    private val audioRoutingController: AudioRoutingController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioState(isRootedFlavor = rootCapabilityRegistry.isRootedFlavor))
    val state: StateFlow<AudioState> = _state

    private val _rootTools = MutableStateFlow(AudioRootToolsState())

    /** Live status of the rooted read-only audio-policy dump. */
    val rootTools: StateFlow<AudioRootToolsState> = _rootTools.asStateFlow()

    fun onDumpAudioPolicy() {
        viewModelScope.launch {
            _rootTools.update { it.copy(audioPolicy = it.audioPolicy.copy(running = true)) }
            val result = audioRoutingController.dumpAudioPolicy()
            _rootTools.update { it.copy(audioPolicy = result.toActionState()) }
        }
    }

    private fun AudioRoutingControllerResult.toActionState(): RootActionState = when (this) {
        is AudioRoutingControllerResult.AudioDumpExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars of audio policy")
        is AudioRoutingControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        AudioRoutingControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        AudioRoutingControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is AudioRoutingControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        is AudioRoutingControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is AudioRoutingControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is AudioRoutingControllerResult.VolumeSnapshot ->
            RootActionState(message = "Volume ${appliedIndex}/${maxIndex}")
        is AudioRoutingControllerResult.RoutingSnapshot ->
            RootActionState(message = "Routed to $appliedTarget")
    }

    init {
        checkPermission()
        viewModelScope.launch {
            recorder.isRecording.collect { recording ->
                _state.update { it.copy(isRecording = recording) }
            }
        }
        viewModelScope.launch {
            dbMeter.stream()?.collect { db ->
                _state.update { it.copy(currentDbLevel = db) }
            }
        }
    }

    fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        _state.update { it.copy(permissionGranted = granted) }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
    }

    fun startRecording() {
        val intent = Intent(AudioRecordService.ACTION_START_RECORD).setPackage(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopRecording() {
        val intent = Intent(AudioRecordService.ACTION_STOP_RECORD).setPackage(context.packageName)
        context.startService(intent)
    }
}
