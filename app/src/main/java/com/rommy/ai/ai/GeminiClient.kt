package com.rommy.ai.ai

import kotlinx.coroutines.Dispatchers
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
                .put("generationConfig", JSONObject().put("temperature", 0.8).put("maxOutputTokens", 2048))
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
}
