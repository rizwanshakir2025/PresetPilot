package com.presetpilot.midi

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.LinkedBlockingQueue

/**
 * WiFi MIDI transport, mirroring the design Riz1 already runs with MouthMIDI:
 * each raw MIDI message goes out as one UDP datagram to the PC, where
 * MidiBridge.exe (listening on UDP 52365) forwards it through a loopMIDI
 * virtual port into the DAW.
 *
 * The FIFO queue + single sender thread keeps message order (CC0 -> CC32 ->
 * Program Change) intact on the LAN.
 */
class NetworkMidiTransport {

    companion object {
        private const val TAG = "NetMidi"
        const val DEFAULT_PORT = 52365
    }

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val queue = LinkedBlockingQueue<DatagramPacket>()

    @Volatile private var running = false
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private var port: Int = DEFAULT_PORT
    private var currentHost: String = ""
    private var senderThread: Thread? = null

    /** (Re)connect to the bridge. Safe to call repeatedly; no-op when unchanged. */
    @Synchronized
    fun configure(host: String, port: Int) {
        val h = host.trim()
        if (h.isEmpty()) {
            disconnect()
            return
        }
        val p = port.coerceIn(1, 65535)
        if (h == currentHost && p == this.port && _connected.value) return
        disconnect()
        try {
            // Numeric LAN IPs resolve instantly with no DNS hit.
            address = InetAddress.getByName(h)
            this.port = p
            socket = DatagramSocket()
            currentHost = h
            running = true
            senderThread = Thread(::pump, "presetpilot-udp").also { it.start() }
            _connected.value = true
            Log.i(TAG, "UDP MIDI -> $h:$p")
        } catch (e: Exception) {
            Log.w(TAG, "UDP connect failed: ${e.message}")
            disconnect()
        }
    }

    @Synchronized
    fun disconnect() {
        running = false
        _connected.value = false
        senderThread?.interrupt()
        senderThread = null
        queue.clear()
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        address = null
        currentHost = ""
    }

    /** Enqueue one raw MIDI message. Non-blocking; dropped silently when offline. */
    fun send(bytes: ByteArray) {
        val addr = address
        val sock = socket
        if (!_connected.value || addr == null || sock == null || bytes.isEmpty()) return
        queue.offer(DatagramPacket(bytes.copyOf(), bytes.size, addr, port))
    }

    private fun pump() {
        while (running) {
            try {
                val packet = queue.take()
                if (!running) break
                try {
                    socket?.send(packet)
                    MidiMonitor.log(packet.data.copyOf(packet.length), transport = "wifi")
                } catch (e: Exception) {
                    Log.w(TAG, "UDP send failed: ${e.message}")
                }
            } catch (_: InterruptedException) {
                break
            }
        }
    }
}
