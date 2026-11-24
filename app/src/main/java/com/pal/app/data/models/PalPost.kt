package com.pal.app.data.models

import org.json.JSONObject

data class PalUser(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val photoURL: String,
    val verified: Boolean
)

data class PalPost(
    val id: String,
    val author: PalUser,
    val text: String,
    val photoData: String?,
    val photoMimeType: String?,
    val createdAt: Long,
    val likesCount: Int,
    val commentsCount: Int,
    val likedByMe: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): PalPost {
            val authorJson = json.optJSONObject("author") ?: JSONObject()

            return PalPost(
                id = json.optString("id"),
                author = PalUser(
                    uid = authorJson.optString("uid"),
                    firstName = authorJson.optString("firstName"),
                    lastName = authorJson.optString("lastName"),
                    username = authorJson.optString("username"),
                    photoURL = authorJson.optString("photoURL"),
                    verified = authorJson.optBoolean("verified")
                ),
                text = json.optString("text"),
                photoData = json.optString("photoData").takeIf { it.isNotBlank() && it != "null" },
                photoMimeType = json.optString("photoMimeType").takeIf { it.isNotBlank() && it != "null" },
                createdAt = json.optLong("createdAt"),
                likesCount = json.optInt("likesCount"),
                commentsCount = json.optInt("commentsCount"),
                likedByMe = json.optBoolean("likedByMe")
            )
        }
    }
}
