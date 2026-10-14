package com.presetpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.presetpilot.data.Library
import com.presetpilot.data.PresetRepository
import com.presetpilot.midi.MidiEngine
import com.presetpilot.ui.AppNav
import com.presetpilot.ui.theme.PresetPilotTheme
import kotlinx.coroutines.launch

class AppState(
    val repo: PresetRepository,
    val midi: MidiEngine,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PresetPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val appState = remember {
                        AppState(
                            PresetRepository(applicationContext),
                            MidiEngine(applicationContext),
                        )
                    }
                    DisposableEffect(Unit) {
                        appState.midi.start()
                        onDispose { appState.midi.shutdown() }
                    }

                    val library by appState.repo.library
                        .collectAsState(initial = Library())

                    LaunchedEffect(library.midiTarget) {
                        appState.midi.selectTarget(library.midiTarget)
                    }

                    AppNav(
                        appState = appState,
                        library = library,
                        onOnboardingDone = {
                            lifecycleScope.launch {
                                appState.repo.update { it.copy(onboardingDone = true) }
                            }
                        },
                    )
                }
            }
        }
    }
}
