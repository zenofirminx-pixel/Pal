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
}
