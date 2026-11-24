package com.pal.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pal.app.data.AuthRepository
import kotlinx.coroutines.launch

private val PalBlue = Color(0xFF087BFF)

@Composable
fun LoginScreen(onAuthenticated: () -> Unit) {
    var registering by remember { mutableStateOf(false) }
    var twoFactor by remember { mutableStateOf(false) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("prefer_not_say") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "P",
            color = PalBlue,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (twoFactor) "Vérification de sécurité"
                   else if (registering) "Rejoindre Pal"
                   else "Connexion au réseau Pal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(24.dp))

        if (registering) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Date de naissance") },
                placeholder = { Text("AAAA-MM-JJ") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("Genre", color = Color.White)
            Column(Modifier.fillMaxWidth()) {
                listOf(
                    "female" to "Femme",
                    "male" to "Homme",
                    "other" to "Autre",
                    "prefer_not_say" to "Préfère ne pas répondre"
                ).forEach { (value, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = gender == value,
                            onClick = { gender = value },
                            colors = RadioButtonDefaults.colors(selectedColor = PalBlue)
                        )
                        Text(label, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (twoFactor) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code de vérification") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Adresse e-mail") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color(0xFFFF8A80))
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (loading) return@Button

                message = ""
                loading = true

                scope.launch {
                    try {
                        val result = when {
                            twoFactor ->
                                AuthRepository.verifyTwoFactor(code)

                            registering ->
                                AuthRepository.register(
                                    firstName = firstName,
                                    lastName = lastName,
                                    birthDate = birthDate,
                                    gender = gender,
                                    email = email,
                                    password = password
                                )

                            else ->
                                AuthRepository.login(email, password)
                        }

                        when {
                            !result.success -> {
                                message = result.message.ifBlank {
                                    "Une erreur est survenue (HTTP ${result.httpStatus})."
                                }
                            }

                            twoFactor -> {
                                twoFactor = false
                                onAuthenticated()
                            }

                            registering -> {
                                registering = false
                                message = "Compte créé. Vérifie ton adresse e-mail avant de te connecter."
                                password = ""
                            }

                            result.twoFactorRequired -> {
                                twoFactor = true
                                message = "Saisis ton code de vérification."
                            }

                            else -> onAuthenticated()
                        }
                    } catch (exception: Exception) {
                        message = exception.message ?: "Impossible de contacter le serveur."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PalBlue)
        ) {
            Text(
                when {
                    loading -> "Connexion..."
                    twoFactor -> "Vérifier"
                    registering -> "Créer mon compte"
                    else -> "Se connecter"
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = {
                registering = !registering
                twoFactor = false
                message = ""
            },
            enabled = !loading && !twoFactor
        ) {
            Text(
                if (registering) "Déjà inscrit ? Se connecter"
                else "Sign in for a new account",
                color = PalBlue
            )
        }

        if (twoFactor) {
            TextButton(onClick = {
                twoFactor = false
                message = ""
            }) {
                Text("Retour", color = PalBlue)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Learn more for login",
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
