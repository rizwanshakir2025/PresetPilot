package com.presetpilot.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presetpilot.midi.MidiEngine

/** First-run setup: USB MIDI mode, DAW hookup, how preset switching works. */
@Composable
fun OnboardingScreen(midi: MidiEngine, onDone: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val titles = listOf("Connect USB MIDI", "Set up your DAW", "How it works", "Plugin setup")
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "PresetPilot", fontSize = 30.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            titles[page], fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (page) {
                0 -> UsbPage(midi)
                1 -> DawPage()
                2 -> HowPage()
                3 -> PluginPage()
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > 0) TextButton(onClick = { page-- }) { Text("Back") }
            else TextButton(onClick = onDone) { Text("Skip") }
            Text("${page + 1} / 4", color = Color.Gray)
            if (page < 3) Button(onClick = { page++ }) { Text("Next") }
            else Button(onClick = onDone) { Text("Done") }
        }
    }
}

@Composable
private fun UsbPage(midi: MidiEngine) {
    val ready by midi.virtualReady.collectAsState()
    Text("One-time setup — do this once and forget it:", fontSize = 16.sp, lineHeight = 24.sp)
    Spacer(Modifier.height(12.dp))
    listOf(
        "Plug the phone into your PC with a USB-C data cable (not charge-only).",
        "On the phone, tap the USB notification ('Charging this device').",
        "Under 'Use USB for', choose MIDI.",
        "The PC now sees 'PresetPilot' as a MIDI device.",
    ).forEachIndexed { i, s -> NumberedStep(i + 1, s) }
    Spacer(Modifier.height(16.dp))
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ready) Color(0xFF12351F) else Color(0xFF3A2E12),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (ready) "✓ PresetPilot virtual device is live."
            else "○ Virtual device starting…",
            modifier = Modifier.padding(16.dp), fontSize = 15.sp,
        )
    }
}

@Composable
private fun DawPage() {
    Text(
        "On the DAW track that holds your VST, enable the 'PresetPilot' MIDI " +
            "input (usually under the track's MIDI input dropdown or the DAW's " +
            "MIDI settings). Arm the track if your DAW needs it.",
        fontSize = 16.sp, lineHeight = 24.sp,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Most VSTs switch presets on Program Change out of the box. A few " +
            "(Kontakt instruments, some Neural DSP plugins) need 'Program Change' " +
            "or 'MIDI preset switching' enabled in their own settings — check once " +
            "per plugin.",
        fontSize = 14.sp, lineHeight = 22.sp, color = Color.Gray,
    )
}

@Composable
private fun HowPage() {
    Text("Each preset sends, in order:", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    listOf(
        "Bank Select MSB (CC 0) — from the bank's settings",
        "Bank Select LSB (CC 32) — from the bank's settings",
        "Program Change — the preset's program number (0–127)",
    ).forEachIndexed { i, s -> NumberedStep(i + 1, s) }
    Spacer(Modifier.height(12.dp))
    Text(
        "Organize presets into banks per instrument or song section, give each " +
            "bank its own MIDI channel, and build setlists for gigs. The Panic " +
            "button on the Setlist tab silences everything.",
        fontSize = 14.sp, lineHeight = 22.sp, color = Color.Gray,
    )
}

@Composable
private fun NumberedStep(n: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text("$n. ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun PluginPage() {
    var plugin by remember { mutableStateOf("Neural DSP") }
    val plugins = listOf("Neural DSP", "Serum 2", "Thall")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        plugins.forEach { name ->
            val selected = name == plugin
            OutlinedButton(
                onClick = { plugin = name },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    name,
                    fontSize = 12.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else Color.Gray,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    when (plugin) {
        "Neural DSP" -> {
            Text(
                "Neural DSP plugins don't switch presets on raw Program Change " +
                    "alone — you map each preset once, then PresetPilot drives it:",
                fontSize = 14.sp, lineHeight = 22.sp, color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Route PresetPilot's MIDI to the track with the Neural DSP plugin.",
                "In the plugin, click the MIDI port icon (bottom-left) → MIDI mappings → + → 'Program Change Preset'.",
                "Pick the preset, set the PC number (channel 1 = the default), save.",
                "Repeat for each preset. The little MIDI icon appears next to mapped presets.",
                "Shortcut: right-click any preset → 'Enable MIDI Learn' → tap the PresetPilot button for it.",
            ).forEachIndexed { i, s -> NumberedStep(i + 1, s) }
        }
        "Serum 2" -> {
            Text(
                "Serum responds to MIDI Program Change directly — no mapping " +
                    "needed. PCs step through presets in browser order.",
                fontSize = 14.sp, lineHeight = 22.sp, color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Route PresetPilot's MIDI to the Serum 2 track.",
                "Tap a PresetPilot button → Serum loads the preset at that program number.",
                "Tip: arrange your gig presets in one Serum folder so the numbers stay predictable.",
            ).forEachIndexed { i, s -> NumberedStep(i + 1, s) }
        }
        else -> {
            Text(
                "Odeholm's Thall amp has no documented MIDI preset switching " +
                    "yet — treat this one as an experiment:",
                fontSize = 14.sp, lineHeight = 22.sp, color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Route PresetPilot's MIDI to the Thall track and tap a preset.",
                "If nothing happens, Thall likely ignores Program Change — " +
                    "check its manual/settings for any MIDI options.",
                "Fallback: automate Thall's preset menu from the DAW, or ask " +
                    "Odeholm support — feature requests from users drive these.",
            ).forEachIndexed { i, s -> NumberedStep(i + 1, s) }
        }
    }
}
