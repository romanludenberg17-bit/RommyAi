# Rommy AI architecture

## Layers

```text
Android UI / Assistant surface
        |
        v
Assistant Orchestrator
  |       |        |        |
  v       v        v        v
Speech   Intent   AI       Avatar
STT      Engine   Router   Runtime
  |       |        |        |
  v       v        v        v
Wake     Apps     LLM      VRM/Live2D
Word     Timer    Local    Emotion
VAD      Media    Remote   Lip-sync

                 |
                 v
             TTS Engine
```

## 1. Speech

Use a provider interface so Rommy can switch between offline and online engines without changing command logic.

```kotlin
interface SpeechRecognizer {
    suspend fun recognize(audio: ByteArray): SpeechResult
}

interface WakeWordEngine {
    fun start()
    fun stop()
}
```

Candidate implementations include Vosk / sherpa-onnx / Whisper-family models. The final Android choice will be benchmarked for latency, RAM and battery use.

## 2. Command engine

Commands are typed as structured intents instead of being executed directly from an LLM string.

Examples:

- `OpenApp(packageName)`
- `SetTimer(duration)`
- `LaunchUrl(url)`
- `SetVolume(stream, level)`
- `StartAssistantConversation()`

The LLM may propose an intent, but the Android command layer validates it before execution.

## 3. App launching

Installed applications are discovered with Android's package APIs. User-facing app names and package IDs are indexed locally so commands such as "открой Telegram" can resolve to the correct installed package, including non-Play and modified APK installations.

## 4. Avatar

The primary runtime target is a real animated character rather than a sheet of PNGs.

Preferred asset path:

`VRM 1.0 -> expression controller -> animation mixer -> lip sync -> Compose/Surface renderer`

Fallback:

`Live2D -> expression controller -> motion controller -> lip sync`

Static PNGs remain useful for notifications, onboarding and low-power fallback UI.

## 5. Voice output

```kotlin
interface SpeechSynthesizer {
    suspend fun speak(text: String, voice: VoiceId, locale: Locale)
    fun stop()
}
```

Voice packs are data modules. The default voice is intended to be an anime-girl style voice; additional voices can be installed without changing the assistant engine.

## 6. Android 17

Target SDK 37. Background microphone operation, notifications, assistant role behavior and lock-screen activation must follow Android's current permission and foreground-service rules rather than relying on undocumented behavior.
