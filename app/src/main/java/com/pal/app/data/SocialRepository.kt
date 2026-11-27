package com.pal.app.data

import com.pal.app.data.models.PalPost
import com.pal.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FeedResult(
    val success: Boolean,
    val posts: List<PalPost>,
    val nextCursor: String?,
    val message: String
)

object SocialRepository {

    suspend fun getFeed(
        limit: Int = 20,
        before: String? = null
    ): FeedResult = withContext(Dispatchers.IO) {
        val path = buildString {
            append("/api/social/feed?limit=$limit")
            if (!before.isNullOrBlank()) {
                append("&before=$before")
            }
        }

        val response = ApiClient.get(path)
        val array = response.optJSONArray("posts")
        val posts = mutableListOf<PalPost>()

        if (array != null) {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                posts.add(PalPost.fromJson(item))
            }
        }

        FeedResult(
            success = response.optBoolean("success"),
            posts = posts,
            nextCursor = response.optString("nextCursor").takeIf { it.isNotBlank() && it != "null" },
            message = response.optString("message")
        )
    }

    suspend fun createPost(
        text: String,
        photoData: String? = null,
        photoMimeType: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("text", text)
            if (!photoData.isNullOrBlank()) {
                put("photoData", photoData)
                put("photoMimeType", photoMimeType)
            }
        }

        ApiClient.post("/api/social/posts", body)
    }

    suspend fun deletePost(postId: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.delete("/api/social/posts/$postId")
        }

    suspend fun likePost(postId: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.post(
                "/api/social/posts/$postId/like",
                JSONObject()
            )
        }

    suspend fun getComments(postId: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.get("/api/social/posts/$postId/comments")
        }

    suspend fun addComment(
        postId: String,
        text: String
    ): JSONObject = withContext(Dispatchers.IO) {
        ApiClient.post(
            "/api/social/posts/$postId/comments",
            JSONObject().put("text", text)
        )
    }


    suspend fun searchUsers(query: String): JSONObject =
        withContext(Dispatchers.IO) {
            val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            ApiClient.get("/api/social/users/search?q=$encoded")
        }

    suspend fun getConversations(): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.get("/api/social/conversations")
        }

    suspend fun getMessages(uid: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.get("/api/social/messages/$uid")
        }

    suspend fun sendMessage(uid: String, text: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.post(
                "/api/social/messages/$uid",
                JSONObject().put("text", text)
            )
        }

    suspend fun getNotifications(): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.get("/api/social/notifications")
        }

    suspend fun markNotificationRead(notificationId: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.patch(
                "/api/social/notifications/$notificationId/read",
                JSONObject()
            )
        }

    suspend fun sendFriendRequest(uid: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.post(
                "/api/social/friends/$uid/request",
                JSONObject()
            )
        }

    suspend fun getPublicProfile(uid: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.get("/api/social/users/$uid")
        }

    suspend fun acceptFriendRequest(uid: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.post(
                "/api/social/friends/$uid/accept",
                JSONObject()
            )
        }

    suspend fun declineFriendRequest(uid: String): JSONObject =
        withContext(Dispatchers.IO) {
            ApiClient.post(
                "/api/social/friends/$uid/decline",
                JSONObject()
            )
        }

}
