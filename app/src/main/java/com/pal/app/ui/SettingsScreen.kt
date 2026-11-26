package com.pal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.ProfileRepository

private val SettingsBlue = Color(0xFF087BFF)
private val SettingsBlack = Color(0xFF000000)
private val SettingsCard = Color(0xFF0D1015)
private val SettingsMuted = Color(0xFF8C929D)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditPhoto: () -> Unit,
    onEditProfile: () -> Unit,
    onSecurity: () -> Unit,
    onLogout: () -> Unit,
    loading: Boolean = false
) {
    var profilePhotoURL by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            val result = ProfileRepository.getProfile()
            profilePhotoURL = result.profile
                ?.optString("photoURL")
                ?.takeIf { it.isNotBlank() && it != "null" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBlack)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                "Paramètres",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "PROFIL",
            color = SettingsMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SettingsCard)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditPhoto() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    photoURL = profilePhotoURL,
                    modifier = Modifier.size(62.dp),
                    fallbackText = "P"
                )

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "Photo de profil",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Modifier votre photo",
                        color = SettingsMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SettingsMuted
                )
            }

            HorizontalDivider(
                color = Color(0xFF20242B),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            SettingsRow(
                icon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = SettingsBlue
                    )
                },
                title = "Modifier le profil",
                subtitle = "Vos informations personnelles",
                onClick = onEditProfile
            )
        }

        Spacer(Modifier.height(26.dp))

        Text(
            "SÉCURITÉ",
            color = SettingsMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SettingsCard)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingsRow(
                icon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = SettingsBlue
                    )
                },
                title = "Authentification à deux facteurs",
                subtitle = "Gérer la protection de votre compte",
                onClick = onSecurity
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onLogout,
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF20242B),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(21.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null
                )
                Spacer(Modifier.width(10.dp))
                Text("Se déconnecter", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = SettingsMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SettingsMuted
        )
    }
}
