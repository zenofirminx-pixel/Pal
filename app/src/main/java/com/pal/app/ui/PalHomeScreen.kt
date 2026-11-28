package com.pal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private val PalBlue = Color(0xFF087BFF)
private val PalBlack = Color(0xFF000000)
private val Card = Color(0xFF0D1015)
private val Muted = Color(0xFF8C929D)


@Composable
fun PalHomeScreen(onLogout: () -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var liked by remember { mutableStateOf(false) }
    var previousTab by remember { mutableIntStateOf(0) }
    var settingsPage by remember { mutableStateOf<String?>(null) }
    var selectedPublicUserUid by remember { mutableStateOf<String?>(null) }
    var pendingChatUser by remember { mutableStateOf<JSONObject?>(null) }

    Surface(color = PalBlack, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            if (settingsPage == null && selectedPublicUserUid == null) {
                TopBar(onMenuClick = { settingsPage = "main" })
            }

            if (settingsPage != null) {
                when (settingsPage) {
                    "main" -> SettingsScreen(
                        onBack = { settingsPage = null },
                        onEditPhoto = { settingsPage = "photo" },
                        onEditProfile = { settingsPage = "profile" },
                        onSecurity = { settingsPage = "security" },
                        onLogout = onLogout
                    )

                    "profile" -> ProfileEditScreen(
                        onBack = { settingsPage = "main" }
                    )
                    "photo" -> AvatarEditScreen(
                        onBack = { settingsPage = "main" }
                    )
                    "security" -> SecurityScreen(
                        onBack = { settingsPage = "main" }
                    )
                    else -> SettingsPlaceholderScreen(
                        title = when (settingsPage) {
                            "photo" -> "Photo de profil"
                            "security" -> "Authentification à deux facteurs"
                            else -> "Paramètres"
                        },
                        onBack = { settingsPage = "main" }
                    )
                }
            } else {
                if (selectedPublicUserUid != null) {
                    PublicProfileScreen(
                        uid = selectedPublicUserUid!!,
                        onBack = { selectedPublicUserUid = null },
                        onOpenChat = { user ->
                            pendingChatUser = user
                            selectedPublicUserUid = null
                            selectedTab = 1
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            0 -> PalRealFeedContent()

                            1 -> MessagesScreen(
                                onOpenPalAi = {
                                    previousTab = 1
                                    selectedTab = 4
                                },
                                initialUser = pendingChatUser,
                                onInitialUserOpened = {
                                    pendingChatUser = null
                                }
                            )

                            2 -> NotificationsScreen(
                                onOpenProfile = { uid ->
                                    selectedPublicUserUid = uid
                                }
                            )

                            3 -> SearchScreen(
                                onOpenProfile = { uid ->
                                    selectedPublicUserUid = uid
                                },
                                onOpenChat = { user ->
                                    pendingChatUser = user
                                    selectedTab = 1
                                }
                            )

                            4 -> PalAiScreen(
                                onBack = { selectedTab = previousTab }
                            )
                        }
                    }

                    BottomBar(selectedTab) { tab ->
                        if (tab == 4) {
                            previousTab = selectedTab
                        }
                        selectedTab = tab
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PalBlack)
            .padding(20.dp)
    ) {
        Text(
            "‹  Retour",
            color = PalBlue,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 12.dp)
        )

        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TopBar(onMenuClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PalMark(Modifier.size(42.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Pal",
            color = PalBlue,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            " Messenger",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Default.Menu,
            contentDescription = "Paramètres",
            tint = Color.White,
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onMenuClick)
        )
    }
}

@Composable
private fun PalMark(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        // A simple native vector approximation of PAL's blue ribbon P mark.
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w*.13f,h*.08f); lineTo(w*.62f,h*.08f)
            cubicTo(w*.88f,h*.10f,w*.96f,h*.27f,w*.88f,h*.43f)
            cubicTo(w*.82f,h*.54f,w*.69f,h*.58f,w*.47f,h*.58f)
            lineTo(w*.28f,h*.72f); lineTo(w*.28f,h*.96f); lineTo(w*.08f,h*.96f)
            lineTo(w*.08f,h*.62f); cubicTo(w*.08f,h*.52f,w*.18f,h*.45f,w*.34f,h*.45f)
            lineTo(w*.68f,h*.45f); cubicTo(w*.77f,h*.45f,w*.78f,h*.39f,w*.77f,h*.31f)
            cubicTo(w*.76f,h*.23f,w*.70f,h*.22f,w*.61f,h*.22f)
            lineTo(w*.13f,h*.22f); close()
        }
        drawPath(path, Brush.linearGradient(listOf(Color(0xFF17B5FF), Color(0xFF0757FF), Color(0xFF0034A8))))
    }
}


@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple(0, "Accueil", Icons.Default.Home),
        Triple(1, "Messages", Icons.Default.ChatBubbleOutline),
        Triple(4, "Pal AI", Icons.Default.AutoAwesome),
        Triple(2, "Notifications", Icons.Default.NotificationsNone),
        Triple(3, "Recherche", Icons.Default.Search)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PalBlack)
    ) {
        HorizontalDivider(color = Color(0xFF20242B), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (index, label, icon) ->
                val active = selected == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (active) PalBlue else Color.White,
                        modifier = Modifier.size(25.dp)
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        label,
                        color = if (active) PalBlue else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
