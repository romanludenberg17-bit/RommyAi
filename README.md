# Rommy AI

**Rommy** is a native Android AI assistant with an anime-inspired personality and interface. The product direction is a Gemini-style assistant experience: conversational AI, voice interaction, useful phone actions, persistent chats and an expressive character.

## Current implementation

- Kotlin + Jetpack Compose, Android API 26+
- Dark expressive anime-inspired chat UI
- Gemini API integration using the `gemini-2.5-flash` model
- Multi-turn conversation context
- API key encrypted with Android Keystore
- Persistent recent conversations stored locally
- Local commands for launching installed apps and setting timers
- Android speech recognition and text-to-speech fallback
- TTS waits for asynchronous engine initialization instead of silently dropping early speech

## First launch

1. Install the APK from the latest successful GitHub Actions run.
2. Create a Gemini API key in [Google AI Studio](https://aistudio.google.com/apikey).
3. Open Rommy → menu → **Настройки** and save the key.
4. Start chatting. Local actions such as opening apps and setting timers are handled on-device; other prompts are sent to Gemini.

The API key is encrypted using Android Keystore and is not committed to the repository. Gemini API usage may be billed or rate-limited according to Google's current terms.

## Product direction

The goal is a capable personal assistant, not just a chat screen. Planned development areas:

- Streaming responses and cancellation
- Full voice conversation mode with interruption and hands-free controls
- Natural neural Russian/English voice packs and expressive delivery
- Anime character artwork, animation, lip-sync and emotion states
- Camera and image understanding
- Tool/function calling for Android actions, timers, reminders and app control
- Search and other external tools
- Better memory controls and conversation management
- Optional local/offline models where device performance and model licences allow

## Build

GitHub Actions builds the Android debug APK on every push. Download the `RommyAi-debug` artifact from the latest successful run in the repository's Actions tab.

## Licence

See [LICENSE](LICENSE). Third-party model weights, voice packs and libraries will be included only when their licences permit the intended use and redistribution.
