# Rommy AI voice architecture

## goal

speech must sound natural, varied and characterful in both Russian and English. The assistant should not repeat one canned phrase or speak every sentence with identical prosody.

## engine strategy

Rommy AI uses a replaceable `SpeechEngine` interface.

1. **Android TTS fallback** — available immediately and useful when no neural voice is installed.
2. **Neural offline engine** — planned as the primary voice path. Supertonic 3 is a strong candidate because it supports Russian and English and runs locally through ONNX Runtime. An Android implementation already demonstrates on-device Supertonic 3 playback and model caching.
3. **Additional voice packs** — voices are treated as downloadable model/style packages rather than hard-coded into the assistant.

The engine must support:

- Russian and English
- selectable female character voices
- speech rate / pitch
- sentence and paragraph chunking
- pre-generation of the next sentence
- audio caching
- interruption / barge-in
- offline operation after model download

## expressive delivery

The assistant separates **what is said** from **how it is said**.

`ResponseComposer` varies short acknowledgements and errors. The LLM remains responsible for the actual answer. `SpeechStyle` controls delivery states such as calm, cheerful, soft, serious and surprised.

Future neural backends should also map mascot state to delivery:

- listening → quiet / short acknowledgement
- thinking → calm, slower speech
- success → cheerful
- warning → serious
- confusion → softer / slightly higher pitch
- AI conversation → natural default

## important licensing note

Do not bundle a voice model until its individual model/voice license has been checked. Supertonic's model license and each third-party voice pack license must be preserved in the repository's attribution/third-party notices.

## reference projects

- Supertonic 3 Android implementation: https://github.com/ouor/supertonic-android
- Supertonic Kotlin Multiplatform wrapper: https://github.com/nedmah/supertonic-kmp
- Kokoro Android: https://github.com/ffmpegkit-maintained/kokoro-android
- Piper Android: https://github.com/piper-tts-apps/piper-app-android
- HayaiTTS: https://github.com/HayaiApp/HayaiTTS

These are reference implementations. Code and models are not copied blindly; license compatibility is checked before redistribution.
