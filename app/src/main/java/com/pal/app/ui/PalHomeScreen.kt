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

private val PalBlue = Color(0xFF087BFF)
private val PalBlack = Color(0xFF000000)
private val Card = Color(0xFF0D1015)
private val Muted = Color(0xFF8C929D)


@Composable
fun PalHomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var liked by remember { mutableStateOf(false) }
    Surface(color = PalBlack, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar()
            when (selectedTab) {
                0 -> FeedContent(liked)
                1 -> SimplePlaceholder("Message", "Tes conversations apparaîtront ici.", Icons.Default.ChatBubbleOutline)
                2 -> SimplePlaceholder("Notifications", "Tu retrouveras ici les nouveautés de ton réseau.", Icons.Default.NotificationsNone)
                else -> SimplePlaceholder("Recherche", "Recherche des personnes sur Pal.", Icons.Default.Search)
            }
            BottomBar(selectedTab) { selectedTab = it }
        }
    }
}


@Composable
private fun TopBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PalMark(Modifier.size(42.dp))
        Spacer(Modifier.width(10.dp))
        Text("Pal", color = PalBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
        Text(" Messenger", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Menu, contentDescription = "Paramètres", tint = Color.White, modifier = Modifier.size(30.dp).clickable { })
    }
}


@Composable
private fun FeedContent(liked: Boolean) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { StoriesRow() }
        item { ComposerCard() }
        item { PostCard("Firmin", "@firmin", "Il y a 15 min", "Aujourd'hui est une nouvelle occasion de faire mieux. ✨", 24, 8, true) }
        item { PostCard("Sarah", "@sarah_m", "Il y a 1 h", "La vie est plus belle quand on croit en soi. 💙", 37, 12, false) }
        item { PostCard("Kevin", "@kevin_01", "Il y a 2 h", "Un petit moment de calme.", 16, 4, false) }
    }
}


@Composable
private fun StoriesRow() {
    val storyNames = listOf("Ajouter", "Sarah", "Kevin", "Maria", "Patrick", "Jean")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 15.dp, end = 10.dp, top = 4.dp)) {
        storyNames.forEachIndexed { index, name ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp).padding(end = 7.dp)) {
                Box(Modifier.size(67.dp), contentAlignment = Alignment.Center) {
                    if (index == 0) {
                        Box(Modifier.size(65.dp).border(2.dp, Color(0xFF222832), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter une story", tint = PalBlue, modifier = Modifier.size(30.dp))
                        }
                    } else {
                        Box(Modifier.size(66.dp).border(3.dp, PalBlue, CircleShape).padding(4.dp).clip(CircleShape)) {
                            Avatar(name, index, Modifier.fillMaxSize())
                        }
                    }
                    if (index == 0) Box(Modifier.align(Alignment.BottomEnd).size(23.dp).clip(CircleShape).background(PalBlue), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
                Text(if (index == 0) "Ajouter une story" else name, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}


@Composable
private fun ComposerCard() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(25.dp)).background(Card).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar("Firmin", 0, Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).clip(RoundedCornerShape(30.dp)).background(Color(0xFF10141B)).border(1.dp, Color(0xFF1E2631), RoundedCornerShape(30.dp)).clickable { }.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("Ta pensée du moment ?", color = Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            ComposerAction(Icons.Default.Image, "Photo")
            VerticalDivider(Modifier.height(25.dp), color = Color(0xFF28303B))
            ComposerAction(Icons.Default.VideoCall, "Vidéo")
            VerticalDivider(Modifier.height(25.dp), color = Color(0xFF28303B))
            ComposerAction(Icons.Default.Edit, "Texte")
            Button(onClick = {}, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = PalBlue)) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Publier", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun ComposerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }.padding(horizontal = 2.dp)) {
        Icon(icon, contentDescription = label, tint = PalBlue, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}


@Composable
private fun PostCard(name: String, handle: String, time: String, body: String, likes: Int, comments: Int, cityImage: Boolean) {
    var isLiked by remember { mutableStateOf(name == "Firmin") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(26.dp)).background(Card).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, name.hashCode().let { kotlin.math.abs(it % 5) }, Modifier.size(58.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (name == "Firmin" || name == "Sarah") {
                        Spacer(Modifier.width(5.dp)); VerifiedDot()
                    }
                }
                Text("$handle · $time", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.Menu, contentDescription = "Options", tint = Muted, modifier = Modifier.clickable { }.size(23.dp))
        }
        Text(body, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 68.dp, top = 12.dp, bottom = 12.dp))
        if (cityImage) CityImage(Modifier.fillMaxWidth().height(220.dp)) else BlueRoseStyle(Modifier.fillMaxWidth().height(140.dp))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isLiked = !isLiked }) {
                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "J'aime", tint = if (isLiked) PalBlue else Muted, modifier = Modifier.size(28.dp))
                Text("${likes + (if (isLiked && name != "Firmin") 1 else 0)}", color = Color.White, modifier = Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.width(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Commenter", tint = Muted, modifier = Modifier.size(26.dp))
                Text("$comments", color = Color.White, modifier = Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                Icon(Icons.Default.Send, contentDescription = "Partager", tint = Muted, modifier = Modifier.size(24.dp))
                Text("Partager", color = Color.White, modifier = Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.BookmarkBorder, contentDescription = "Enregistrer", tint = Muted, modifier = Modifier.size(25.dp).clickable { })
        }
    }
}


@Composable
private fun Avatar(label: String, seed: Int, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFF174F9C), Color(0xFF164A69), Color(0xFF32465B), Color(0xFF36518A), Color(0xFF343B59))
    Box(modifier.clip(CircleShape).background(Brush.linearGradient(listOf(colors[kotlin.math.abs(seed) % colors.size], Color(0xFF11151C)))), contentAlignment = Alignment.Center) {
        Text(label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}


@Composable
private fun VerifiedDot() {
    Box(Modifier.size(17.dp).clip(CircleShape).background(PalBlue), contentAlignment = Alignment.Center) {
        Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun CityImage(modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(20.dp))) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF053B8B), Color(0xFF020A24), Color(0xFF01030A))))
        drawCircle(Color(0xFF0B57C8).copy(alpha = .22f), radius = size.width * .35f, center = Offset(size.width * .76f, size.height * .18f))
        val buildings = listOf(.16f to .55f, .27f to .44f, .38f to .65f, .48f to .35f, .59f to .50f, .70f to .39f, .80f to .62f, .90f to .43f)
        buildings.forEachIndexed { i, (x, h) ->
            val w = size.width * .105f
            val bh = size.height * h
            drawRect(Color(0xFF030918), topLeft = Offset(size.width*x-w/2, size.height-bh), size = Size(w,bh))
            for (row in 0..5) for (col in 0..2) {
                val wx = size.width*x-w/2 + w*.18f + col*w*.25f
                val wy = size.height-bh + row*bh*.13f + 6f
                if (wy < size.height-bh+bh*.75f) drawRect(Color(0xFF1E80FF).copy(alpha=.35f), Offset(wx,wy), Size(w*.09f, bh*.035f))
            }
        }
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF01040C)), startY=size.height*.75f, endY=size.height), topLeft=Offset(0f,size.height*.7f), size=Size(size.width,size.height*.3f))
    }
}


@Composable
private fun BlueRoseStyle(modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(20.dp))) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF010816), Color(0xFF043B91), Color(0xFF01050E))))
        val center = Offset(size.width*.5f, size.height*.5f)
        for (i in 0..12) {
            val radius = size.minDimension * (.08f + i*.026f)
            drawOval(Color(0xFF1682FF).copy(alpha=.14f + i*.012f), topLeft=Offset(center.x-radius, center.y-radius*.8f), size=Size(radius*2, radius*1.6f), style=Stroke(width=2.5f))
        }
        drawCircle(Color(0xFF4AA4FF).copy(alpha=.3f), size.minDimension*.12f, center)
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
    val labels = listOf("Accueil", "Message", "Notifications", "Recherche")
    val icons = listOf(Icons.Default.Home, Icons.Default.ChatBubbleOutline, Icons.Default.NotificationsNone, Icons.Default.Search)
    Row(Modifier.fillMaxWidth().background(PalBlack).navigationBarsPadding().padding(top = 7.dp, bottom = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { onSelect(index) }.padding(vertical = 3.dp)) {
                Icon(icons[index], contentDescription = label, tint = if (active) PalBlue else Color.White, modifier = Modifier.size(27.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, color = if (active) PalBlue else Color.White, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(3.dp))
                Box(Modifier.width(42.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).background(if (active) PalBlue else PalBlack))
            }
        }
    }
}


@Composable
private fun SimplePlaceholder(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = PalBlue, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = Muted)
        }
    }
}
