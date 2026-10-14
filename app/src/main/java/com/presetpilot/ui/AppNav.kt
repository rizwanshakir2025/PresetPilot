package com.presetpilot.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.presetpilot.AppState
import com.presetpilot.data.Library
import com.presetpilot.ui.home.HomeScreen
import com.presetpilot.ui.onboarding.OnboardingScreen
import com.presetpilot.ui.setlist.SetlistScreen
import com.presetpilot.ui.settings.SettingsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Presets("Presets", Icons.Filled.PlayArrow),
    Setlist("Setlist", Icons.Filled.List),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun AppNav(
    appState: AppState,
    library: Library,
    onOnboardingDone: () -> Unit,
) {
    if (!library.onboardingDone) {
        OnboardingScreen(midi = appState.midi, onDone = onOnboardingDone)
        return
    }
    var tab by remember { mutableStateOf(Tab.Presets) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.Presets -> HomeScreen(appState, library, padding)
            Tab.Setlist -> SetlistScreen(appState, library, padding)
            Tab.Settings -> SettingsScreen(appState, library, padding)
        }
    }
}
