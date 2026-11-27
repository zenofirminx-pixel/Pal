package com.pal.app.data

import com.pal.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object AssistantRepository {

    suspend fun sendMessage(message: String): String =
        withContext(Dispatchers.IO) {

            val body = JSONObject()
                .put("message", message)
                .put("history", JSONArray())
                .put("language", "fr")

            val response = ApiClient.post(
                "/api/assistant/chat",
                body
            )

            val data = response.optJSONObject("data")
                ?: response.optJSONObject("result")
                ?: response

            if (!response.optBoolean("success")) {
                throw IllegalStateException(
                    response.optString(
                        "message",
                        "Impossible de contacter Pal AI."
                    )
                )
            }

            val reply = data.optString("reply")
                .ifBlank { response.optString("reply") }

            if (reply.isBlank()) {
                throw IllegalStateException(
                    "Le serveur n'a retourné aucune réponse."
                )
            }

            reply
        }


    suspend fun getHistory(): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val response = ApiClient.get("/api/assistant/history")

            if (!response.optBoolean("success")) {
                throw IllegalStateException(
                    response.optString(
                        "message",
                        "Impossible de récupérer les conversations."
                    )
                )
            }

            val data = response.optJSONObject("data")
                ?: response.optJSONObject("result")
                ?: response

            val messages = data.optJSONArray("messages")
                ?: org.json.JSONArray()

            buildList {
                for (index in 0 until messages.length()) {
                    val item = messages.optJSONObject(index) ?: continue
                    val role = item.optString("role")
                    val content = item.optString("content")

                    if (
                        (role == "user" || role == "assistant") &&
                        content.isNotBlank()
                    ) {
                        add(role to content)
                    }
                }
            }
        }
\}
