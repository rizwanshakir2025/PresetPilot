package com.presetpilot.data

import java.util.UUID

data class Bank(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val channel: Int = 0,   // 0-based
    val msb: Int = 0,       // Bank Select MSB (CC0)
    val lsb: Int = 0,       // Bank Select LSB (CC32)
)

data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val bankId: String,
    val name: String,
    val program: Int = 0,   // 0-127
)

data class Library(
    val banks: List<Bank> = emptyList(),
    val presets: List<Preset> = emptyList(),
    /** Ordered preset ids for setlist mode. */
    val setlist: List<String> = emptyList(),
    val onboardingDone: Boolean = false,
    /** "virtual" or a physical device id. */
    val midiTarget: String = "virtual",
    /** "usb" or "wifi" — WiFi goes to MidiBridge.exe on the PC over UDP. */
    val transportMode: String = "usb",
    val wifiHost: String = "192.168.1.100",
    val wifiPort: Int = 52365,
) {
    fun presetsIn(bankId: String): List<Preset> =
        presets.filter { it.bankId == bankId }

    fun setlistPresets(): List<Preset> =
        setlist.mapNotNull { id -> presets.firstOrNull { it.id == id } }
}
