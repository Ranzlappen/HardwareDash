package com.hardwaredash.ui.screens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TorchScreen() {
    val context = LocalContext.current
    var torchOn  by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Check for flash hardware on first composition
    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        hasFlash = cm.cameraIdList.any { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    // Keep torch state in sync with actual hardware
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchOn = enabled
            }
            override fun onTorchModeUnavailable(cameraId: String) {
                torchOn = false
                errorMsg = "Torch unavailable — camera in use?"
            }
        }
        cm.registerTorchCallback(callback, null)
        onDispose { cm.unregisterTorchCallback(callback) }
    }

    // Pulsing glow animation when torch is on
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (torchOn) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowColor by animateColorAsState(
        targetValue = if (torchOn) Color(0xFFFFEB3B) else Color(0xFF37474F),
        animationSpec = tween(300),
        label = "glowColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Torch / Flashlight",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFlash) "Flash hardware detected" else "No flash hardware on this device",
            style = MaterialTheme.typography.bodyMedium,
            color = if (hasFlash) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(48.dp))

        // ── Animated glow circle ─────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(glowColor.copy(alpha = 0.25f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector        = if (torchOn) Icons.Default.FlashlightOn
                                         else        Icons.Default.FlashlightOff,
                    contentDescription = "Torch icon",
                    tint               = if (torchOn) Color(0xFFFFEB3B) else Color.Gray,
                    modifier           = Modifier.size(64.dp),
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── Toggle button ────────────────────────────────────────────────────
        Button(
            enabled = hasFlash,
            onClick = {
                errorMsg = null
                try {
                    val cm  = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    // Find the first back camera with flash
                    val cid = cm.cameraIdList.first { id ->
                        cm.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                    cm.setTorchMode(cid, !torchOn)
                } catch (e: Exception) {
                    errorMsg = e.message
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (torchOn) MaterialTheme.colorScheme.primary
                                 else         MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
        ) {
            Text(
                if (torchOn) "Turn OFF" else "Turn ON",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        errorMsg?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Uses CameraManager.setTorchMode() — no CAMERA permission needed for torch alone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
