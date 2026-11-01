# Rommy AI

Android voice assistant with an anime character, bilingual speech, wake-word activation, app control, timers, TTS and an optional AI mode.

## Direction

Rommy AI is being built as a native Android 17 application using Kotlin + Jetpack Compose. Android 17 is API 37.

### Planned core

- Russian + English speech recognition with automatic language detection
- wake-word activation and hands-free listening
- lock-screen / background assistant flow where Android permits it
- timer and alarm actions
- installed-app discovery and launching by package name / label
- assistant-style intents and command routing
- AI conversation mode with pluggable LLM backends
- anime-style TTS voice with replaceable voice packs
- expressive character system
- VRM / Live2D character support instead of a static PNG-only mascot
- lip-sync, blinking, idle motion and emotion transitions
- optional local/offline speech and AI components

## Open-source references

The implementation will use compatible open-source components where their licenses permit redistribution. Candidate references include Anime AI (VRM + animation + TTS + lip-sync), ChatdollKit (virtual assistant dialog and wake-word architecture), Open VRM Companion (emotion/animation architecture), and Android-oriented assistant automation projects.

Third-party code and assets will remain separated and credited with their original licenses.

## Project status

The repository currently contains the initial Android 17 / Compose foundation. Voice, wake-word, command execution and avatar layers are being added incrementally.
