package com.gadget

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gadget.ui.theme.GadgetTheme
import timber.log.Timber

class CallerScreenActivity : ComponentActivity() {

    private var ringtone: android.media.Ringtone? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()

        val durationSec = intent.getIntExtra(EXTRA_DURATION, 30)

        // Start ringing
        startRinging()

        // Auto-stop after duration
        handler.postDelayed({ stopAndFinish() }, durationSec * 1000L)

        setContent {
            GadgetTheme {
                CallerScreen(
                    onDecline = { stopAndFinish() },
                )
            }
        }
    }

    private fun startRinging() {
        try {
            val prefs = getSharedPreferences("widget_settings", MODE_PRIVATE)
            val bypassDnd = prefs.getBoolean("bypass_dnd", false)

            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            if (bypassDnd) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 0
            try {
                val stream = if (bypassDnd) AudioManager.STREAM_ALARM else AudioManager.STREAM_RING
                audioManager?.setStreamVolume(
                    stream,
                    audioManager?.getStreamMaxVolume(stream) ?: 7,
                    0,
                )
            } catch (e: SecurityException) {
                Timber.w(e, "Setting ring/alarm volume blocked by DND/policy")
            }
            ringtone?.play()
        } catch (e: Exception) {
            Timber.w(e, "Ringtone init failed; degrading gracefully")
        }
    }

    private fun stopAndFinish() {
        handler.removeCallbacksAndMessages(null)
        try { ringtone?.stop() } catch (e: Exception) { Timber.w(e, "Ringtone stop failed") }
        try { audioManager?.setStreamVolume(AudioManager.STREAM_RING, originalVolume, 0) } catch (e: Exception) { Timber.w(e, "Restore volume failed") }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try { ringtone?.stop() } catch (_: Exception) {}
        try { audioManager?.setStreamVolume(AudioManager.STREAM_RING, originalVolume, 0) } catch (_: Exception) {}
    }

    companion object {
        const val EXTRA_DURATION = "ring_duration_seconds"
    }
}

@Composable
private fun CallerScreen(onDecline: () -> Unit) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B2F)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(80.dp))

            // Pulsing circle with phone icon
            Box(contentAlignment = Alignment.Center) {
                // Outer pulse ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = pulseAlpha)),
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(56.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Caller name
            Text(
                "Gadget",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            // Subtitle
            Text(
                "Incoming Call",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
            )

            Spacer(Modifier.weight(1f))

            // Decline button
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Text(
                "Stop Ringing",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}
