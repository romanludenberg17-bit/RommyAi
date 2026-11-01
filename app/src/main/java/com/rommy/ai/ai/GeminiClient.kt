package com.rommy.ai.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Gemini REST client. Network work is always performed away from the main thread. */
class GeminiClient(
    private val model: String = "gemini-2.5-flash"
) {

    /**
     * Streams incremental text chunks from Gemini's SSE endpoint. The caller can
     * render tokens as they arrive and cancel the flow to close the HTTP request.
     */
    fun stream(apiKey: String, prompt: String, history: List<Pair<String, String>>): Flow<String> = channelFlow {
        require(apiKey.isNotBlank()) { "сначала добавь Gemini API key в настройках." }
        val contents = JSONArray()
        history.takeLast(20).forEach { (role, text) ->
            if (text.isNotBlank()) {
                contents.put(
                    JSONObject()
                        .put("role", if (role == "assistant") "model" else "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", text)))
                )
            }
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )
        val payload = JSONObject().put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION))))
            .put("generationConfig", JSONObject().put("temperature", 0.85).put("maxOutputTokens", 4096))
        val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$encodedKey")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            withContext(Dispatchers.IO) {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                if (status !in 200..299) {
                    val body = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val message = json?.optJSONObject("error")?.optString("message")
                    throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "ошибка Gemini API ($status)")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isBlank() || data == "[DONE]") continue
                        val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
                        val parts = json.optJSONArray("candidates")
                            ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                            ?: continue
                        for (index in 0 until parts.length()) {
                            val part = parts.optJSONObject(index) ?: continue
                            val chunk = part.optString("text")
                            if (chunk.isNotEmpty()) trySend(chunk)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        awaitClose { connection.disconnect() }
    }

    suspend fun generate(apiKey: String, prompt: String, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            require(apiKey.isNotBlank()) { "сначала добавь Gemini API key в настройках." }
            val contents = JSONArray()
            history.takeLast(20).forEach { (role, text) ->
                if (text.isNotBlank()) {
                    contents.put(
                        JSONObject()
                            .put("role", if (role == "assistant") "model" else "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", text)))
                    )
                }
            }
            contents.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            )

            val payload = JSONObject().put("contents", contents)
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION))))
                .put("generationConfig", JSONObject().put("temperature", 0.85).put("maxOutputTokens", 4096))
            val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
            val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (status !in 200..299) {
                    val message = json?.optJSONObject("error")?.optString("message")
                    throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "ошибка Gemini API ($status)")
                }
                val candidates = json?.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                val answer = buildString {
                    if (parts != null) for (index in 0 until parts.length()) {
                        val part = parts.optJSONObject(index)
                        if (part?.has("text") == true) append(part.optString("text"))
                    }
                }.trim()
                answer.ifBlank { "модель не вернула текстовый ответ. попробуй переформулировать запрос." }
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val SYSTEM_INSTRUCTION = "Ты Ромми — дружелюбная аниме-помощница с живым характером. Общайся на языке пользователя, естественно и тепло, без навязчивой восторженности. Ты умеешь объяснять сложное простыми словами, помогать с планированием и рассуждать. Не выдумывай факты и честно говори о неизвестном. Отвечай содержательно, но без лишней воды. Не утверждай, что выполнила действия на телефоне, если приложение не подтвердило их выполнение."
    }
}
