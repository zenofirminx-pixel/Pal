package com.pal.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pal.app.data.SecurityRepository
import kotlinx.coroutines.launch

private val SecurityBlue = Color(0xFF087BFF)
private val SecurityBlack = Color(0xFF000000)
private val SecurityCard = Color(0xFF0D1015)
private val SecurityMuted = Color(0xFF8C929D)

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var enabled by remember { mutableStateOf<Boolean?>(null) }
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    var qrCode by remember { mutableStateOf("") }
    var manualKey by remember { mutableStateOf("") }
    var recoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }

    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        try {
            val result = SecurityRepository.getStatus()
            if (result.success) {
                enabled = result.twoFactorEnabled
            } else {
                error = result.message.ifBlank {
                    "Impossible de vérifier la sécurité du compte."
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Erreur de connexion."
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecurityBlack)
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
                "Authentification à deux facteurs",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (loading) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SecurityBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (enabled == true) {
                    Text(
                        "Protection activée",
                        color = SecurityBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Ton compte utilise actuellement une authentification à deux facteurs.",
                        color = Color.White
                    )

                    SecurityField(
                        label = "Mot de passe actuel",
                        value = password,
                        onValueChange = { password = it },
                        password = true
                    )

                    SecurityField(
                        label = "Code de l'application ou code de récupération",
                        value = code,
                        onValueChange = { code = it }
                    )

                    Button(
                        onClick = {
                            if (password.isBlank() || code.isBlank()) {
                                error = "Saisis ton mot de passe et ton code."
                                return@Button
                            }

                            scope.launch {
                                processing = true
                                error = ""
                                message = ""
                                try {
                                    val result = SecurityRepository.disable(
                                        password,
                                        code
                                    )
                                    if (result.success) {
                                        enabled = false
                                        password = ""
                                        code = ""
                                        message = "La double authentification a été désactivée."
                                    } else {
                                        error = result.message
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Erreur de désactivation."
                                }
                                processing = false
                            }
                        },
                        enabled = !processing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecurityBlue
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("Désactiver la 2FA")
                    }
                } else if (!qrCode.isNullOrBlank()) {
                    Text(
                        "Configurer ton application",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Scanne ce QR code avec ton application d'authentification.",
                        color = SecurityMuted
                    )

                    val qrBitmap = remember(qrCode) {
                        try {
                            val encoded = qrCode.substringAfter(
                                ",",
                                qrCode
                            )
                            val bytes = Base64.decode(
                                encoded,
                                Base64.DEFAULT
                            )
                            BitmapFactory.decodeByteArray(
                                bytes,
                                0,
                                bytes.size
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR code de configuration 2FA",
                            modifier = Modifier
                                .size(220.dp)
                                .align(Alignment.CenterHorizontally)
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    } else {
                        Text(
                            "Le QR code n'a pas pu être affiché. Utilise la clé manuelle.",
                            color = Color(0xFFFF6B6B)
                        )
                    }

                    Text(
                        "Clé de configuration manuelle",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = manualKey,
                        color = SecurityBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(
                                    android.content.ClipData.newPlainText("Pal 2FA", manualKey)
                                )
                                Toast.makeText(context, "Clé copiée", Toast.LENGTH_SHORT).show()
                            }
                    )

                    Text(
                        "Saisis le code à six chiffres généré par ton application.",
                        color = SecurityMuted
                    )

                    SecurityField(
                        label = "Code à six chiffres",
                        value = code,
                        onValueChange = {
                            code = it.filter(Char::isDigit).take(6)
                        }
                    )

                    Button(
                        onClick = {
                            if (!code.matches(Regex("\\d{6}"))) {
                                error = "Le code doit contenir six chiffres."
                                return@Button
                            }

                            scope.launch {
                                processing = true
                                error = ""
                                try {
                                    val result = SecurityRepository.confirm(code)
                                    if (result.success) {
                                        enabled = true
                                        qrCode = ""
                                        manualKey = ""
                                        code = ""
                                        recoveryCodes = result.recoveryCodes
                                        message = result.message
                                            .ifBlank { "La 2FA est activée." }
                                    } else {
                                        error = result.message
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Code de vérification incorrect."
                                }
                                processing = false
                            }
                        },
                        enabled = !processing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecurityBlue
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("Confirmer l'activation")
                    }
                } else if (enabled == false) {
                    Text(
                        "Protection supplémentaire",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Active la double authentification pour ajouter une vérification lors de la connexion.",
                        color = SecurityMuted
                    )

                    SecurityField(
                        label = "Mot de passe actuel",
                        value = password,
                        onValueChange = { password = it },
                        password = true
                    )

                    Button(
                        onClick = {
                            if (password.isBlank()) {
                                error = "Saisis ton mot de passe actuel."
                                return@Button
                            }

                            scope.launch {
                                processing = true
                                error = ""
                                message = ""
                                try {
                                    val result = SecurityRepository.setup(password)
                                    if (result.success) {
                                        qrCode = result.qrCode.orEmpty()
                                        manualKey = result.manualEntryKey.orEmpty()
                                        password = ""

                                        if (qrCode.isBlank() || manualKey.isBlank()) {
                                            error = "Le serveur n'a pas fourni les informations de configuration."
                                            qrCode = ""
                                        }
                                    } else {
                                        error = result.message
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Impossible de préparer la 2FA."
                                }
                                processing = false
                            }
                        },
                        enabled = !processing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecurityBlue
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("Activer la 2FA")
                    }
                }

                if (recoveryCodes.isNotEmpty()) {
                    HorizontalDivider(color = SecurityMuted)

                    Text(
                        "Codes de récupération",
                        color = SecurityBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Conserve ces codes dans un endroit sûr. Chacun ne peut être utilisé qu'une seule fois. Ils ne seront plus affichés après avoir quitté cet écran.",
                        color = Color.White
                    )

                    recoveryCodes.forEach { recoveryCode ->
                        Text(
                            recoveryCode,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SecurityCard,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        )
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF6B6B))
                }

                if (message.isNotBlank()) {
                    Text(message, color = SecurityBlue)
                }

                if (processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = SecurityBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SecurityBlue,
            unfocusedBorderColor = Color(0xFF303640),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = SecurityCard,
            unfocusedContainerColor = SecurityCard,
            focusedLabelColor = SecurityBlue,
            unfocusedLabelColor = SecurityMuted,
            cursorColor = SecurityBlue
        )
    )
}
