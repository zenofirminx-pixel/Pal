package com.pal.app.data

import com.pal.app.network.ApiClient
import com.pal.app.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AuthResult(
    val success: Boolean,
    val message: String,
    val user: JSONObject? = null,
    val twoFactorRequired: Boolean = false,
    val httpStatus: Int = 0,
    val code: String? = null
)

object AuthRepository {

    private fun result(response: JSONObject): AuthResult {
        return AuthResult(
            success = response.optBoolean("success"),
            message = response.optString("message"),
            user = response.optJSONObject("user"),
            twoFactorRequired = response.optBoolean("twoFactorRequired"),
            httpStatus = response.optInt("httpStatus"),
            code = response.optString("code").takeIf { it.isNotBlank() }
        )
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        birthDate: String,
        gender: String,
        email: String,
        password: String,
        username: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("firstName", firstName.trim())
            put("lastName", lastName.trim())
            put("birthDate", birthDate)
            put("gender", gender)
            put("email", email.trim())
            put("password", password)

            if (!username.isNullOrBlank()) {
                put("username", username.trim())
            }
        }

        result(ApiClient.post("/api/auth/register", body))
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }

        result(ApiClient.post("/api/auth/login", body))
    }

    suspend fun verifyTwoFactor(
        code: String
    ): AuthResult = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("code", code.trim())
        }

        result(ApiClient.post("/api/auth/2fa/login", body))
    }

    suspend fun currentUser(): AuthResult = withContext(Dispatchers.IO) {
        result(ApiClient.get("/api/auth/me"))
    }

    suspend fun logout(): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.post(
                "/api/auth/logout",
                JSONObject()
            )

            val authResult = result(response)

            if (authResult.success) {
                SessionManager.clear()
            }

            authResult
        } catch (exception: Exception) {
            AuthResult(
                success = false,
                message = exception.message ?: "Erreur de déconnexion."
            )
        }
    }
}
