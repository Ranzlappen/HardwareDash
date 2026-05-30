package com.gadget.microphone

import android.content.Context
import dev.ranzlappen.gadget.core.root.core.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val SYSTEM_AUDIO_HARD_CEILING_MILLIS = 5L * 60L * 1000L
private const val CAPTURE_AUDIO_OUTPUT_PERMISSION = "android.permission.CAPTURE_AUDIO_OUTPUT"

/**
 * Enables system-audio loopback by granting the system-only
 * `CAPTURE_AUDIO_OUTPUT` permission to this app via `pm grant`. The
 * permission is revoked in a `NonCancellable` finally so a cancelled
 * coroutine cannot leave the elevated permission attached to the app
 * across the user's next app session.
 *
 * **Mandatory legal warning** — this is wired with `requiresExplicitConfirm`
 * on the descriptor so the user sees the warning at least once.
 *
 * The actual recording surface (an `AudioPlaybackCaptureConfiguration` or
 * a system loopback consumer) is left to a follow-up batch — this method
 * exercises the privilege flip and confirms it is reversible.
 */
@Singleton
class SystemAudioCapture @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: RootShell,
) {
    suspend fun grantThenWait(durationMillis: Long): MicrophoneControllerResult {
        val effectiveDuration = durationMillis.coerceAtMost(SYSTEM_AUDIO_HARD_CEILING_MILLIS)
        val packageName = context.packageName
        val grant = shell.exec("pm grant $packageName $CAPTURE_AUDIO_OUTPUT_PERMISSION")
        if (!grant.isSuccess) {
            return MicrophoneControllerResult.HardwareError(
                "pm grant failed: ${grant.stderr.firstOrNull().orEmpty()}",
            )
        }
        return try {
            delay(effectiveDuration)
            MicrophoneControllerResult.Ok
        } finally {
            withContext(NonCancellable) {
                shell.exec("pm revoke $packageName $CAPTURE_AUDIO_OUTPUT_PERMISSION")
            }
        }
    }
}
