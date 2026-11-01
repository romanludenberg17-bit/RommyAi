package com.rommy.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.rommy.ai.apps.AppLauncher
import com.rommy.ai.commands.CommandRouter
import com.rommy.ai.timers.TimerManager
import com.rommy.ai.voice.SpeechRecognizerController
import com.rommy.ai.voice.SpeechStyle
import com.rommy.ai.voice.VoiceCoordinator
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var voice: VoiceCoordinator
    private lateinit var recognizer: SpeechRecognizerController

    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recognizer.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voice = VoiceCoordinator(this)
        recognizer = SpeechRecognizerController(this, onResult = ::handleCommand)
        setContent { RommyAiRoot(voice, ::startListening) }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else recognizer.start()
    }

    private fun handleCommand(text: String) {
        val result = CommandRouter(AppLauncher(this), TimerManager(this)).handle(text)
        voice.speak(result.response, Locale.getDefault(), if (result.handled) SpeechStyle.CHEERFUL else SpeechStyle.CALM)
    }

    override fun onDestroy() {
        recognizer.release()
        voice.release()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun RommyAiRoot(voice: VoiceCoordinator, onListen: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context) }
    val timerManager = remember { TimerManager(context) }
    val router = remember { CommandRouter(launcher, timerManager) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("готова к команде") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Rommy AI", style = MaterialTheme.typography.headlineMedium)
                Text(status, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("команда") })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val result = router.handle(query)
                        status = result.response
                        voice.speak(result.response, Locale.getDefault(), if (result.handled) SpeechStyle.CHEERFUL else SpeechStyle.CALM)
                    }) { Text("выполнить") }
                    Button(onClick = { status = "слушаю…"; onListen() }) { Text("говорить") }
                    Button(onClick = { status = "я здесь"; voice.speak("я здесь. слушаю тебя.", Locale.getDefault(), SpeechStyle.SOFT) }) { Text("голос") }
                }
            }
        }
    }
}
