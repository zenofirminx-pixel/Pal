package com.pal.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pal.app.data.ProfileRepository
import kotlinx.coroutines.launch

private val ProfileBlue = Color(0xFF087BFF)
private val ProfileBlack = Color(0xFF000000)
private val ProfileCard = Color(0xFF0D1015)
private val ProfileMuted = Color(0xFF8C929D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var genderExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val result = ProfileRepository.getProfile()
            if (result.success && result.profile != null) {
                firstName = result.profile.optString("firstName")
                lastName = result.profile.optString("lastName")
                username = result.profile.optString("username")
                birthDate = result.profile.optString("birthDate")
                gender = result.profile.optString("gender")
                email = result.profile.optString("email")
            } else {
                error = result.message.ifBlank { "Impossible de charger le profil." }
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de chargement."
        }
        loading = false
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
            }
            Text(
                "Modifier le profil",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ProfileBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    "INFORMATIONS PERSONNELLES",
                    color = ProfileMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                ProfileField("Prénom", firstName) { firstName = it }
                ProfileField("Nom", lastName) { lastName = it }
                ProfileField("Nom d'utilisateur", username) { username = it }
                ProfileField(
                    "Date de naissance",
                    birthDate,
                    "Format : AAAA-MM-JJ"
                ) { birthDate = it }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Genre", color = Color.White)
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (gender) {
                                "female" -> "Femme"
                                "male" -> "Homme"
                                "other" -> "Autre"
                                "prefer_not_say" -> "Préfère ne pas répondre"
                                else -> "Non renseigné"
                            },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = genderExpanded
                                )
                            },
                            colors = profileFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            listOf(
                                "Non renseigné" to "",
                                "Femme" to "female",
                                "Homme" to "male",
                                "Autre" to "other",
                                "Préfère ne pas répondre" to "prefer_not_say"
                            ).forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        gender = value
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Adresse e-mail", color = Color.White)
                    OutlinedTextField(
                        value = email,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = profileFieldColors()
                    )
                    Text(
                        "L'adresse e-mail ne peut pas être modifiée ici.",
                        color = ProfileMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF6B6B))
                }
                if (message.isNotBlank()) {
                    Text(message, color = ProfileBlue)
                }
            }

            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank()) {
                        error = "Le prénom et le nom sont obligatoires."
                        return@Button
                    }
                    if (!username.matches(Regex("[a-zA-Z0-9_]{3,24}"))) {
                        error = "Le nom d'utilisateur doit contenir 3 à 24 caractères."
                        return@Button
                    }

                    scope.launch {
                        saving = true
                        error = ""
                        message = ""
                        try {
                            val result = ProfileRepository.updateProfile(
                                firstName,
                                lastName,
                                username,
                                birthDate,
                                gender
                            )
                            if (result.success) {
                                message = "Profil enregistré avec succès."
                            } else {
                                error = result.message.ifBlank {
                                    "Impossible d'enregistrer le profil."
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Erreur d'enregistrement."
                        }
                        saving = false
                    }
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 22.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProfileBlue
                ),
                shape = RoundedCornerShape(13.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    hint: String = "",
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color.White)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                if (hint.isNotBlank()) {
                    Text(hint, color = ProfileMuted)
                }
            },
            colors = profileFieldColors()
        )
    }
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ProfileBlue,
    unfocusedBorderColor = Color(0xFF303640),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = ProfileCard,
    unfocusedContainerColor = ProfileCard,
    focusedLabelColor = ProfileBlue,
    unfocusedLabelColor = ProfileMuted,
    cursorColor = ProfileBlue
)
