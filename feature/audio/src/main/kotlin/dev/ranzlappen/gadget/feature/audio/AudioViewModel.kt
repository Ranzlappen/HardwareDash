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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: AudioRecorder,
    private val dbMeter: DbMeterMetricSource,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state

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
