package com.rommy.ai.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StoredChatMessage(val role: String, val text: String)
data class StoredConversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<StoredChatMessage>
)

/** Keeps recent conversations in the app's private storage. */
class ChatHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    fun conversations(): List<StoredConversation> = readAll().sortedByDescending { it.updatedAt }

    fun activeMessages(): List<StoredChatMessage> {
        var id = preferences.getString(ACTIVE_ID, null)
        if (id == null || readAll().none { it.id == id }) {
            createNewChat()
            id = preferences.getString(ACTIVE_ID, null)
        }
        return readAll().firstOrNull { it.id == id }?.messages.orEmpty()
    }

    fun saveActive(messages: List<StoredChatMessage>) {
        val all = readAll().toMutableList()
        var id = preferences.getString(ACTIVE_ID, null)
        if (id == null || all.none { it.id == id }) {
            createNewChat()
            id = preferences.getString(ACTIVE_ID, null)
        }
        val current = all.firstOrNull { it.id == id }
        val firstPrompt = messages.firstOrNull { it.role == "user" }?.text
        val title = if (current?.title == null || current.title == "новый чат") {
            firstPrompt?.take(42) ?: "новый чат"
        } else current.title
        val updated = StoredConversation(id!!, title, System.currentTimeMillis(), messages.takeLast(100))
        all.removeAll { it.id == id }
        all.add(updated)
        writeAll(all.sortedByDescending { it.updatedAt }.take(MAX_CONVERSATIONS))
    }

    fun createNewChat() {
        val conversation = StoredConversation(UUID.randomUUID().toString(), "новый чат", System.currentTimeMillis(), emptyList())
        val all = readAll().toMutableList()
        all.add(conversation)
        writeAll(all.sortedByDescending { it.updatedAt }.take(MAX_CONVERSATIONS))
        preferences.edit().putString(ACTIVE_ID, conversation.id).apply()
    }

    fun activate(id: String) {
        if (readAll().any { it.id == id }) preferences.edit().putString(ACTIVE_ID, id).apply()
    }

    private fun readAll(): List<StoredConversation> = runCatching {
        val array = JSONArray(preferences.getString(CONVERSATIONS, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val messagesJson = item.optJSONArray("messages") ?: JSONArray()
                val messages = buildList {
                    for (j in 0 until messagesJson.length()) {
                        val message = messagesJson.optJSONObject(j) ?: continue
                        add(StoredChatMessage(message.optString("role"), message.optString("text")))
                    }
                }
                add(
                    StoredConversation(
                        item.optString("id"),
                        item.optString("title", "новый чат"),
                        item.optLong("updatedAt"),
                        messages
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun writeAll(conversations: List<StoredConversation>) {
        val array = JSONArray()
        conversations.forEach { conversation ->
            val messages = JSONArray()
            conversation.messages.forEach { message ->
                messages.put(JSONObject().put("role", message.role).put("text", message.text))
            }
            array.put(
                JSONObject()
                    .put("id", conversation.id)
                    .put("title", conversation.title)
                    .put("updatedAt", conversation.updatedAt)
                    .put("messages", messages)
            )
        }
        preferences.edit().putString(CONVERSATIONS, array.toString()).apply()
    }

    private companion object {
        const val CONVERSATIONS = "conversations"
        const val ACTIVE_ID = "active_conversation"
        const val MAX_CONVERSATIONS = 30
    }
}
