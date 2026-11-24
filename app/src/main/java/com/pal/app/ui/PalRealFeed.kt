package com.pal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.SocialRepository
import com.pal.app.data.models.PalPost
import kotlinx.coroutines.launch
import org.json.JSONObject

private val FeedBlue = Color(0xFF087BFF)
private val FeedCard = Color(0xFF0D1015)
private val FeedMuted = Color(0xFF8C929D)

@Composable
fun PalRealFeedContent() {
    var posts by remember { mutableStateOf<List<PalPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var postText by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun loadFeed() {
        loading = true
        error = null

        try {
            val result = SocialRepository.getFeed()
            if (result.success) {
                posts = result.posts
            } else {
                error = result.message.ifBlank {
                    "Impossible de charger les publications."
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de connexion au serveur."
        }

        loading = false
    }

    LaunchedEffect(Unit) {
        loadFeed()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Fil d’actualité",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(
                    onClick = {
                        scope.launch { loadFeed() }
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualiser",
                        tint = FeedBlue
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(FeedCard)
                    .padding(15.dp)
            ) {
                Text(
                    "Créer une publication",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = postText,
                    onValueChange = {
                        if (it.length <= 5000) postText = it
                        publishError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Quoi de neuf ?") },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !publishing
                )

                publishError?.let {
                    Text(it, color = Color(0xFFFF6B6B))
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch {
                            publishing = true
                            publishError = null
                            try {
                                val response =
                                    SocialRepository.createPost(postText.trim())

                                if (response.optBoolean("success")) {
                                    postText = ""
                                    loadFeed()
                                } else {
                                    publishError = response.optString(
                                        "message",
                                        "La publication a échoué."
                                    )
                                }
                            } catch (e: Exception) {
                                publishError = e.message
                                    ?: "Erreur lors de la publication."
                            } finally {
                                publishing = false
                            }
                        }
                    },
                    enabled = postText.isNotBlank() && !publishing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FeedBlue
                    )
                ) {
                    if (publishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Publier")
                    }
                }
            }
        }

        when {
            loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 65.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FeedBlue)
                    }
                }
            }

            error != null -> {
                item {
                    FeedMessage(
                        title = "Chargement impossible",
                        message = error ?: "",
                        action = "Réessayer",
                        onAction = {
                            scope.launch { loadFeed() }
                        }
                    )
                }
            }

            posts.isEmpty() -> {
                item {
                    FeedMessage(
                        title = "Aucune publication",
                        message = "Les nouvelles publications apparaîtront ici.",
                        action = "Actualiser",
                        onAction = {
                            scope.launch { loadFeed() }
                        }
                    )
                }
            }

            else -> {
                items(posts, key = { it.id }) { post ->
                    RealPostCard(
                        post = post,
                        onLike = {
                            scope.launch {
                                try {
                                    val response =
                                        SocialRepository.likePost(post.id)

                                    if (response.optBoolean("success")) {
                                        val updated = posts.map { current ->
                                            if (current.id == post.id) {
                                                current.copy(
                                                    likedByMe = response.optBoolean(
                                                        "liked",
                                                        !current.likedByMe
                                                    ),
                                                    likesCount = response.optInt(
                                                        "likesCount",
                                                        current.likesCount
                                                    )
                                                )
                                            } else {
                                                current
                                            }
                                        }
                                        posts = updated
                                    }
                                } catch (_: Exception) {
                                    error = "Impossible d’envoyer ton J’aime."
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RealPostCard(
    post: PalPost,
    onLike: () -> Unit
) {
    var showComments by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentSending by remember { mutableStateOf(false) }
    var commentsError by remember { mutableStateOf<String?>(null) }
    var displayedCommentsCount by remember { mutableIntStateOf(post.commentsCount) }
    val commentScope = rememberCoroutineScope()

    suspend fun loadComments() {
        commentsLoading = true
        commentsError = null
        try {
            val response = SocialRepository.getComments(post.id)
            val data = response.optJSONObject("data")
                ?: response.optJSONObject("result")
                ?: response

            if (response.optBoolean("success")) {
                val array = data.optJSONArray("comments")
                comments = (0 until (array?.length() ?: 0)).mapNotNull {
                    array?.optJSONObject(it)
                }
            } else {
                commentsError = response.optString(
                    "message",
                    "Impossible de charger les commentaires."
                )
            }
        } catch (e: Exception) {
            commentsError = e.message ?: "Erreur de connexion."
        } finally {
            commentsLoading = false
        }
    }

    val authorName = listOf(
        post.author.firstName,
        post.author.lastName
    ).filter { it.isNotBlank() }.joinToString(" ")
        .ifBlank { post.author.username.ifBlank { "Utilisateur Pal" } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(FeedCard)
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF174F9C)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    authorName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        authorName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    if (post.author.verified) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(FeedBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✓",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (post.author.username.isNotBlank()) {
                    Text(
                        "@${post.author.username}",
                        color = FeedMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (post.text.isNotBlank()) {
            Text(
                text = post.text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 15.dp,
                    bottom = 13.dp
                )
            )
        }

        if (!post.photoData.isNullOrBlank()) {
            Text(
                "Cette publication contient une photo.",
                color = FeedMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        HorizontalDivider(color = Color(0xFF252B35))

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onLike() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (post.likedByMe) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = "J’aime",
                    tint = if (post.likedByMe) FeedBlue else FeedMuted
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    post.likesCount.toString(),
                    color = Color.White
                )
            }

            Spacer(Modifier.width(25.dp))

            Row(
                modifier = Modifier.clickable {
                    showComments = true
                    commentScope.launch { loadComments() }
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = "Commentaires",
                    tint = FeedMuted
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    displayedCommentsCount.toString(),
                    color = Color.White
                )
            }
        }
    }

    if (showComments) {
        AlertDialog(
            onDismissRequest = { showComments = false },
            containerColor = FeedCard,
            title = {
                Text("Commentaires", color = Color.White)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                ) {
                    when {
                        commentsLoading -> {
                            Box(
                                Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = FeedBlue)
                            }
                        }

                        commentsError != null -> {
                            Text(
                                commentsError ?: "",
                                color = Color(0xFFFF6B6B)
                            )
                            TextButton(
                                onClick = {
                                    commentScope.launch { loadComments() }
                                }
                            ) {
                                Text("Réessayer")
                            }
                        }

                        comments.isEmpty() -> {
                            Text(
                                "Aucun commentaire pour le moment.",
                                color = FeedMuted
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(comments) { comment ->
                                    val author = comment.optJSONObject("author")
                                    val name = listOf(
                                        author?.optString("firstName").orEmpty(),
                                        author?.optString("lastName").orEmpty()
                                    ).filter { it.isNotBlank() }
                                        .joinToString(" ")
                                        .ifBlank {
                                            author?.optString("username")
                                                ?.takeIf { it.isNotBlank() }
                                                ?: "Utilisateur Pal"
                                        }

                                    Column {
                                        Text(
                                            name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            comment.optString("text"),
                                            color = FeedMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = commentText,
                        onValueChange = {
                            if (it.length <= 2000) commentText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Écrire un commentaire...")
                        },
                        enabled = !commentSending,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = commentText.isNotBlank() && !commentSending,
                    onClick = {
                        commentScope.launch {
                            commentSending = true
                            try {
                                val response = SocialRepository.addComment(
                                    post.id,
                                    commentText.trim()
                                )
                                if (response.optBoolean("success")) {
                                    commentText = ""
                                    displayedCommentsCount++
                                    loadComments()
                                } else {
                                    commentsError = response.optString(
                                        "message",
                                        "Impossible d'ajouter le commentaire."
                                    )
                                }
                            } catch (e: Exception) {
                                commentsError = e.message
                                    ?: "Erreur lors de l'envoi."
                            } finally {
                                commentSending = false
                            }
                        }
                    }
                ) {
                    Text(
                        if (commentSending) "Envoi..." else "Envoyer",
                        color = FeedBlue
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showComments = false }) {
                    Text("Fermer", color = FeedMuted)
                }
            }
        )
    }
}

@Composable
private fun FeedMessage(
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            message,
            color = FeedMuted
        )

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = FeedBlue
            )
        ) {
            Text(action)
        }
    }
}
