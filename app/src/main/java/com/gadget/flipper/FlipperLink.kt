package com.gadget.flipper

import kotlinx.coroutines.flow.Flow

/**
 * Transport-agnostic byte pipe to a Flipper Zero. Implemented by USB CDC-ACM
 * and BLE GATT. Hands raw bytes back and forth — framing and protobuf live in
 * [com.gadget.flipper.rpc].
 */
interface FlipperLink {

    val transportName: String

    /** Emits chunks of bytes as they arrive from the device. */
    fun incoming(): Flow<ByteArray>

    /** Send raw bytes to the device. Throws on transport error. */
    suspend fun send(data: ByteArray)

    /** Close the underlying connection. Idempotent. */
    suspend fun close()
}
