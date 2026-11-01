package com.rommy.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rommy.ai.ai.AssistantSettings
import com.rommy.ai.ai.GeminiClient
import com.rommy.ai.apps.AppLauncher
import com.rommy.ai.commands.CommandRouter
import com.rommy.ai.timers.TimerManager
import com.rommy.ai.voice.SpeechRecognizerController
import com.rommy.ai.voice.SpeechStyle
import com.rommy.ai.voice.VoiceCoordinator
import java.util.Locale
import kotlinx.coroutines.launch

private val Ink = Color(0xFF11101A)
private val Panel = Color(0xFF1B1927)
private val PanelRaised = Color(0xFF252236)
private val Lavender = Color(0xFFC8B8FF)
private val Pink = Color(0xFFFFB8D2)
private val Muted = Color(0xFFB9B3C9)

class MainActivity : ComponentActivity() {
    private lateinit var voice: VoiceCoordinator
    private lateinit var recognizer: SpeechRecognizerController

    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recognizer.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voice = VoiceCoordinator(this)
        recognizer = SpeechRecognizerController(this, onResult = ::handleRecognizedCommand)
        val assistantSettings = AssistantSettings(this)
        val geminiClient = GeminiClient()
        setContent {
            RommyTheme {
                RommyAssistant(
                    voice = voice,
                    onListen = ::startListening,
                    onAsk = { prompt, history -> askAssistant(prompt, history, assistantSettings, geminiClient) },
                    onSaveApiKey = assistantSettings::saveGeminiApiKey,
                    apiKeyConfigured = assistantSettings.getGeminiApiKey() != null
                )
            }
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else recognizer.start()
    }

    private fun handleRecognizedCommand(text: String) {
        runLocalCommand(text)
    }

    private suspend fun askAssistant(
        text: String,
        history: List<Pair<String, String>>,
        settings: AssistantSettings,
        gemini: GeminiClient
    ): String {
        val local = CommandRouter(AppLauncher(this), TimerManager(this)).handle(text)
        if (local.handled) {
            voice.speak(local.response, Locale.getDefault(), SpeechStyle.CHEERFUL)
            return local.response
        }
        val key = settings.getGeminiApiKey()
            ?: return "подключи Gemini API key в настройках, чтобы я могла отвечать на вопросы."
        return try {
            gemini.generate(key, text, history).also { voice.speak(it, Locale.getDefault(), SpeechStyle.DEFAULT) }
        } catch (error: Exception) {
            "не удалось получить ответ: ${error.message ?: "ошибка соединения"}"
        }
    }

    private fun runLocalCommand(text: String): String {
        val result = CommandRouter(AppLauncher(this), TimerManager(this)).handle(text)
        voice.speak(result.response, Locale.getDefault(), if (result.handled) SpeechStyle.CHEERFUL else SpeechStyle.CALM)
        return result.response
    }

    override fun onDestroy() {
        recognizer.release()
        voice.release()
        super.onDestroy()
    }
}

@Composable
private fun RommyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lavender,
            secondary = Pink,
            background = Ink,
            surface = Panel,
            surfaceVariant = PanelRaised,
            onPrimary = Ink,
            onBackground = Color(0xFFF7F1FF),
            onSurface = Color(0xFFF7F1FF),
            onSurfaceVariant = Muted
        ),
        content = content
    )
}

private data class ChatMessage(val text: String, val fromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RommyAssistant(
    voice: VoiceCoordinator,
    onListen: () -> Unit,
    onAsk: suspend (String, List<Pair<String, String>>) -> String,
    onSaveApiKey: (String) -> Unit,
    apiKeyConfigured: Boolean
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var prompt by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var showCapabilities by remember { mutableStateOf(true) }
    var isThinking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var hasApiKey by remember { mutableStateOf(apiKeyConfigured) }
    val drawerState = androidx.compose.material3.rememberDrawerState(
        if (drawerOpen) androidx.compose.material3.DrawerValue.Open else androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("подключение Gemini") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (hasApiKey) "ключ уже сохранён на устройстве. введи новый, чтобы заменить его, или оставь поле пустым для удаления." else "добавь ключ Google AI Studio, чтобы включить настоящий ИИ-диалог.", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSaveApiKey(apiKeyInput); hasApiKey = apiKeyInput.isNotBlank(); apiKeyInput = ""; showSettings = false }) { Text("сохранить") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("отмена") } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Panel,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimeMark(Modifier.size(48.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("rommy", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("твой аниме-ассистент", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    NavigationDrawerItem(
                        label = { Text("＋   новый чат") },
                        selected = false,
                        onClick = {
                            messages.clear()
                            showCapabilities = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("◷   недавние чаты") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    Spacer(Modifier.weight(1f))
                    Card(colors = CardDefaults.cardColors(containerColor = PanelRaised)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("ромми учится", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "подключение нейросети, память и расширенные инструменты — следующие этапы разработки.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    NavigationDrawerItem(
                        label = { Text("⚙   настройки") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showSettings = true
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("rommy", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("аниме-помощница", color = Muted, fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("☰", fontSize = 22.sp, color = Lavender)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            messages.clear()
                            showCapabilities = true
                        }) { Text("＋", fontSize = 25.sp, color = Lavender) }
                        Spacer(Modifier.width(4.dp))
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Ink,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Surface(color = Ink, tonalElevation = 0.dp) {
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(30.dp))
                                .background(PanelRaised)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            IconButton(onClick = { showCapabilities = !showCapabilities }) {
                                Text("＋", color = Lavender, fontSize = 24.sp)
                            }
                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("спроси что-нибудь…", color = Muted) },
                                maxLines = 5,
                                shape = RoundedCornerShape(22.dp),
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(onClick = {
                                listening = true
                                onListen()
                            }) { Text("♩", color = if (listening) Pink else Lavender, fontSize = 23.sp) }
                            IconButton(onClick = {
                                val value = prompt.trim()
                                if (value.isNotEmpty()) {
                                    messages.add(ChatMessage(value, true))
                                    prompt = ""
                                    val history = messages.dropLast(1).map { (if (it.fromUser) "user" else "assistant") to it.text }
                                    isThinking = true
                                    showCapabilities = false
                                    scope.launch {
                                        val answer = onAsk(value, history)
                                        messages.add(ChatMessage(answer, false))
                                        isThinking = false
                                    }
                                }
                            }) {
                                Box(
                                    Modifier.size(42.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Lavender, Pink))),
                                    contentAlignment = Alignment.Center
                                ) { Text("↑", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Text(
                            "rommy может ошибаться. проверяй важную информацию.",
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                            textAlign = TextAlign.Center,
                            color = Muted.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        ) { padding ->
            if (messages.isEmpty()) {
                WelcomeScreen(
                    modifier = Modifier.padding(padding),
                    onPrompt = { prompt = it },
                    showCapabilities = showCapabilities
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                    if (isThinking) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimeMark(Modifier.size(34.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("думаю…", color = Lavender, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onPrompt: (String) -> Unit,
    showCapabilities: Boolean
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        AnimeMark(Modifier.size(104.dp))
        Spacer(Modifier.height(24.dp))
        Text("привет, я ромми", fontSize = 31.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
        Text("твой маленький помощник в большом мире.", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(28.dp))
        AnimatedVisibility(visible = showCapabilities) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("с чего начнём?", color = Lavender, fontWeight = FontWeight.SemiBold)
                SuggestionCard("✦", "помоги разобраться", "объясни сложную тему простыми словами") { onPrompt("объясни сложную тему простыми словами") }
                SuggestionCard("⌁", "организуй мой день", "составь план на сегодня") { onPrompt("составь план на сегодня") }
                SuggestionCard("◈", "быстрые действия", "поставь таймер на 10 минут") { onPrompt("поставь таймер на 10 минут") }
            }
        }
    }
}

@Composable
private fun SuggestionCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = Lavender, fontSize = 22.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Muted, fontSize = 24.sp)
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.fromUser) {
            AnimeMark(Modifier.size(34.dp))
            Spacer(Modifier.width(9.dp))
        }
        Surface(
            color = if (message.fromUser) Color(0xFF343047) else Panel,
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (message.fromUser) 22.dp else 6.dp,
                bottomEnd = if (message.fromUser) 6.dp else 22.dp
            ),
            modifier = Modifier.fillMaxWidth(if (message.fromUser) 0.84f else 0.9f)
        ) {
            Text(message.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp), fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun AnimeMark(modifier: Modifier = Modifier) {
    Box(
        modifier.clip(CircleShape).background(
            Brush.linearGradient(listOf(Color(0xFF9D86E9), Color(0xFFED9DC1), Color(0xFF6E6BC4)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Text("り", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
}
