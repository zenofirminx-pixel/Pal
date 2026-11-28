package com.pal.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

private val ProfileBlue = Color(0xFF087BFF)
private val ProfileBlack = Color(0xFF000000)
private val ProfileCard = Color(0xFF0D1015)
private val ProfileMuted = Color(0xFF8C929D)
private val ProfileWhite = Color.White

@Composable
fun PublicProfileScreen(
    uid: String,
    onBack: () -> Unit,
    onOpenChat: (JSONObject) -> Unit
) {
    var user by remember { mutableStateOf<JSONObject?>(null) }
    var relationship by remember { mutableStateOf("none") }
    var posts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadProfile() {
        loading = true
        error = null
        try {
            val response = com.pal.app.data.SocialRepository.getPublicProfile(uid)
            if (response.optBoolean("success")) {
                user = response.optJSONObject("user")
                relationship = response.optString("relationship", "none")
                val array = response.optJSONArray("posts")
                posts = (0 until (array?.length() ?: 0)).mapNotNull {
                    array?.optJSONObject(it)
                }
            } else {
                val status = response.optInt("httpStatus", 0)
                val code = response.optString("code", "UNKNOWN")
                val message = response.optString("message", "Réponse invalide")
                error = "Erreur profil (HTTP $status) : $code — $message"
            }
        } catch (e: Exception) {
            error = e.message ?: "Une erreur est survenue."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(uid) {
        loadProfile()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProfileBlack
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = ProfileWhite
                    )
                }
                Text(
                    "Profil",
                    color = ProfileWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            when {
                loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ProfileBlue)
                }

                error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error!!, color = ProfileWhite)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { scope.launch { loadProfile() } },
                        colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue)
                    ) {
                        Text("Réessayer")
                    }
                }

                user != null -> {
                    val profile = user!!
                    val firstName = profile.optString("firstName")
                    val lastName = profile.optString("lastName")
                    val fullName = "$firstName $lastName".trim()
                    val username = profile.optString("username")

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ProfileAvatar(
                                    photoURL = profile.optString("photoURL")
                                        .takeIf { it.isNotBlank() && it != "null" },
                                    modifier = Modifier.size(104.dp),
                                    fallbackText = firstName.firstOrNull()?.uppercase() ?: "P"
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        fullName.ifBlank { "Utilisateur Pal" },
                                        color = ProfileWhite,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (profile.optBoolean("verified")) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("✓", color = ProfileBlue, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (username.isNotBlank()) {
                                    Text(
                                        "@$username",
                                        color = ProfileMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                when (relationship) {
                                    "none" -> Button(
                                        enabled = !actionLoading,
                                        onClick = {
                                            scope.launch {
                                                actionLoading = true
                                                try {
                                                    val result = com.pal.app.data.SocialRepository.sendFriendRequest(uid)
                                                    if (result.optBoolean("success")) {
                                                        val status = result.optString("status")
                                                        relationship = if (status.contains("incoming", true)) {
                                                            "incoming_pending"
                                                        } else {
                                                            "outgoing_pending"
                                                        }
                                                    } else {
                                                        error = "La demande n'a pas pu être envoyée."
                                                    }
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Erreur lors de l'envoi."
                                                } finally {
                                                    actionLoading = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue)
                                    ) {
                                        Text(if (actionLoading) "Patientez..." else "Ajouter comme ami")
                                    }

                                    "outgoing_pending" -> Button(
                                        enabled = false,
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = ProfileCard,
                                            disabledContentColor = ProfileMuted
                                        )
                                    ) {
                                        Text("Demande envoyée")
                                    }

                                    "incoming_pending" -> Button(
                                        enabled = !actionLoading,
                                        onClick = {
                                            scope.launch {
                                                actionLoading = true
                                                try {
                                                    val result = com.pal.app.data.SocialRepository.acceptFriendRequest(uid)
                                                    if (result.optBoolean("success")) {
                                                        relationship = "friends"
                                                    } else {
                                                        error = "Impossible d'accepter la demande."
                                                    }
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Erreur lors de l'acceptation."
                                                } finally {
                                                    actionLoading = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue)
                                    ) {
                                        Text(if (actionLoading) "Patientez..." else "Accepter la demande")
                                    }

                                    "friends" -> Button(
                                        onClick = { onOpenChat(profile) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue)
                                    ) {
                                        Text("Envoyer un message")
                                    }

                                    "self" -> Text(
                                        "Votre profil",
                                        color = ProfileMuted
                                    )
                                }

                                Spacer(Modifier.height(22.dp))
                                HorizontalDivider(color = ProfileCard)
                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "Publications",
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ProfileWhite,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        if (posts.isEmpty()) {
                            item {
                                Text(
                                    "Aucune publication pour le moment.",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    color = ProfileMuted
                                )
                            }
                        } else {
                            items(posts) { post ->
                                ProfilePostCard(post)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePostCard(post: JSONObject) {
    val text = post.optString("text")
    val photoData = post.optString("photoData")
        .takeIf { it.isNotBlank() && it != "null" }

    val bitmap = remember(photoData) {
        try {
            photoData?.let {
                val encoded = it.substringAfter(",", it)
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ProfileCard)
    ) {
        Column(Modifier.padding(14.dp)) {
            if (text.isNotBlank()) {
                Text(
                    text,
                    color = ProfileWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (bitmap != null) {
                Spacer(Modifier.height(10.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Image de publication",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "♥ ${post.optInt("likesCount")}",
                    color = ProfileMuted
                )
                Spacer(Modifier.width(18.dp))
                Text(
                    "Commentaires : ${post.optInt("commentsCount")}",
                    color = ProfileMuted
                )
            }
        }
    }
}
