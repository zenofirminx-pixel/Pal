package com.pal.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private val AvatarBlue = Color(0xFF087BFF)
private val AvatarBlack = Color(0xFF000000)
private val AvatarCard = Color(0xFF0D1015)
private val AvatarMuted = Color(0xFF8C929D)

@Composable
fun AvatarEditScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loading = true
                error = ""
                message = ""
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        } ?: throw Exception("Impossible de lire cette image.")
                    }

                    if (bitmap.width < 1 || bitmap.height < 1) {
                        throw Exception("Image invalide.")
                    }

                    val encoded = withContext(Dispatchers.Default) {
                        compressAvatar(bitmap)
                    }

                    selectedBitmap = bitmap
                    imageBase64 = encoded
                } catch (e: Exception) {
                    error = e.message ?: "Impossible de charger cette image."
                }
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AvatarBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }
            Text(
                "Photo de profil",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(AvatarCard),
                contentAlignment = Alignment.Center
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Aperçu de la photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = AvatarMuted,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Choisissez votre photo de profil",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Format WebP, taille maximale : 120 Ko.",
                color = AvatarMuted
            )

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = { picker.launch("image/*") },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("Choisir une photo", color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val encoded = imageBase64 ?: return@Button
                    scope.launch {
                        loading = true
                        error = ""
                        message = ""
                        try {
                            val result = ProfileRepository.uploadAvatar(encoded)
                            if (result.success) {
                                message = "Photo de profil enregistrée."
                            } else {
                                error = result.message.ifBlank {
                                    "Échec de l'enregistrement."
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Erreur pendant l'envoi."
                        }
                        loading = false
                    }
                },
                enabled = imageBase64 != null && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AvatarBlue
                ),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("Enregistrer la photo", fontWeight = FontWeight.Bold)
            }

            if (loading) {
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(color = AvatarBlue)
            }

            if (error.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(error, color = Color(0xFFFF6B6B))
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(message, color = AvatarBlue)
            }
        }
    }
}

private fun compressAvatar(source: Bitmap): String {
    var width = source.width
    var height = source.height
    var bitmap = source

    while (true) {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 85, output)
        val bytes = output.toByteArray()

        if (bytes.size <= 120 * 1024) {
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        if (width <= 160 || height <= 160) {
            throw Exception("Cette image ne peut pas être compressée sous 120 Ko.")
        }

        width = (width * 0.8).toInt()
        height = (height * 0.8).toInt()
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
