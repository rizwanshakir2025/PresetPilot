package com.presetpilot.midi

import android.media.midi.MidiReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Timestamped log of every MIDI message leaving the phone. */
object MidiMonitor {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun log(bytes: ByteArray) {
        val line = "${timeFormat.format(Date())}  ${describe(bytes)}"
        _lines.value = (_lines.value + line).takeLast(40)
    }

    fun clear() {
        _lines.value = emptyList()
    }

    private fun describe(b: ByteArray): String {
        if (b.size < 2) return b.joinToString(" ") { "%02X".format(it) }
        val status = b[0].toInt() and 0xFF
        val ch = (status and 0x0F) + 1
        return when (status and 0xF0) {
            0xC0 -> "Program Change  ch$ch  program ${b[1]}"
            0xB0 -> when (b[1].toInt()) {
                0 -> "Bank Select MSB  ch$ch  = ${b[2]}"
                32 -> "Bank Select LSB  ch$ch  = ${b[2]}"
                123 -> "All Notes Off  ch$ch"
                else -> "CC  ch$ch  cc ${b[1]} = ${b[2]}"
            }
            0x90 -> "Note On   ch$ch  note ${b[1]}  vel ${b[2]}"
            0x80 -> "Note Off  ch$ch  note ${b[1]}"
            else -> b.joinToString(" ") { "%02X".format(it) }
        }
    }
}

/** Receiver attached to our virtual device's input port (feeds the monitor). */
class MonitorReceiver : MidiReceiver() {
    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
        MidiMonitor.log(msg.copyOfRange(offset, offset + count))
    }
}
