# Rommy AI

native Android voice assistant with an anime character, bilingual speech, wake-word activation, app control, timers, neural TTS and an optional AI mode.

## direction

Rommy AI is being built as a native Android 17 application using Kotlin + Jetpack Compose (API 37).

### core

- Russian + English speech recognition with automatic language detection
- wake-word activation and hands-free listening
- lock-screen / background assistant flow where Android and the device permit it
- timer and alarm actions
- installed-app discovery and launching, including apps installed outside Google Play
- assistant-style intent routing
- AI conversation mode with pluggable LLM backends
- natural female anime-style neural voice with replaceable voice packs
- expressive speech styles and varied short responses
- VRM / Live2D character support
- lip-sync, blinking, idle motion and emotion transitions
- optional local/offline speech and AI components

## voice quality

Voice is treated as a first-class subsystem rather than a thin `TextToSpeech` wrapper.

The app already has a replaceable `SpeechEngine` abstraction and an Android TTS fallback. The primary target is a neural on-device engine with high-quality Russian and English speech, sentence chunking, caching, interruption support and expressive delivery.

See [`docs/VOICE_ARCHITECTURE.md`](docs/VOICE_ARCHITECTURE.md).

## open-source references

Compatible open-source components may be used where their licenses permit redistribution. Current research references include Supertonic 3 Android implementations, Supertonic KMP, Kokoro Android, Piper Android, HayaiTTS, Anime AI and ChatdollKit.

Third-party code, models and voice packs remain separated and credited with their original licenses. A model/voice is not bundled until its redistribution terms are checked.

## project status

The Android 17 / Compose foundation is in place. Voice architecture and expressive speech are now being implemented first; wake word, command execution, app launching, timers and avatar rendering follow as independent modules.
