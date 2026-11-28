package com.pal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PersonAdd
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

private val SearchBlue = Color(0xFF087BFF)
private val SearchCard = Color(0xFF10141B)

@Composable
fun SearchScreen(onOpenProfile: (String) -> Unit = {}, onOpenChat: (JSONObject) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val users = remember { mutableStateListOf<JSONObject>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    suspend fun searchUsers() {
        val term = query.trim()
        if (term.length < 2) {
            users.clear()
            searched = false
            error = null
            return
        }

        loading = true
        error = null
        searched = true

        try {
            val response = SocialRepository.searchUsers(term)

            if (response.optBoolean("success")) {
                users.clear()
                val array = response.optJSONArray("users")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.let { users.add(it) }
                    }
                }
            } else {
                error = response.optString(
                    "message",
                    "La recherche a échoué."
                )
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de connexion."
        }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Recherche",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SearchCard, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF8C929D)
            )

            Spacer(Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Nom ou nom d'utilisateur...",
                                color = Color(0xFF777F8C)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            TextButton(
                onClick = { scope.launch { searchUsers() } }
            ) {
                Text("Chercher", color = SearchBlue)
            }
        }

        Spacer(Modifier.height(18.dp))

        when {
            loading -> {
                Box(
                    Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SearchBlue)
                }
            }

            error != null -> {
                Text(
                    error ?: "Erreur inconnue.",
                    color = Color(0xFFFF7777),
                    modifier = Modifier.padding(12.dp)
                )
            }

            !searched -> {
                Text(
                    "Recherche des personnes sur Pal.",
                    color = Color(0xFF8C929D)
                )
            }

            users.isEmpty() -> {
                Text(
                    "Aucun utilisateur trouvé.",
                    color = Color(0xFF8C929D)
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(
                        items = users,
                        key = { it.optString("uid") }
                    ) { user ->
                        SearchUserItem(user, onClick = { onOpenProfile(user.optString("uid")) }, onOpenChat = onOpenChat)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(
    user: JSONObject,
    onClick: () -> Unit,
    onOpenChat: (JSONObject) -> Unit
) {
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var relationship by remember(user.optString("uid")) {
        mutableStateOf(user.optString("relationship", "none"))
    }
    var message by remember { mutableStateOf<String?>(null) }

    val firstName = user.optString("firstName")
    val lastName = user.optString("lastName")
    val fullName = "$firstName $lastName".trim()
        .ifBlank { user.optString("username", "Utilisateur Pal") }
    val username = user.optString("username")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = SearchCard)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    photoURL = user.optString("photoURL").takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    modifier = Modifier.size(46.dp),
                    fallbackText = fullName.firstOrNull()?.uppercase() ?: "?"
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        fullName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (username.isNotBlank()) {
                        Text(
                            "@$username",
                            color = Color(0xFF8C929D),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (user.optBoolean("verified")) {
                        Text(
                            "Compte vérifié",
                            color = SearchBlue,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                when (relationship) {
                    "none" -> {
                        Button(
                            enabled = !sending,
                            onClick = {
                                scope.launch {
                                    sending = true
                                    message = null
                                    try {
                                        val result = SocialRepository.sendFriendRequest(
                                            user.optString("uid")
                                        )
                                        if (result.optBoolean("success")) {
                                            relationship = "outgoing_pending"
                                            user.put("relationship", relationship)
                                        } else {
                                            message = result.optString(
                                                "message",
                                                "Demande impossible."
                                            )
                                        }
                                    } catch (e: Exception) {
                                        message = e.message ?: "Erreur de connexion."
                                    }
                                    sending = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SearchBlue
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        ) {
                            if (sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Ajouter comme ami")
                            }
                        }
                    }

                    "outgoing_pending", "incoming_pending", "friends" -> {
                        Button(
                            onClick = { onOpenChat(user) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SearchBlue
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 18.dp,
                                vertical = 8.dp
                            )
                        ) {
                            Text("Chat")
                        }
                    }

                    else -> {
                        Text(
                            "Indisponible",
                            color = Color(0xFF8C929D),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (message != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    message ?: "",
                    color = Color(0xFFFF7777),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
