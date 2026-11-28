package com.pal.app.data.models

import org.json.JSONObject

data class PalStory(
    val id: String,
    val photoData: String,
    val photoMimeType: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    companion object {
        fun fromJson(json: JSONObject): PalStory {
            return PalStory(
                id = json.optString("id"),
                photoData = json.optString("photoData"),
                photoMimeType = json.optString("photoMimeType"),
                createdAt = json.optLong("createdAt"),
                expiresAt = json.optLong("expiresAt")
            )
        }
    }
}

data class PalStoryGroup(
    val author: PalUser,
    val stories: List<PalStory>
)
