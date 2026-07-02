package dev.ranzlappen.gadget.feature.battery.control


/**
 * Rooted-only Battery capability surface. The standard-flavor
 * implementation always returns [BatteryControllerResult.Unsupported] so
 * shared UI uses one code path for both flavors.
 *
 * Every method routes through `dev.ranzlappen.gadget.core.root.RootSafetyGate` before
 * doing anything privileged. Hard cutoffs (active windows, thermal-zone
 * breach abort) are enforced inside the impl and cannot be extended by
 * callers.
 *
 * The interface deliberately exposes ONLY extreme-tier operations.
 * Baseline battery info continues to flow through `BatteryManager` in
 * `BatteryScreen` — this controller is for the rooted "Root extras"
 * surface only.
 */
interface BatteryController {

    /**
     * Reads every node under `/sys/class/power_supply/battery/` (and
     * `bms/` / `main/` where present) and returns them as a flat map.
     */
    suspend fun fuelGaugeRaw(): BatteryControllerResult

    /**
     * For multi-cell packs that expose per-cell sysfs nodes: returns a
     * snapshot of every cell's voltage / current / temperature.
     */
    suspend fun cellMonitor(): BatteryControllerResult

    /**
     * Writes to `constant_charge_current_max` and / or
     * `constant_charge_voltage_max`. Snapshot+restore in finally; hard
     * 30-second active window; thermal-zone-monitor coroutine aborts
     * + restores on any zone breaching `trip_point_0_temp`.
     */
    suspend fun chargingProfile(config: ChargingProfileConfig): BatteryControllerResult

    /**
     * Writes `disabled` to charger / battery / USB thermal-zone `mode`
     * nodes. Same severity as charging profile.
     */
    suspend fun thermalBypass(config: ThermalBypassConfig): BatteryControllerResult

    /**
     * Coerces USB charging-type detection.
     */
    suspend fun chargingTypeOverride(config: ChargingTypeOverrideConfig): BatteryControllerResult

    /**
     * Writes a single timestamped JSON snapshot of every readable
     * `/sys/class/power_supply/...` and `/sys/class/thermal/thermal_zone*`
     * node into the app's logbook directory under external files.
     */
    suspend fun fullDump(): BatteryControllerResult

    /**
     * Reverts every mutation this controller has registered with the
     * shared `SysfsMutationLog`, filtered by battery-related path
     * prefixes. Standard-flavor returns `ResetCompleted(0, 0)`.
     */
    suspend fun resetAllBatteryMutations(): BatteryControllerResult

    /**
     * Holds the pack at a target SOC by toggling `input_suspend` /
     * `charge_disable` while polling `capacity` at 1 Hz. Hard ceiling
     * 600 s; target SOC clamped to 20–90 inside the helper. Snapshot+restore
     * via `power_supply://battery/hold_soc`.
     */
    suspend fun holdStateOfCharge(config: HoldSocConfig): BatteryControllerResult

    /**
     * Reads cycle-count / design-capacity / full-charge-capacity / fuel-gauge
     * IC nodes (MAX1720x, qpnp-fg, etc.) and returns them as a
     * [BatteryControllerResult.BatteryHealthReading]. Read-only.
     */
    suspend fun batteryHealthDeepDump(): BatteryControllerResult

    /**
     * Caps the wireless-charging coil current. Hard ceiling 1 500 000 µA
     * and a 30-second active window enforced inside the helper, regardless
     * of caller-supplied values. Snapshot+restore via
     * `power_supply://wireless/coil_current`.
     */
    suspend fun wirelessCoilCurrent(config: WirelessCoilCurrentConfig): BatteryControllerResult
}
