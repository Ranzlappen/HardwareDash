package dev.ranzlappen.gadget.feature.lock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class LockMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<LockState> = _state.asStateFlow()

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addAction(Intent.ACTION_SCREEN_ON)
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                _state.value = readState()
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun readState(): LockState {
        val isLocked = keyguard?.isKeyguardLocked == true
        val isSecure = keyguard?.isDeviceSecure == true
        val hasBiometric = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
        return LockState(isLocked = isLocked, isSecure = isSecure, hasBiometric = hasBiometric)
    }
}
