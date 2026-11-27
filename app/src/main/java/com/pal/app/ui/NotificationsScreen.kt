package com.pal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.SocialRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

private val NotificationBlue = Color(0xFF087BFF)
private val NotificationCard = Color(0xFF0D1015)

@Composable
fun NotificationsScreen(onOpenProfile: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val notifications = remember { mutableStateListOf<JSONObject>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadNotifications() {
        loading = true
        error = null

        try {
            val response = SocialRepository.getNotifications()

            if (!response.optBoolean("success")) {
                error = response.optString(
                    "message",
                    "Impossible de charger les notifications."
                )
            } else {
                notifications.clear()
                val array = response.optJSONArray("notifications")

                if (array != null) {
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.let { notifications.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de connexion."
        }

        loading = false
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notifications",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { scope.launch { loadNotifications() } }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Actualiser",
                    tint = NotificationBlue
                )
            }
        }

        when {
            loading && notifications.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NotificationBlue)
                }
            }

            error != null && notifications.isEmpty() -> {
                EmptyNotificationState(
                    message = error ?: "Une erreur est survenue.",
                    action = { scope.launch { loadNotifications() } }
                )
            }

            notifications.isEmpty() -> {
                EmptyNotificationState(
                    message = "Tu n'as pas encore de notifications.",
                    action = { scope.launch { loadNotifications() } }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.optString("id") }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                val id = notification.optString("id")
                                val type = notification.optString("type")
                                val actorUid = notification.optString("actorUid")

                                if (type == "friend_request" && actorUid.isNotBlank()) {
                                    onOpenProfile(actorUid)
                                }

                                if (id.isNotBlank() &&
                                    !notification.optBoolean("read")
                                ) {
                                    scope.launch {
                                        try {
                                            val result =
                                                SocialRepository.markNotificationRead(id)

                                            if (result.optBoolean("success")) {
                                                notification.put("read", true)
                                            }
                                        } catch (_: Exception) {
                                            error = "Impossible de mettre à jour cette notification."
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: JSONObject,
    onClick: () -> Unit
) {
    val read = notification.optBoolean("read")
    val actor = notification.optJSONObject("actor")

    val actorName = actor?.let {
        listOf(
            it.optString("firstName"),
            it.optString("lastName")
        ).filter { part -> part.isNotBlank() }.joinToString(" ")
    }.orEmpty().ifBlank {
        notification.optString("actorUid", "Un utilisateur")
    }

    val type = notification.optString("type")

    val message = when (type) {
        "like" -> "$actorName a aimé une publication."
        "comment" -> "$actorName a commenté une publication."
        "friend_request" -> "$actorName t'a envoyé une demande d'ami."
        "friend_accepted" -> "$actorName a accepté ta demande d'ami."
        "message" -> "$actorName t'a envoyé un message."
        else -> "$actorName : nouvelle activité sur Pal."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (read) NotificationCard else Color(0xFF14243A)
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        NotificationBlue.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = NotificationBlue
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    message,
                    color = Color.White,
                    fontWeight = if (read) {
                        FontWeight.Normal
                    } else {
                        FontWeight.SemiBold
                    }
                )

                val date = notification.optString("createdAt")

                if (date.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        date,
                        color = Color(0xFF8C929D),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (!read) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(9.dp)
                        .background(NotificationBlue, RoundedCornerShape(50))
                )
            } else {
                Icon(
                    Icons.Default.Done,
                    contentDescription = "Lue",
                    tint = Color(0xFF8C929D),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationState(
    message: String,
    action: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Color(0xFF8C929D),
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(message, color = Color(0xFF8C929D))

            Spacer(Modifier.height(14.dp))

            TextButton(onClick = action) {
                Text("Réessayer", color = NotificationBlue)
            }
        }
    }
}
