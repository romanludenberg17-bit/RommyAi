package com.rommy.ai.voice

import kotlin.random.Random

/**
 * Keeps assistant replies from sounding like the same canned sentence every time.
 * The language model remains responsible for factual/content generation; this layer
 * controls short acknowledgements and spoken delivery style.
 */
class ResponseComposer(
    private val random: Random = Random.Default,
) {
    fun acknowledgement(action: String): String = when (random.nextInt(5)) {
        0 -> "$action готово."
        1 -> "сделано — $action."
        2 -> "есть. $action выполнено."
        3 -> "готово. $action."
        else -> "всё, $action сделано."
    }

    fun error(): String = listOf(
        "не получилось выполнить эту команду.",
        "что-то пошло не так. попробую ещё раз.",
        "не сработало. давай попробуем другим способом.",
        "я не смогла выполнить это действие."
    ).random(random)

    fun greeting(): String = listOf(
        "я слушаю.",
        "да, я здесь.",
        "слушаю тебя.",
        "что нужно сделать?",
        "я готова."
    ).random(random)
}
