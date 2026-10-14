package com.presetpilot.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presetpilot.AppState
import com.presetpilot.data.Bank
import com.presetpilot.data.Library
import com.presetpilot.data.Preset
import com.presetpilot.ui.components.StepperRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(appState: AppState, library: Library, padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val midi = appState.midi
    val virtualReady by midi.virtualReady.collectAsState()

    var activeBankId by remember(library.banks) {
        mutableStateOf(library.banks.firstOrNull()?.id)
    }
    if (activeBankId !in library.banks.map { it.id }) {
        activeBankId = library.banks.firstOrNull()?.id
    }
    val bank: Bank? = library.banks.firstOrNull { it.id == activeBankId }
    val presets = bank?.let { library.presetsIn(it.id) } ?: emptyList()

    var flashedId by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var managePreset by remember { mutableStateOf<Preset?>(null) }
    var editingPreset by remember { mutableStateOf<Preset?>(null) }

    fun fire(preset: Preset) {
        val b = library.banks.firstOrNull { it.id == preset.bankId } ?: return
        midi.sendPreset(preset.program, b.channel, b.msb, b.lsb)
        flashedId = preset.id
        scope.launch {
            delay(350)
            if (flashedId == preset.id) flashedId = null
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            if (bank != null) {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add preset")
                }
            }
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "PresetPilot", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (virtualReady) Color(0xFF7CFC9A) else Color.Gray)
                        .padding(6.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (virtualReady) "MIDI live" else "MIDI…",
                    fontSize = 12.sp, color = Color.Gray,
                )
            }

            // Bank chips
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (library.banks.isEmpty()) {
                    Text(
                        "No banks yet — add one in Settings.",
                        color = Color.Gray, modifier = Modifier.padding(8.dp),
                    )
                }
                library.banks.forEach { b ->
                    FilterChip(
                        selected = b.id == activeBankId,
                        onClick = { activeBankId = b.id },
                        label = { Text(b.name) },
                    )
                }
            }

            // Preset grid
            if (presets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No presets in this bank.\nTap + to add your first one.",
                        color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 24.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(presets, key = { it.id }) { preset ->
                        val flashed = flashedId == preset.id
                        Card(
                            modifier = Modifier
                                .aspectRatio(1.4f)
                                .combinedClickable(
                                    onClick = { fire(preset) },
                                    onLongClick = { managePreset = preset },
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (flashed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        preset.name,
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                        color = if (flashed) Color.Black else Color.White,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "PC ${preset.program}",
                                        fontSize = 12.sp,
                                        color = if (flashed) Color.Black else Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd && bank != null) {
        PresetEditDialog(
            title = "Add preset",
            initialName = "",
            initialProgram = 0,
            onDismiss = { showAdd = false },
            onSave = { name, program ->
                scope.launch {
                    appState.repo.addPreset(
                        Preset(bankId = bank.id, name = name, program = program),
                    )
                }
                showAdd = false
            },
        )
    }

    managePreset?.let { preset ->
        AlertDialog(
            onDismissRequest = { managePreset = null },
            title = { Text(preset.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            editingPreset = preset
                            managePreset = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Edit name / program") }
                    TextButton(
                        onClick = {
                            scope.launch { appState.repo.addToSetlist(preset.id) }
                            managePreset = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add to setlist") }
                    TextButton(
                        onClick = {
                            scope.launch { appState.repo.deletePreset(preset.id) }
                            managePreset = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { managePreset = null }) { Text("Close") } },
        )
    }

    editingPreset?.let { preset ->
        PresetEditDialog(
            title = "Edit preset",
            initialName = preset.name,
            initialProgram = preset.program,
            onDismiss = { editingPreset = null },
            onSave = { name, program ->
                scope.launch {
                    appState.repo.updatePreset(preset.copy(name = name, program = program))
                }
                editingPreset = null
            },
        )
    }
}

@Composable
private fun PresetEditDialog(
    title: String,
    initialName: String,
    initialProgram: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var program by remember { mutableStateOf(initialProgram) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                StepperRow(
                    label = "Program",
                    value = program, range = 0..127,
                    onChange = { program = it },
                )
                Text(
                    "Matches the program number in your VST.",
                    fontSize = 12.sp, color = Color.Gray,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.ifBlank { "Preset ${program}" }, program) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
