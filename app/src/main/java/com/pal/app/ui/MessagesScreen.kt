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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.SocialRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

private val MsgBlue = Color(0xFF087BFF)
private val MsgCard = Color(0xFF0D1015)
private val MsgMuted = Color(0xFF8C929D)

@Composable
fun MessagesScreen(onOpenPalAi: () -> Unit = {}) {
    var conversations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<JSONObject?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun loadConversations() {
        loading = true
        error = null
        try {
            val response = SocialRepository.getConversations()
            if (response.optBoolean("success")) {
                val array = response.optJSONArray("conversations")
                conversations = buildList {
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            array.optJSONObject(i)?.let { add(it) }
                        }
                    }
                }
            } else {
                error = response.optString("message", "Impossible de charger les conversations.")
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de connexion."
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        loadConversations()
    }

    val current = selectedUser

    if (current != null) {
        ConversationScreen(
            user = current,
            onBack = { selectedUser = null }
        )
    } else {
        Column(
            Modifier.fillMaxSize().background(Color.Black)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Messages",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    scope.launch { loadConversations() }
                }) {
                    Icon(Icons.Default.Refresh, "Actualiser", tint = MsgBlue)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MsgCard)
                            .clickable { onOpenPalAi() }
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MsgBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(13.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "Pal AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Ton assistant intelligent",
                                color = MsgMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                when {
                    loading -> item {
                        Box(
                            Modifier.fillMaxWidth().padding(25.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MsgBlue)
                        }
                    }

                    error != null -> item {
                        MessageNotice(error ?: "", "Réessayer") {
                            scope.launch { loadConversations() }
                        }
                    }

                    conversations.isEmpty() -> item {
                        MessageNotice(
                            "Aucune autre conversation pour le moment.",
                            "Actualiser"
                        ) {
                            scope.launch { loadConversations() }
                        }
                    }

                    else -> items(
                        conversations,
                        key = { it.optString("id") }
                    ) { conversation ->
                        val user = conversation.optJSONObject("user")
                            ?: JSONObject()

                        val name = listOf(
                            user.optString("firstName"),
                            user.optString("lastName")
                        ).filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank {
                                user.optString("username", "Utilisateur Pal")
                            }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MsgCard)
                                .clickable { selectedUser = user }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF174F9C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.width(13.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    conversation.optString("lastMessage"),
                                    color = MsgMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationScreen(
    user: JSONObject,
    onBack: () -> Unit
) {
    val uid = user.optString("uid")
    val name = listOf(
        user.optString("firstName"),
        user.optString("lastName")
    ).filter { it.isNotBlank() }.joinToString(" ")
        .ifBlank { user.optString("username", "Utilisateur Pal") }

    var messages by remember(uid) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var loading by remember(uid) { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun loadMessages() {
        try {
            val response = SocialRepository.getMessages(uid)
            if (response.optBoolean("success")) {
                val array = response.optJSONArray("messages")
                messages = buildList {
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            array.optJSONObject(i)?.let { add(it) }
                        }
                    }
                }
                error = null
            } else {
                error = response.optString("message", "Impossible de charger les messages.")
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de connexion."
        }
        loading = false
    }

    LaunchedEffect(uid) {
        loadMessages()
    }

    Column(
        Modifier.fillMaxSize().background(Color.Black)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
            }
            Text(
                name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch { loadMessages() }
            }) {
                Icon(Icons.Default.Refresh, "Actualiser", tint = MsgBlue)
            }
        }

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MsgBlue)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "Aucun message dans cette conversation.",
                            color = MsgMuted
                        )
                    }
                }

                items(messages, key = { it.optString("id") }) { message ->
                    val mine = message.optString("senderUid") != uid
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            message.optString("text"),
                            color = Color.White,
                            modifier = Modifier
                                .widthIn(max = 290.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 17.dp,
                                        topEnd = 17.dp,
                                        bottomStart = if (mine) 17.dp else 4.dp,
                                        bottomEnd = if (mine) 4.dp else 17.dp
                                    )
                                )
                                .background(if (mine) MsgBlue else MsgCard)
                                .padding(13.dp)
                        )
                    }
                }
            }
        }

        error?.let {
            Text(it, color = Color(0xFFFF7777), modifier = Modifier.padding(horizontal = 12.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 4000) text = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(25.dp))
                    .background(MsgCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("Écrire un message...", color = MsgMuted)
                        inner()
                    }
                }
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                enabled = text.isNotBlank() && !sending && uid.isNotBlank(),
                onClick = {
                    val message = text.trim()
                    scope.launch {
                        sending = true
                        try {
                            val response = SocialRepository.sendMessage(uid, message)
                            if (response.optBoolean("success")) {
                                text = ""
                                loadMessages()
                            } else {
                                error = response.optString("message", "Envoi impossible.")
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Erreur d'envoi."
                        }
                        sending = false
                    }
                }
            ) {
                Icon(Icons.Default.Send, "Envoyer", tint = MsgBlue)
            }
        }
    }
}

@Composable
private fun MessageNotice(
    message: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(25.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MsgMuted)
        Spacer(Modifier.height(15.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = MsgBlue)) {
            Text(action)
        }
    }
}
