package com.pal.app.data

import com.pal.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ProfileResult(
    val success: Boolean,
    val message: String,
    val profile: JSONObject? = null,
    val photoURL: String? = null,
    val httpStatus: Int = 0
)

object ProfileRepository {

    private fun result(response: JSONObject): ProfileResult {
        return ProfileResult(
            success = response.optBoolean("success"),
            message = response.optString("message"),
            profile = response.optJSONObject("profile"),
            photoURL = response.optString("photoURL").takeIf { it.isNotBlank() },
            httpStatus = response.optInt("httpStatus")
        )
    }

    suspend fun getProfile(): ProfileResult =
        withContext(Dispatchers.IO) {
            result(ApiClient.get("/api/profile"))
        }

    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        username: String,
        birthDate: String,
        gender: String
    ): ProfileResult = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("firstName", firstName.trim())
            put("lastName", lastName.trim())
            put("username", username.trim())
            put("birthDate", birthDate.trim())
            put("gender", gender)
        }

        result(ApiClient.patch("/api/profile", body))
    }

    suspend fun uploadAvatar(imageBase64: String): ProfileResult =
        withContext(Dispatchers.IO) {

            val body = JSONObject().apply {
                put("imageBase64", imageBase64)
            }

            result(ApiClient.post("/api/profile/avatar", body))
        }
}
