package com.gadget.flipper.rpc

/**
 * System-namespace RPC commands: ping, device info, power info.
 */
class SystemCommands(private val client: FlipperRpcClient) {

    /** Send a ping; returns true if the device responded with OK. */
    suspend fun ping(): Boolean {
        val resp = client.call(PbMain.C_PING_REQUEST, ByteArray(0))
        return PbStatus.isOk(resp.commandStatus) && resp.contentField == PbMain.C_PING_RESPONSE
    }

    data class DeviceInfo(val pairs: Map<String, String>) {
        val firmwareVersion: String? get() = pairs["firmware_version"] ?: pairs["protobuf_version"]
        val hardwareName: String? get() = pairs["hardware_name"] ?: pairs["device_name"]
    }

    /**
     * DeviceInfo is streamed: each response frame carries one (key, value) pair
     * with `has_next=true` until the last. We currently take only the first
     * pair returned (firmware_version is typically near the front); a richer
     * implementation would extend FlipperRpcClient to collect every frame
     * sharing a command_id.
     */
    suspend fun deviceInfo(): DeviceInfo {
        val resp = client.call(PbMain.C_DEVICE_INFO_REQUEST, ByteArray(0))
        val pairs = mutableMapOf<String, String>()
        readPair(resp.contentBody, pairs)
        return DeviceInfo(pairs)
    }

    data class PowerInfo(val pairs: Map<String, String>) {
        val batteryLevel: Int? get() = pairs["charge_level"]?.toIntOrNull()
        val voltage: String? get() = pairs["voltage"]
    }

    suspend fun powerInfo(): PowerInfo {
        val resp = client.call(PbMain.C_POWER_INFO_REQUEST, ByteArray(0))
        val pairs = mutableMapOf<String, String>()
        readPair(resp.contentBody, pairs)
        return PowerInfo(pairs)
    }

    private fun readPair(body: ByteArray, out: MutableMap<String, String>) {
        // Both DeviceInfoResponse and PowerInfoResponse: { string key = 1; string value = 2 }.
        val r = PbReader(body)
        var key: String? = null
        var value: String? = null
        while (r.hasMore()) {
            val tag = r.readTag()
            val field = r.fieldOf(tag)
            val wire = r.wireOf(tag)
            when (field) {
                1 -> key = r.readString()
                2 -> value = r.readString()
                else -> r.skip(wire)
            }
        }
        if (key != null) out[key] = value.orEmpty()
    }
}
