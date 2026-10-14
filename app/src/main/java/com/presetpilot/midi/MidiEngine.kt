package com.presetpilot.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns all MIDI output.
 *
 * Primary path: the app opens its own virtual device ("PresetPilot") and
 * writes into its input port; USB MIDI peripheral mode bridges that to the PC.
 * Fallback: any physical MIDI device visible to [MidiManager].
 */
class MidiEngine(private val appContext: Context) {

    companion object {
        private const val TAG = "MidiEngine"
        const val VIRTUAL_TARGET_ID = "virtual"
    }

    data class Target(val id: String, val label: String, val isVirtual: Boolean)

    private val midiManager: MidiManager =
        appContext.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var selfWritePort: MidiInputPort? = null
    private var physicalPort: MidiInputPort? = null
    private var physicalDevice: MidiDevice? = null

    var targetId: String = VIRTUAL_TARGET_ID

    private val _virtualReady = MutableStateFlow(false)
    val virtualReady: StateFlow<Boolean> = _virtualReady.asStateFlow()

    fun listTargets(): List<Target> {
        val out = mutableListOf(
            Target(VIRTUAL_TARGET_ID, "Phone USB — PresetPilot (recommended)", true),
        )
        midiManager.devices
            .filter { info ->
                info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) != "PresetPilot" &&
                    info.inputPortCount > 0
            }
            .forEach { info ->
                val name = info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                    ?: info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: "MIDI device"
                out.add(Target(deviceId(info), name, false))
            }
        return out
    }

    fun start() {
        val self = midiManager.devices.firstOrNull { info ->
            info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) == "PresetPilot"
        }
        if (self == null) {
            mainHandler.postDelayed({ start() }, 1500)
            return
        }
        midiManager.openDevice(self, { device ->
            selfWritePort = device.openInputPort(0)
            _virtualReady.value = selfWritePort != null
            Log.i(TAG, "Virtual device write port open")
        }, mainHandler)
    }

    fun selectTarget(id: String) {
        targetId = id
        try { physicalPort?.close() } catch (_: Exception) {}
        try { physicalDevice?.close() } catch (_: Exception) {}
        physicalPort = null
        physicalDevice = null
        if (id == VIRTUAL_TARGET_ID) return
        val info = midiManager.devices.firstOrNull { deviceId(it) == id } ?: return
        midiManager.openDevice(info, { device ->
            physicalDevice = device
            physicalPort = device.openInputPort(0)
            Log.i(TAG, "Physical target open: $id")
        }, mainHandler)
    }

    /** The core primitive: switch a VST to a preset. */
    fun sendPreset(program: Int, channel: Int, msb: Int, lsb: Int) {
        // Bank Select MSB/LSB first, then Program Change — the universal order.
        controlChange(0, msb, channel)
        controlChange(32, lsb, channel)
        programChange(program, channel)
    }

    fun programChange(program: Int, channel: Int) {
        send(byteArrayOf((0xC0 or (channel and 0x0F)).toByte(), program.toByte()))
    }

    fun controlChange(cc: Int, value: Int, channel: Int) {
        send(
            byteArrayOf(
                (0xB0 or (channel and 0x0F)).toByte(),
                cc.coerceIn(0, 127).toByte(),
                value.coerceIn(0, 127).toByte(),
            ),
        )
    }

    /** Panic button: silence everything, everywhere. */
    fun allNotesOff() {
        for (ch in 0..15) controlChange(123, 0, ch)
    }

    private fun send(bytes: ByteArray) {
        try {
            val port = if (targetId == VIRTUAL_TARGET_ID) selfWritePort else physicalPort
            port?.send(bytes, 0, bytes.size)
            MidiMonitor.log(bytes)
        } catch (e: Exception) {
            Log.w(TAG, "send failed: ${e.message}")
        }
    }

    fun shutdown() {
        try { selfWritePort?.close() } catch (_: Exception) {}
        try { physicalPort?.close() } catch (_: Exception) {}
        try { physicalDevice?.close() } catch (_: Exception) {}
    }

    private fun deviceId(info: MidiDeviceInfo): String {
        val product = info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT) ?: "dev"
        val serial = info.properties.getString(MidiDeviceInfo.PROPERTY_SERIAL_NUMBER) ?: "0"
        return "$product:$serial:${info.id}"
    }
}
