package dev.ranzlappen.gadget.feature.actuators

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ActuatorsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _state = MutableStateFlow(
        ActuatorsState(
            vibratorAvailable = vibrator?.hasVibrator() == true,
            hasAmplitudeControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                vibrator?.hasAmplitudeControl() == true,
            isRootedFlavor = rootCapabilityRegistry.isRootedFlavor,
        ),
    )
    val state: StateFlow<ActuatorsState> = _state
}
