package com.pal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pal.app.ui.PalHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PalTheme { PalHomeScreen() }
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
    MaterialTheme(colorScheme = colors, content = content)
}
