package dev.ranzlappen.gadget.feature.flipper.rpc

import dev.ranzlappen.gadget.feature.flipper.transport.FlipperLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sends framed PB_Main messages over a [FlipperLink] and demultiplexes
 * responses by `command_id`. Each request gets its own channel that yields
 * one or more response messages until `has_next == false`.
 *
 * Note: before the first RPC call you must put the Flipper in RPC mode by
 * sending the CLI command "start_rpc_session\r" — see [enterRpcSession].
 */
class FlipperRpcClient(
    private val link: FlipperLink,
) {

    private val nextId = AtomicInteger(1)
    private val pending = mutableMapOf<Int, Channel<PbMain.Decoded>>()
    private val pendingLock = Mutex()
    private val frameReader = FrameReader()
    private val sendLock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pumpJob: Job? = null
    @Volatile private var rpcStarted = false

    /** Begin reading bytes from the link and routing responses. Idempotent. */
    fun start() {
        if (pumpJob != null) return
        pumpJob = scope.launch {
            link.incoming().collect { chunk ->
                val frames = frameReader.feed(chunk)
                for (frame in frames) {
                    val decoded = runCatching { PbMain.decode(frame) }.getOrNull() ?: continue
                    pendingLock.withLock {
                        val ch = pending[decoded.commandId]
                        if (ch != null) {
                            ch.trySend(decoded)
                            if (!decoded.hasNext) {
                                ch.close()
                                pending.remove(decoded.commandId)
                            }
                        } else {
                            Timber.d("Unsolicited frame, command_id=${decoded.commandId}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Send the CLI command that switches the Flipper from text CLI mode into
     * binary RPC mode. Safe to call multiple times.
     */
    suspend fun enterRpcSession() {
        if (rpcStarted) return
        sendLock.withLock {
            link.send("\r\nstart_rpc_session\r\n".toByteArray(Charsets.US_ASCII))
            rpcStarted = true
        }
    }

    /** Allocate a fresh non-zero command_id. */
    fun nextCommandId(): Int = nextId.getAndIncrement()

    /**
     * Send a request and await the (single, terminal) response.
     */
    internal suspend fun call(
        contentField: Int,
        contentBody: ByteArray,
        timeoutMs: Long = 5_000,
    ): PbMain.Decoded {
        val id = nextCommandId()
        val ch = Channel<PbMain.Decoded>(capacity = 8)
        pendingLock.withLock { pending[id] = ch }
        try {
            val frame = RpcFraming.frame(PbMain.encode(id, contentField, contentBody))
            sendLock.withLock { link.send(frame) }
            return withTimeout(timeoutMs) {
                ch.receive()
            }
        } finally {
            pendingLock.withLock { pending.remove(id)?.close() }
        }
    }

    /**
     * Send a streaming request (multiple frames sharing one command_id, all but
     * the last with has_next=true) and wait for the terminal response.
     */
    internal suspend fun callStreaming(
        contentField: Int,
        bodies: Sequence<ByteArray>,
        timeoutMs: Long = 30_000,
    ): PbMain.Decoded {
        val id = nextCommandId()
        val ch = Channel<PbMain.Decoded>(capacity = 8)
        pendingLock.withLock { pending[id] = ch }
        try {
            val iter = bodies.iterator()
            check(iter.hasNext()) { "Streaming call requires at least one body" }
            sendLock.withLock {
                var current = iter.next()
                while (true) {
                    val hasNext = iter.hasNext()
                    val frame = RpcFraming.frame(PbMain.encode(id, contentField, current, hasNext))
                    link.send(frame)
                    if (!hasNext) break
                    current = iter.next()
                }
            }
            return withTimeout(timeoutMs) { ch.receive() }
        } finally {
            pendingLock.withLock { pending.remove(id)?.close() }
        }
    }

    suspend fun stop() {
        pumpJob?.cancel()
        pumpJob = null
        scope.cancel()
        pendingLock.withLock {
            pending.values.forEach { it.close() }
            pending.clear()
        }
    }
}
