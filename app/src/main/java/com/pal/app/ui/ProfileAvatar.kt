package com.pal.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ProfileAvatar(
    photoURL: String?,
    modifier: Modifier = Modifier,
    fallbackText: String = "P"
) {
    val bitmap = remember(photoURL) {
        runCatching {
            val encoded = photoURL
                ?.substringAfter(",")
                ?.takeIf { it.isNotBlank() }
                ?: return@runCatching null
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo de profil",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFF174F9C)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.ifBlank { "P" }.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
