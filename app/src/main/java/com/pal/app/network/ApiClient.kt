package com.pal.app.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object ApiClient {

    private val baseUri = URI(ApiConfig.BASE_URL)

    fun get(path: String): JSONObject {
        return request("GET", path)
    }

    fun post(path: String, body: JSONObject): JSONObject {
        return request("POST", path, body)
    }

    fun patch(path: String, body: JSONObject): JSONObject {
        return request("PATCH", path, body)
    }

    fun delete(path: String): JSONObject {
        return request("DELETE", path)
    }

    @Synchronized
    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        retryCsrf: Boolean = true
    ): JSONObject {

        if (method in listOf("POST", "PATCH", "DELETE")) {
            if (SessionManager.getCsrfToken() == null) {
                fetchCsrfToken()
            }
        }

        val url = URL(
            ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = ApiConfig.CONNECT_TIMEOUT
            connection.readTimeout = ApiConfig.READ_TIMEOUT

            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")

            val cookies = SessionManager.getCookies(baseUri)
            cookies.forEach { (name, values) ->
                if (values.isNotEmpty()) {
                    connection.setRequestProperty(name, values.joinToString("; "))
                }
            }

            if (method in listOf("POST", "PATCH", "DELETE")) {
                SessionManager.getCsrfToken()?.let {
                    connection.setRequestProperty("X-CSRF-Token", it)
                }
            }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode

            SessionManager.saveCookies(
                baseUri,
                connection.headerFields
            )

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use {
                it.readText()
            } ?: "{}"

            val response = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject().put("raw", responseText)
            }

            if (status == 403 &&
                response.optString("code") == "CSRF_INVALID" &&
                retryCsrf
            ) {
                fetchCsrfToken()
                return request(method, path, body, false)
            }

            response.put("httpStatus", status)
            return response

        } finally {
            connection.disconnect()
        }
    }

    @Synchronized
    private fun fetchCsrfToken() {
        val connection = URL(
            ApiConfig.BASE_URL.trimEnd('/') + "/api/auth/csrf"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = ApiConfig.CONNECT_TIMEOUT
            connection.readTimeout = ApiConfig.READ_TIMEOUT
            connection.setRequestProperty("Accept", "application/json")

            val status = connection.responseCode

            SessionManager.saveCookies(
                baseUri,
                connection.headerFields
            )

            if (status !in 200..299) {
                throw IllegalStateException("Impossible de récupérer le CSRF : HTTP $status")
            }

            val text = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val response = JSONObject(text)
            val token = response.optString("csrfToken")

            if (!response.optBoolean("success") || token.isBlank()) {
                throw IllegalStateException("Réponse CSRF invalide")
            }

            SessionManager.saveCsrfToken(token)

        } finally {
            connection.disconnect()
        }
    }
}
