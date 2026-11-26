package com.pal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.AssistantRepository
import kotlinx.coroutines.launch

private val AiBlue = Color(0xFF087BFF)
private val AiCard = Color(0xFF0D1015)

private data class AiMessage(
    val fromUser: Boolean,
    val text: String
)

@Composable
fun PalAiScreen(onBack: () -> Unit = {}) {

    var messages by remember {
        mutableStateOf(
            listOf(
                AiMessage(
                    false,
                    "Bonjour ! Je suis Pal AI. Comment puis-je t'aider aujourd'hui ?"
                )
            )
        )
    }

    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var loadingHistory by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val history = AssistantRepository.getHistory()

            if (history.isNotEmpty()) {
                messages = history.map { (role, content) ->
                    AiMessage(
                        fromUser = role == "user",
                        text = content
                    )
                }
            }
        } catch (e: Exception) {
            error = e.message
                ?: "Impossible de récupérer les anciennes conversations."
        } finally {
            loadingHistory = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Box(
                Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(AiBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Pal AI",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "Pal AI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "Assistant intelligent",
                    color = AiBlue,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        HorizontalDivider(color = AiCard)

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            itemsIndexed(messages) { _, message ->

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {

                    Surface(
                        color = if (message.fromUser) {
                            AiBlue
                        } else {
                            AiCard
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {

                        Text(
                            message.text,
                            color = Color.White,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (loadingHistory) {
                item {
                    Text(
                        "Récupération de la conversation...",
                        color = AiBlue,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (sending) {
                item {
                    Text(
                        "Pal AI réfléchit...",
                        color = AiBlue,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        error?.let {
            Text(
                it,
                color = Color(0xFFFF7777),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(AiCard)
                .navigationBarsPadding()
                .imePadding()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) {
                            Text(
                                "Écris à Pal AI...",
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                enabled = !sending && !loadingHistory && input.isNotBlank(),
                onClick = {

                    val message = input.trim()

                    if (message.isNotEmpty()) {

                        messages = messages + AiMessage(
                            true,
                            message
                        )

                        input = ""
                        error = null
                        sending = true

                        scope.launch {
                            try {
                                val reply =
                                    AssistantRepository.sendMessage(message)

                                messages = messages + AiMessage(
                                    false,
                                    reply
                                )

                            } catch (e: Exception) {
                                error = e.message
                                    ?: "Une erreur est survenue."
                            } finally {
                                sending = false
                            }
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Envoyer",
                    tint = AiBlue
                )
            }
        }
    }
}
