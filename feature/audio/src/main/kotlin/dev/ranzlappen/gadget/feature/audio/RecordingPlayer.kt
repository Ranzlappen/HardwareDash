package dev.ranzlappen.gadget.feature.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays one recording at a time (v3 B10). [playing] is the URI being played,
 * or null; it clears itself when playback ends or fails. Owned by
 * [AudioViewModel], which calls [release] in `onCleared`.
 */
class RecordingPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null
    private val _playing = MutableStateFlow<Uri?>(null)
    val playing: StateFlow<Uri?> = _playing.asStateFlow()

    /** Starts [uri], or stops it if it is the one already playing. */
    fun toggle(uri: Uri) {
        val wasPlaying = _playing.value
        stop()
        if (wasPlaying == uri) return
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mp.setDataSource(context, uri)
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, _, _ -> stop(); true }
            mp.setOnPreparedListener { it.start() }
            player = mp
            _playing.value = uri
            mp.prepareAsync()
        } catch (_: Exception) {
            mp.release()
            player = null
            _playing.value = null
        }
    }

    fun stop() {
        player?.let {
            try { it.stop() } catch (_: IllegalStateException) {}
            it.release()
        }
        player = null
        _playing.value = null
    }

    fun release() = stop()
}
