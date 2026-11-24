package com.artefgo.metro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artefgo.metro.data.AppEntry
import com.artefgo.metro.data.AppsRepository

/** Panel izquierdo: mosaicos estilo Windows Phone con las apps instaladas. */
@Composable
fun TilesPanel(
    apps: List<AppEntry>,
    pinned: List<String>?,
    wide: Set<String>?,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onToggleWide: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinnedList = pinned ?: AppsRepository.defaultPinned(apps)
    val wideSet = wide ?: pinnedList.take(2).toSet()
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val pinnedApps = pinnedList.mapNotNull { byPkg[it] }
    val others = apps.filter { it.packageName !in pinnedList }

    val theme = LocalLauncherTheme.current
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "inicio", color = theme.text, fontSize = 30.sp, fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Configuración", tint = theme.text)
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val gap = 6.dp
            val tile = (maxWidth - gap * 2) / 3
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    pinnedApps,
                    key = { "p_" + it.component.flattenToString() },
                    span = { GridItemSpan(if (it.packageName in wideSet) 2 else 1) }
                ) { app ->
                    Tile(
                        app, wide = app.packageName in wideSet, pinned = true, height = tile,
                        onClick = { onLaunch(app) }, onTogglePin = { onTogglePin(app) },
                        onToggleWide = { onToggleWide(app) }, onInfo = { onInfo(app) }
                    )
                }
                if (others.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "todas las aplicaciones", color = theme.subText, fontSize = 16.sp,
                            fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                }
                items(others, key = { "o_" + it.component.flattenToString() }) { app ->
                    Tile(
                        app, wide = false, pinned = false, height = tile,
                        onClick = { onLaunch(app) }, onTogglePin = { onTogglePin(app) },
                        onToggleWide = {}, onInfo = { onInfo(app) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(
    app: AppEntry,
    wide: Boolean,
    pinned: Boolean,
    height: Dp,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleWide: () -> Unit,
    onInfo: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "tilePress")
    val theme = LocalLauncherTheme.current
    val color = theme.tileColor(app.packageName).let { if (LocalHasWallpaper.current) it.copy(alpha = it.alpha * 0.85f) else it }

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = (1f - scale) * 60f   // efecto "tilt" de Windows Phone
            }
            .background(color)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = { menu = true },
                onClick = onClick
            )
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.align(if (wide) Alignment.CenterStart else Alignment.Center)
                .padding(start = if (wide) 14.dp else 0.dp, bottom = 8.dp)
                .size(height * 0.42f)
        )
        Text(
            app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, end = 6.dp, bottom = 4.dp)
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (pinned) "Desanclar de inicio" else "Anclar a inicio") },
                onClick = { menu = false; onTogglePin() }
            )
            if (pinned) {
                DropdownMenuItem(
                    text = { Text(if (wide) "Tamaño normal" else "Tamaño ancho") },
                    onClick = { menu = false; onToggleWide() }
                )
            }
            DropdownMenuItem(text = { Text("Información de la app") }, onClick = { menu = false; onInfo() })
        }
    }
}
