package com.presetpilot.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presetpilot.AppState
import com.presetpilot.data.Bank
import com.presetpilot.data.Library
import com.presetpilot.midi.MidiMonitor
import com.presetpilot.ui.components.SectionTitle
import com.presetpilot.ui.components.StepperRow
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(appState: AppState, library: Library, padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val midi = appState.midi
    val virtualReady by midi.virtualReady.collectAsState()
    var targets by remember { mutableStateOf(midi.listTargets()) }
    val monitorLines by MidiMonitor.lines.collectAsState()
    var showMonitor by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<Bank?>(null) }
    var addingBank by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        Text(
            "Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        SectionTitle("MIDI Output")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                if (virtualReady) "● PresetPilot virtual device live — PC sees it over USB"
                else "○ Virtual device starting…",
                modifier = Modifier.padding(12.dp), fontSize = 14.sp,
                color = if (virtualReady) Color(0xFF7CFC9A) else Color.Gray,
            )
        }
        Spacer(Modifier.height(8.dp))
        targets.forEach { t ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable {
                        scope.launch {
                            appState.repo.update { it.copy(midiTarget = t.id) }
                        }
                    }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = library.midiTarget == t.id,
                    onClick = {
                        scope.launch {
                            appState.repo.update { it.copy(midiTarget = t.id) }
                        }
                    },
                )
                Text(t.label, fontSize = 15.sp)
            }
        }
        OutlinedButton(onClick = { targets = midi.listTargets() }) {
            Text("Refresh devices")
        }
        TextButton(onClick = { showMonitor = !showMonitor }) {
            Text(if (showMonitor) "Hide MIDI monitor" else "Show MIDI monitor")
        }
        if (showMonitor) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0D12)),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    monitorLines.takeLast(12).forEach { line ->
                        Text(line, fontSize = 11.sp, color = Color(0xFF7CFC9A))
                    }
                    if (monitorLines.isEmpty()) {
                        Text(
                            "No messages yet — tap a preset.",
                            fontSize = 12.sp, color = Color.Gray,
                        )
                    }
                    TextButton(onClick = { MidiMonitor.clear() }) { Text("Clear") }
                }
            }
        }

        SectionTitle("Banks")
        library.banks.forEach { bank ->
            val count = library.presetsIn(bank.id).size
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bank.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "$count presets · ch ${bank.channel + 1} · bank ${bank.msb}/${bank.lsb}",
                        fontSize = 12.sp, color = Color.Gray,
                    )
                }
                IconButton(onClick = { editingBank = bank }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit bank", tint = Color.Gray)
                }
                IconButton(onClick = {
                    scope.launch { appState.repo.deleteBank(bank.id) }
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete bank", tint = Color.Gray)
                }
            }
        }
        OutlinedButton(onClick = { addingBank = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Add bank")
        }

        SectionTitle("About")
        Text(
            "PresetPilot 1.0.0 — switch VST presets from your phone over USB MIDI.",
            fontSize = 14.sp, color = Color.Gray,
        )
        TextButton(onClick = {
            scope.launch {
                appState.repo.update { it.copy(onboardingDone = false) }
            }
        }) {
            Text("Replay setup guide")
        }
        Spacer(Modifier.height(32.dp))
    }

    if (addingBank) {
        BankEditDialog(
            title = "Add bank",
            initial = Bank(name = ""),
            onDismiss = { addingBank = false },
            onSave = { bank ->
                scope.launch { appState.repo.addBank(bank) }
                addingBank = false
            },
        )
    }
    editingBank?.let { bank ->
        BankEditDialog(
            title = "Edit bank",
            initial = bank,
            onDismiss = { editingBank = null },
            onSave = { updated ->
                scope.launch { appState.repo.updateBank(updated) }
                editingBank = null
            },
        )
    }
}

@Composable
private fun BankEditDialog(
    title: String,
    initial: Bank,
    onDismiss: () -> Unit,
    onSave: (Bank) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var channel by remember { mutableStateOf(initial.channel) }
    var msb by remember { mutableStateOf(initial.msb) }
    var lsb by remember { mutableStateOf(initial.lsb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bank name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                StepperRow(
                    label = "MIDI channel", value = channel + 1, range = 1..16,
                    onChange = { channel = it - 1 },
                )
                StepperRow(
                    label = "Bank MSB (CC0)", value = msb, range = 0..127,
                    onChange = { msb = it },
                )
                StepperRow(
                    label = "Bank LSB (CC32)", value = lsb, range = 0..127,
                    onChange = { lsb = it },
                )
                Text(
                    "Most VSTs ignore bank select and just use the program number — " +
                        "leave MSB/LSB at 0 unless yours needs them.",
                    fontSize = 12.sp, color = Color.Gray,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(name = name.ifBlank { "Bank" }, channel = channel, msb = msb, lsb = lsb))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
