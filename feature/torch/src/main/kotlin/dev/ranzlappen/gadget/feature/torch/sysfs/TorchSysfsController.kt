package dev.ranzlappen.gadget.feature.torch.sysfs

/**
 * Privileged sysfs Torch capability surface — the low-level LED-driver access
 * the **rooted** flavor drives directly (`/sys/class/leds/...`). This is the
 * *current* rooted-tier contract, not deprecated code: the standard-flavor
 * implementation always returns [TorchSysfsControllerResult.Unsupported] so
 * shared UI can use one code path for both flavors.
 *
 * Every method routes through [dev.ranzlappen.gadget.core.root.RootSafetyGate] before doing
 * anything privileged — there is no "fast path" that bypasses the gate.
 */
interface TorchSysfsController {

    /**
     * Drives the LED brightness sysfs node directly. [percent] is interpreted
     * as a fraction of `max_brightness` reported by the driver — values above
     * 100 push past the stock cap up to a hard ceiling enforced by the
     * implementation (currently 150 %).
     */
    suspend fun boostBrightness(percent: Int): TorchSysfsControllerResult

    /**
     * Strobes the LED with independent on/off durations, allowing low-duty
     * pulses at high frequency for thermal headroom (e.g. 50 Hz at 10 %
     * duty). [phaseOffsetMillis] is reserved for the multi-LED variant.
     * Bounded by the implementation's coroutine-driven loop and a
     * [withTimeout] of [durationMillis].
     */
    suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
        phaseOffsetMillis: Long = 0L,
    ): TorchSysfsControllerResult

    /**
     * Lights every available emitter at once: front + back LEDs, notification
     * RGB LED, and optionally screen-as-flashlight. Auto-cuts off after
     * [durationMillis] regardless of the limiter.
     */
    suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean = false,
    ): TorchSysfsControllerResult

    /**
     * Disables OS thermal throttling for the LED driver thermal zone for the
     * duration of [block]. The implementation enforces a HARD 45-second
     * absolute ceiling — callers cannot extend it. The original mode is
     * restored in a `finally` block even if [block] throws or any monitored
     * thermal zone breaches its trip point.
     */
    suspend fun withThermalOverride(
        durationMillis: Long,
        block: suspend () -> Unit,
    ): TorchSysfsControllerResult
}
