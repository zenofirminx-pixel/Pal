package com.pal.app.data

import com.pal.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SecurityResult(
    val success: Boolean,
    val message: String,
    val twoFactorEnabled: Boolean = false,
    val qrCode: String? = null,
    val manualEntryKey: String? = null,
    val recoveryCodes: List<String> = emptyList(),
    val httpStatus: Int = 0
)

object SecurityRepository {

    private fun result(response: JSONObject): SecurityResult {
        val codes = response.optJSONArray("recoveryCodes")
        val recoveryCodes = buildList {
            if (codes != null) {
                for (index in 0 until codes.length()) {
                    add(codes.optString(index))
                }
            }
        }

        return SecurityResult(
            success = response.optBoolean("success"),
            message = response.optString("message"),
            twoFactorEnabled = response
                .optJSONObject("user")
                ?.optBoolean("twoFactorEnabled") ?: false,
            qrCode = response.optString("qrCode").takeIf { it.isNotBlank() },
            manualEntryKey = response.optString("manualEntryKey")
                .takeIf { it.isNotBlank() },
            recoveryCodes = recoveryCodes,
            httpStatus = response.optInt("httpStatus")
        )
    }

    suspend fun getStatus(): SecurityResult =
        withContext(Dispatchers.IO) {
            result(ApiClient.get("/api/auth/me"))
        }

    suspend fun setup(currentPassword: String): SecurityResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("currentPassword", currentPassword)
            }
            result(ApiClient.post("/api/auth/2fa/setup", body))
        }

    suspend fun confirm(code: String): SecurityResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("code", code.trim())
            }
            result(ApiClient.post("/api/auth/2fa/confirm", body))
        }

    suspend fun disable(
        currentPassword: String,
        code: String
    ): SecurityResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("currentPassword", currentPassword)
            put("code", code.trim())
        }
        result(ApiClient.post("/api/auth/2fa/disable", body))
    }
}
