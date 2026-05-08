package com.gadget.gps

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor GPS controller. Every method returns
 * [GpsControllerResult.Unsupported].
 */
@Singleton
class StandardGpsController @Inject constructor() : GpsController {

    override suspend fun nmeaRawTap(config: NmeaTapConfig): GpsControllerResult =
        GpsControllerResult.Unsupported

    override suspend fun constellationDump(): GpsControllerResult =
        GpsControllerResult.Unsupported

    override suspend fun resetAllGpsMutations(): GpsControllerResult =
        GpsControllerResult.ResetCompleted(restored = 0, failed = 0)
}
