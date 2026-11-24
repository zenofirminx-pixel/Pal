package com.pal.app.network

import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.URI

object SessionManager {

    private val cookieManager = CookieManager(
        null,
        CookiePolicy.ACCEPT_ALL
    )

    @Volatile
    private var csrfToken: String? = null

    @Synchronized
    fun saveCookies(uri: URI, headers: Map<String, List<String>>) {
        cookieManager.put(uri, headers)
    }

    @Synchronized
    fun getCookies(uri: URI): Map<String, List<String>> {
        return cookieManager.get(uri, emptyMap())
    }

    fun saveCsrfToken(token: String) {
        csrfToken = token
    }

    fun getCsrfToken(): String? = csrfToken

    @Synchronized
    fun clear() {
        csrfToken = null
        cookieManager.cookieStore.removeAll()
    }

    fun hasSession(): Boolean {
        val uri = URI(ApiConfig.BASE_URL)
        return cookieManager.cookieStore.get(uri)
            .any { it.name == "pal_session" }
    }
}
