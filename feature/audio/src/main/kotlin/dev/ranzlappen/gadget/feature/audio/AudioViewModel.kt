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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val analyzer: MicAnalyzer,
    private val recordings: Recordings,
    private val player: RecordingPlayer,
    private val audioRoutingController: AudioRoutingController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioState(isRootedFlavor = rootCapabilityRegistry.isRootedFlavor))
    val state: StateFlow<AudioState> = _state

    /**
     * Live level + spectrum. The mic is only open while the screen collects
     * this (lifecycle-aware) and RECORD_AUDIO is granted; it closes 2 s after
     * the screen leaves the foreground.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val live: StateFlow<MicFrame?> = _state
        .map { it.permissionGranted }
        .distinctUntilChanged()
        .flatMapLatest { granted -> if (granted) analyzer.frames() else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(LIVE_STOP_TIMEOUT_MS), null)

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
            recorder.lastSaved.filterNotNull().collect { refreshRecordings() }
        }
        viewModelScope.launch {
            player.playing.collect { uri -> _state.update { it.copy(playingUri = uri) } }
        }
        refreshRecordings()
    }

    fun refreshRecordings() {
        viewModelScope.launch {
            val list = recordings.list()
            _state.update { it.copy(recordings = list) }
        }
    }

    fun togglePlayback(recording: Recording) = player.toggle(recording.uri)

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            if (player.playing.value == recording.uri) player.stop()
            if (recordings.delete(recording.uri)) {
                _state.update { s -> s.copy(recordings = s.recordings - recording, error = null) }
            } else {
                _state.update { it.copy(error = DELETE_FAILED) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    override fun onCleared() {
        player.release()
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

    companion object {
        private const val LIVE_STOP_TIMEOUT_MS = 2_000L
        internal const val DELETE_FAILED = "delete_failed"
    }
}
