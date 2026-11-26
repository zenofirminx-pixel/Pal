package com.pal.app.network

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI

object SessionManager {

    private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    private const val PREFS_NAME = "pal_secure_session"
    private const val KEY_COOKIES = "cookies"
    private const val KEY_CSRF = "csrf_token"
    private const val KEY_CSRF_TIME = "csrf_time"
    private const val CSRF_VALIDITY = 2 * 60 * 60 * 1000L

    private var preferences: SharedPreferences? = null

    @Volatile
    private var csrfToken: String? = null

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        preferences = prefs

        val saved = runCatching {
            JSONArray(prefs.getString(KEY_COOKIES, "[]"))
        }.getOrDefault(JSONArray())

        val restored = JSONArray()
        val now = System.currentTimeMillis()

        for (i in 0 until saved.length()) {
            val item = saved.optJSONObject(i) ?: continue
            val header = item.optString("header")
            val expiresAt = item.optLong("expiresAt", 0L)
            val remaining = expiresAt - now

            if (header.isBlank() || remaining <= 0L) continue

            val adjusted = header.replace(
                Regex("(?i)(max-age\\s*=\\s*)\\d+")
            ) { match ->
                match.groupValues[1] + (remaining / 1000L).coerceAtLeast(1L)
            }

            runCatching {
                cookieManager.put(
                    URI(ApiConfig.BASE_URL),
                    mapOf("Set-Cookie" to listOf(adjusted))
                )
            }

            restored.put(item)
        }

        prefs.edit().putString(KEY_COOKIES, restored.toString()).apply()

        val savedToken = prefs.getString(KEY_CSRF, null)
        val savedTime = prefs.getLong(KEY_CSRF_TIME, 0L)

        csrfToken = if (
            savedToken != null &&
            now - savedTime < CSRF_VALIDITY &&
            hasCookie("pal_csrf")
        ) {
            savedToken
        } else {
            prefs.edit()
                .remove(KEY_CSRF)
                .remove(KEY_CSRF_TIME)
                .apply()
            null
        }
    }

    @Synchronized
    fun saveCookies(uri: URI, headers: Map<String, List<String>>) {
        cookieManager.put(uri, headers)

        val newHeaders = headers.entries
            .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value }

        if (newHeaders.isEmpty()) return

        val prefs = preferences ?: return
        val saved = runCatching {
            JSONArray(prefs.getString(KEY_COOKIES, "[]"))
        }.getOrDefault(JSONArray())

        val records = mutableListOf<JSONObject>()

        for (i in 0 until saved.length()) {
            saved.optJSONObject(i)?.let { records.add(it) }
        }

        for (header in newHeaders) {
            val cookies = runCatching {
                HttpCookie.parse(header)
            }.getOrDefault(emptyList())

            for (cookie in cookies) {
                records.removeAll {
                    it.optString("name") == cookie.name
                }

                if (cookie.maxAge == 0L || cookie.hasExpired()) continue

                val expiresAt = if (cookie.maxAge > 0L) {
                    System.currentTimeMillis() + cookie.maxAge * 1000L
                } else {
                    Long.MAX_VALUE
                }

                records.add(
                    JSONObject()
                        .put("name", cookie.name)
                        .put("header", header)
                        .put("expiresAt", expiresAt)
                )
            }
        }

        val output = JSONArray()
        records.forEach { output.put(it) }

        prefs.edit()
            .putString(KEY_COOKIES, output.toString())
            .apply()
    }

    @Synchronized
    fun getCookies(uri: URI): Map<String, List<String>> {
        return cookieManager.get(uri, emptyMap())
    }

    @Synchronized
    fun saveCsrfToken(token: String) {
        csrfToken = token

        preferences?.edit()
            ?.putString(KEY_CSRF, token)
            ?.putLong(KEY_CSRF_TIME, System.currentTimeMillis())
            ?.apply()
    }

    @Synchronized
    fun getCsrfToken(): String? {
        val savedTime = preferences?.getLong(KEY_CSRF_TIME, 0L) ?: 0L

        if (
            csrfToken != null &&
            System.currentTimeMillis() - savedTime >= CSRF_VALIDITY
        ) {
            csrfToken = null
            preferences?.edit()
                ?.remove(KEY_CSRF)
                ?.remove(KEY_CSRF_TIME)
                ?.apply()
        }

        return csrfToken
    }

    private fun hasCookie(name: String): Boolean {
        return cookieManager.cookieStore
            .get(URI(ApiConfig.BASE_URL))
            .any { it.name == name && !it.hasExpired() }
    }

    @Synchronized
    fun clear() {
        csrfToken = null
        cookieManager.cookieStore.removeAll()
        preferences?.edit()?.clear()?.apply()
    }

    @Synchronized
    fun hasSession(): Boolean {
        return hasCookie("pal_session")
    }
}
