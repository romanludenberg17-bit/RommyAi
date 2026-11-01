package com.rommy.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rommy.ai.apps.AppLauncher
import com.rommy.ai.voice.SpeechStyle
import com.rommy.ai.voice.VoiceCoordinator
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var voice: VoiceCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voice = VoiceCoordinator(this)
        setContent { RommyAiRoot(voice) }
    }

    override fun onDestroy() {
        voice.release()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun RommyAiRoot(voice: VoiceCoordinator) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("готова к команде") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Rommy AI", style = MaterialTheme.typography.headlineMedium)
                Text(status, style = MaterialTheme.typography.bodyLarge)

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("команда или приложение") },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val found = launcher.find(query)
                        status = when {
                            found.isEmpty() -> "не нашла приложение: $query"
                            launcher.launchPackage(found.first().packageName) -> "запускаю ${found.first().label}"
                            else -> "не удалось открыть ${found.first().label}"
                        }
                        voice.speak(status, Locale.getDefault(), SpeechStyle.CHEERFUL)
                    }) {
                        Text("открыть")
                    }
                    Button(onClick = {
                        status = "я здесь"
                        voice.speak("я здесь. слушаю тебя.", Locale.getDefault(), SpeechStyle.SOFT)
                    }) {
                        Text("голос")
                    }
                }
            }
        }
    }
}
