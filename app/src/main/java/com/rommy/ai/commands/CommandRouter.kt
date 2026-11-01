package com.rommy.ai.commands

import com.rommy.ai.apps.AppLauncher
import com.rommy.ai.timers.TimerManager
import com.rommy.ai.voice.ResponseComposer
import java.util.Locale
import kotlin.math.roundToLong

/** Deterministic local commands. AI/LLM routing can be added behind this boundary later. */
class CommandRouter(
    private val launcher: AppLauncher,
    private val timerManager: TimerManager,
    private val responses: ResponseComposer = ResponseComposer(),
) {
    fun handle(text: String): CommandResult {
        val input = text.trim()
        val lower = input.lowercase(Locale.getDefault())
        if (input.isBlank()) return CommandResult(false, responses.error())

        if (lower in setOf("привет", "здравствуй", "ты тут", "ты здесь", "hello", "hi")) {
            return CommandResult(true, responses.greeting())
        }

        val appName = extractAppName(lower, input)
        if (appName != null) {
            val found = launcher.find(appName)
            if (found.isNotEmpty() && launcher.launchPackage(found.first().packageName)) {
                return CommandResult(true, responses.acknowledgement("открыла ${found.first().label}"))
            }
            return CommandResult(false, "не нашла приложение «$appName».")
        }

        if (lower.contains("таймер") || lower.contains("timer")) {
            val seconds = parseDurationSeconds(lower)
                ?: return CommandResult(false, "скажи, например: поставь таймер на 10 минут.")
            return if (timerManager.schedule(seconds)) {
                CommandResult(true, responses.acknowledgement("таймер на ${formatDuration(seconds)}"))
            } else {
                CommandResult(false, "на телефоне не разрешены точные таймеры. открой разрешение точных будильников для Rommy AI.")
            }
        }

        return CommandResult(false, "я пока не знаю эту команду. можно открыть приложение или поставить таймер.")
    }

    private fun extractAppName(lower: String, original: String): String? {
        val patterns = listOf("открой ", "запусти ", "открыть ", "запустить ", "open ", "launch ")
        val prefix = patterns.firstOrNull { lower.startsWith(it) } ?: return null
        return original.drop(prefix.length).trim().takeIf { it.isNotBlank() }
    }

    private fun parseDurationSeconds(text: String): Long? {
        val match = Regex("(\\d+(?:[.,]\\d+)?)\\s*(секунд|секунды|сек|s|минут|минуты|мин|m|час|часа|ч|h)").find(text) ?: return null
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2]) {
            "час", "часа", "ч", "h" -> 3600.0
            "минут", "минуты", "мин", "m" -> 60.0
            else -> 1.0
        }
        return (value * multiplier).roundToLong().coerceAtLeast(1)
    }

    private fun formatDuration(seconds: Long): String = when {
        seconds >= 3600L -> "${seconds / 3600} ч"
        seconds >= 60L -> "${seconds / 60} мин"
        else -> "$seconds сек"
    }
}

data class CommandResult(val handled: Boolean, val response: String)
