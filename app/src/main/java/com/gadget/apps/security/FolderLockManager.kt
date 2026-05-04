package com.gadget.apps.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps `androidx.biometric.BiometricPrompt` for the App-Organizer's hidden
 * folders. Uses `BIOMETRIC_WEAK` only so it works cleanly on minSdk 29 — no
 * device-credential mixing (which has API-level pitfalls). Devices without
 * any biometric enrollment fall through to `onUnavailable`.
 */
@Singleton
class FolderLockManager @Inject constructor() {

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val status = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric prompt. Calls [onSuccess] iff auth succeeds; calls
     * [onCancel] for any non-success outcome (cancel, error, lockout). The
     * caller decides whether to finish the Activity or show a fallback.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cancelLabel: String,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onCancel()
            }
            override fun onAuthenticationFailed() {
                // Single failed attempt — let the prompt retry rather than
                // dismissing immediately. The system enforces lockout.
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(cancelLabel)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}
