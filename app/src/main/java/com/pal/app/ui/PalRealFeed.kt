package com.pal.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var selectingPhoto by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                selectingPhoto = true
                publishError = null
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        val bounds = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }

                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, bounds)
                        }

                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                            throw Exception("Image illisible.")
                        }

                        var sampleSize = 1
                        while (
                            bounds.outWidth / sampleSize > 1920 ||
                            bounds.outHeight / sampleSize > 1920
                        ) {
                            sampleSize *= 2
                        }

                        val options = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        }

                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)
                        } ?: throw Exception("Impossible de lire cette image.")
                    }
                    val encoded = withContext(Dispatchers.Default) {
                        compressPostPhoto(bitmap)
                    }
                    selectedBitmap = bitmap
                    photoBase64 = encoded
                } catch (e: Exception) {
                    publishError = e.message ?: "Impossible de charger cette image."
                } finally {
                    selectingPhoto = false
                }
            }
        }
    }

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

                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !publishing && !selectingPhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectingPhoto) "Chargement..."
                        else if (photoBase64 != null) "Changer la photo"
                        else "Ajouter une photo",
                        color = Color.White
                    )
                }

                selectedBitmap?.let { bitmap ->
                    Spacer(Modifier.height(10.dp))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Aperçu de la publication",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                    TextButton(
                        onClick = {
                            selectedBitmap = null
                            photoBase64 = null
                        },
                        enabled = !publishing
                    ) {
                        Text("Retirer la photo", color = FeedMuted)
                    }
                }

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
                                    SocialRepository.createPost(
                                        text = postText.trim(),
                                        photoData = photoBase64?.let {
                                            "data:image/webp;base64,$it"
                                        },
                                        photoMimeType = photoBase64?.let { "image/webp" }
                                    )

                                if (response.optBoolean("success")) {
                                    postText = ""
                                    selectedBitmap = null
                                    photoBase64 = null
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
                    enabled = (postText.isNotBlank() || photoBase64 != null) && !publishing && !selectingPhoto,
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
            ProfileAvatar(
                photoURL = post.author.photoURL,
                modifier = Modifier.size(48.dp),
                fallbackText = authorName
            )

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

        post.photoData?.takeIf { it.isNotBlank() }?.let { photo ->
            PostPhoto(photoData = photo)
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


@Composable
private fun PostPhoto(photoData: String) {
    val bitmap = remember(photoData) {
        runCatching {
            val encoded = photoData.substringAfter(",", photoData)
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Photo de la publication",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

private fun compressPostPhoto(source: Bitmap): String {
    var width = source.width
    var height = source.height
    var bitmap = source

    while (true) {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 82, output)
        val bytes = output.toByteArray()

        if (bytes.size <= 420_000) {
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        if (width <= 160 || height <= 160) {
            throw Exception("Cette image est trop volumineuse.")
        }

        width = (width * 0.8).toInt()
        height = (height * 0.8).toInt()
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
