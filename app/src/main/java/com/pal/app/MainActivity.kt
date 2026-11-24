package com.pal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pal.app.R
import com.pal.app.data.AuthRepository
import com.pal.app.network.SessionManager
import com.pal.app.ui.LoginScreen
import com.pal.app.ui.PalHomeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PalTheme {
                PalApp()
            }
        }
    }
}

@Composable
private fun PalApp() {
    var checkingSession by remember { mutableStateOf(true) }
    var authenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (SessionManager.hasSession()) {
            try {
                val result = AuthRepository.currentUser()
                authenticated = result.success && result.user != null

                if (!authenticated) {
                    SessionManager.clear()
                }
            } catch (_: Exception) {
                authenticated = false
                SessionManager.clear()
            }
        }

        checkingSession = false
    }

    when {
        checkingSession -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pal_logo),
                        contentDescription = "Logo Pal",
                        modifier = Modifier.size(190.dp)
                    )
                    CircularProgressIndicator(color = Color(0xFF087BFF))
                }
            }
        }

        authenticated -> {
            PalHomeScreen()
        }

        else -> {
            LoginScreen(
                onAuthenticated = {
                    authenticated = true
                }
            )
        }
    }
}

@Composable
private fun PalTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Color(0xFF087BFF),
        background = Color(0xFF000000),
        surface = Color(0xFF101319),
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
