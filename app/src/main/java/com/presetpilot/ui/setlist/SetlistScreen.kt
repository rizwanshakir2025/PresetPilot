package com.presetpilot.ui.setlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.presetpilot.data.Library
import com.presetpilot.data.Preset
import kotlinx.coroutines.launch

/**
 * Live setlist mode: ordered songs, giant PREV/NEXT, tap-to-jump, reorder,
 * and a panic button. Built for dark stages and fat fingers.
 */
@Composable
fun SetlistScreen(appState: AppState, library: Library, padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val midi = appState.midi
    val items = library.setlistPresets()
    var currentIndex by remember(items.size) { mutableStateOf(0) }
    if (currentIndex >= items.size) currentIndex = 0

    fun fire(preset: Preset, index: Int) {
        val bank = library.banks.firstOrNull { it.id == preset.bankId } ?: return
        midi.sendPreset(preset.program, bank.channel, bank.msb, bank.lsb)
        currentIndex = index
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Setlist", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { midi.allNotesOff() }) {
                Text("Panic")
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Setlist is empty.\nLong-press a preset on the Presets tab\nto add it here.",
                    color = Color.Gray, lineHeight = 24.sp,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items, key = { _, p -> p.id }) { index, preset ->
                val bank = library.banks.firstOrNull { it.id == preset.bankId }
                val active = index == currentIndex
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { fire(preset, index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${index + 1}.",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (active) Color.Black else Color.Gray,
                            modifier = Modifier.width(36.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                preset.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.Black else Color.White,
                            )
                            Text(
                                "${bank?.name ?: "?"} · PC ${preset.program}",
                                fontSize = 12.sp,
                                color = if (active) Color.Black else Color.Gray,
                            )
                        }
                        IconButton(onClick = {
                            scope.launch { appState.repo.moveSetlist(index, index - 1) }
                        }) {
                            Icon(
                                Icons.Filled.ArrowUpward, contentDescription = "Move up",
                                tint = if (active) Color.Black else Color.Gray,
                            )
                        }
                        IconButton(onClick = {
                            scope.launch { appState.repo.moveSetlist(index, index + 1) }
                        }) {
                            Icon(
                                Icons.Filled.ArrowDownward, contentDescription = "Move down",
                                tint = if (active) Color.Black else Color.Gray,
                            )
                        }
                        IconButton(onClick = {
                            scope.launch { appState.repo.removeFromSetlist(preset.id) }
                        }) {
                            Icon(
                                Icons.Filled.Close, contentDescription = "Remove",
                                tint = if (active) Color.Black else Color.Gray,
                            )
                        }
                    }
                }
            }
        }

        // Giant transport
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    val prev = (currentIndex - 1).coerceAtLeast(0)
                    fire(items[prev], prev)
                },
                modifier = Modifier.weight(1f).height(72.dp),
            ) {
                Text("◀ Prev", fontSize = 20.sp)
            }
            Button(
                onClick = {
                    val next = (currentIndex + 1).coerceAtMost(items.size - 1)
                    fire(items[next], next)
                },
                modifier = Modifier.weight(1f).height(72.dp),
            ) {
                Text("Next ▶", fontSize = 20.sp)
            }
        }
        val current = items.getOrNull(currentIndex)
        if (current != null) {
            Text(
                "Now: ${current.name}",
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray, fontSize = 14.sp,
            )
        }
    }
}
