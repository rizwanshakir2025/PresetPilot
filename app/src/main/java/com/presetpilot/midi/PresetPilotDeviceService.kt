package com.presetpilot.midi

import android.media.midi.MidiDeviceService
import android.media.midi.MidiReceiver

/**
 * The virtual MIDI device "PresetPilot".
 *
 * The app writes Program Change / Bank Select into this device's input port
 * (opened on its own device instance). With the phone in USB MIDI peripheral
 * mode, the system bridges the virtual device to the PC, which sees the phone
 * as a class-compliant MIDI controller — no drivers, no companion app.
 */
class PresetPilotDeviceService : MidiDeviceService() {
    private val monitor = MonitorReceiver()

    override fun onGetInputPortReceivers(): Array<MidiReceiver> {
        return arrayOf(monitor)
    }
}
