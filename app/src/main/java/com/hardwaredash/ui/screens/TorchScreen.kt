package com.hardwaredash.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.hardwaredash.localization.S
import kotlinx.coroutines.delay

@Composable
fun TorchScreen() {
    val context = LocalContext.current
    var torchOn  by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Strobe state
    var strobeActive by remember { mutableStateOf(false) }
    var strobeFreqHz by remember { mutableFloatStateOf(5f) }

    // Brightness state
    var brightness     by remember { mutableFloatStateOf(0.5f) }
    var autoBrightness by remember { mutableStateOf(false) }
    var hasWriteSettings by remember { mutableStateOf(false) }
    val strings = S.torch

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
                errorMsg = strings.torchUnavailable
            }
        }
        cm.registerTorchCallback(callback, null)
        onDispose { cm.unregisterTorchCallback(callback) }
    }

    // Poll brightness state
    LaunchedEffect(Unit) {
        while (true) {
            hasWriteSettings = Settings.System.canWrite(context)
            if (hasWriteSettings) {
                try {
                    val curBrightness = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        128
                    )
                    brightness = curBrightness / 255f
                    val mode = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                    autoBrightness = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                } catch (_: Exception) { }
            }
            delay(2000L)
        }
    }

    // Strobe loop
    LaunchedEffect(strobeActive, strobeFreqHz) {
        if (!strobeActive || !hasFlash) return@LaunchedEffect
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cid = try {
            cm.cameraIdList.first { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { return@LaunchedEffect }

        val halfPeriodMs = (500f / strobeFreqHz).toLong().coerceAtLeast(10L)
        try {
            while (strobeActive) {
                cm.setTorchMode(cid, true)
                delay(halfPeriodMs)
                cm.setTorchMode(cid, false)
                delay(halfPeriodMs)
            }
        } finally {
            try { cm.setTorchMode(cid, false) } catch (_: Exception) {}
        }
    }

    // Stop strobe when leaving screen
    DisposableEffect(Unit) {
        onDispose { strobeActive = false }
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            strings.title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFlash) strings.flashDetected else strings.noFlash,
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

        // ── Toggle buttons ───────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            // Turn ON/OFF
            Button(
                enabled = hasFlash && !strobeActive,
                onClick = {
                    errorMsg = null
                    try {
                        val cm  = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val cid = cm.cameraIdList.first { id ->
                            cm.getCameraCharacteristics(id)
                                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        }
                        cm.setTorchMode(cid, !torchOn)
                    } catch (e: Exception) {
                        errorMsg = e.message
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (torchOn) MaterialTheme.colorScheme.primary
                                     else         MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text(
                    if (torchOn) strings.turnOff else strings.turnOn,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Strobe
            Button(
                enabled = hasFlash,
                onClick = { strobeActive = !strobeActive },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (strobeActive) MaterialTheme.colorScheme.error
                                     else             MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text(
                    if (strobeActive) strings.stop else strings.strobe,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        // Strobe frequency slider
        if (strobeActive || hasFlash) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Strobe: ${"%.0f".format(strobeFreqHz)} Hz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Slider(
                value = strobeFreqHz,
                onValueChange = { strobeFreqHz = it },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.fillMaxWidth(0.7f),
            )
        }

        errorMsg?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            strings.torchNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )

        Spacer(Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ══════════════════════════════════════════════════════════════════════
        // Display Brightness Control
        // ══════════════════════════════════════════════════════════════════════
        Text(strings.displayBrightness, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (!hasWriteSettings) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.writeSettingsNeeded,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }) { Text(strings.grantWriteSettings) }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(strings.autoBrightness, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = autoBrightness, onCheckedChange = { auto ->
                    autoBrightness = auto
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                })
            }

            Text("Brightness: ${"%.0f".format(brightness * 100)}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = brightness,
                onValueChange = { v ->
                    brightness = v
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        (v * 255).toInt(),
                    )
                },
                enabled = !autoBrightness,
            )
        }
    }
}
