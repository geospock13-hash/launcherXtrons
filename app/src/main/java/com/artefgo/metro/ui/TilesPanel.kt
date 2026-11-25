package com.artefgo.metro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.artefgo.metro.data.StartLayout
import com.artefgo.metro.data.TileCfg

/** Acciones de edición de un mosaico. */
class TileActions(
    val launch: (AppEntry) -> Unit,
    val togglePin: (String) -> Unit,
    val move: (String, Int) -> Unit,
    val cycleSize: (String) -> Unit,
    val cycleColor: (String) -> Unit,
    val rename: (String, String?) -> Unit,
    val hide: (String) -> Unit,
    val uninstall: (String) -> Unit,
    val info: (AppEntry) -> Unit
)

/** Panel izquierdo: mosaicos estilo Windows Phone. Mantén presionado para editar. */
@Composable
fun TilesPanel(
    apps: List<AppEntry>,
    layout: StartLayout,
    defaultTiles: List<TileCfg>,
    editMode: Boolean,
    onEditMode: (Boolean) -> Unit,
    actions: TileActions,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalLauncherTheme.current
    val wallpaper = LocalHasWallpaper.current
    var selected by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<AppEntry?>(null) }

    val visible = apps.filter { it.packageName !in layout.hidden }
    val byPkg = visible.associateBy { it.packageName }
    val tiles = (layout.tiles ?: defaultTiles).filter { byPkg.containsKey(it.pkg) }
    val pinnedPkgs = tiles.map { it.pkg }.toSet()
    val others = visible.filter { it.packageName !in pinnedPkgs }
    LaunchedEffect(editMode) { if (!editMode) selected = null }

    fun labelOf(a: AppEntry) = layout.labels[a.packageName] ?: a.label
    fun colorOf(a: AppEntry): Color {
        val c = layout.colors[a.packageName]?.let { Color(it) } ?: theme.tileColor(a.packageName)
        return if (wallpaper) c.copy(alpha = c.alpha * 0.85f) else c
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (editMode) "editar inicio" else "inicio", color = theme.text, fontSize = 30.sp,
                fontWeight = FontWeight.Light, modifier = Modifier.weight(1f)
            )
            if (editMode) {
                TextButton(onClick = { onEditMode(false) }) { Text("Listo", fontSize = 16.sp) }
            } else {
                IconButton(onClick = { onEditMode(true) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = theme.text)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Configuración", tint = theme.text)
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val gap = 6.dp
            val tile = (maxWidth - gap * 2) / 3
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    tiles,
                    key = { "p_" + it.pkg },
                    span = { GridItemSpan(if (it.size == 0) 1 else 2) }
                ) { cfg ->
                    val app = byPkg.getValue(cfg.pkg)
                    Tile(
                        app = app, label = labelOf(app), color = colorOf(app), size = cfg.size,
                        unit = tile, gap = gap, editMode = editMode, selected = selected == app.packageName,
                        onClick = { if (editMode) selected = app.packageName else actions.launch(app) },
                        onLongClick = { onEditMode(true); selected = app.packageName }
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
                        app = app, label = labelOf(app), color = colorOf(app), size = 0,
                        unit = tile, gap = gap, editMode = editMode, selected = selected == app.packageName,
                        onClick = { if (editMode) selected = app.packageName else actions.launch(app) },
                        onLongClick = { onEditMode(true); selected = app.packageName }
                    )
                }
            }
        }

        if (editMode) {
            val sel = selected?.let { s -> visible.firstOrNull { it.packageName == s } }
            EditToolbar(
                title = sel?.let { "“${labelOf(it)}”" } ?: "Toca un mosaico para editarlo",
                enabled = sel != null
            ) {
                if (sel != null) {
                    val pkg = sel.packageName
                    val pinned = pkg in pinnedPkgs
                    if (pinned) {
                        ToolButton(Icons.AutoMirrored.Filled.ArrowBack, "Antes") { actions.move(pkg, -1) }
                        ToolButton(Icons.AutoMirrored.Filled.ArrowForward, "Después") { actions.move(pkg, 1) }
                        ToolButton(Icons.Filled.AspectRatio, "Tamaño") { actions.cycleSize(pkg) }
                    }
                    ToolButton(Icons.Filled.PushPin, if (pinned) "Desanclar" else "Anclar") { actions.togglePin(pkg) }
                    ToolButton(Icons.Filled.Palette, "Color") { actions.cycleColor(pkg) }
                    ToolButton(Icons.Filled.Edit, "Nombre") { renaming = sel }
                    ToolButton(Icons.Filled.VisibilityOff, "Ocultar") { actions.hide(pkg); selected = null }
                    ToolButton(Icons.Filled.Info, "Info") { actions.info(sel) }
                    ToolButton(Icons.Filled.Delete, "Desinstalar") { actions.uninstall(pkg) }
                }
            }
        }
    }

    renaming?.let { app ->
        var text by remember(app.packageName) { mutableStateOf(labelOf(app)) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Cambiar nombre") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { actions.rename(app.packageName, text); renaming = null }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { actions.rename(app.packageName, null); renaming = null }) { Text("Nombre original") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(
    app: AppEntry,
    label: String,
    color: Color,
    size: Int,
    unit: Dp,
    gap: Dp,
    editMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val target = when {
        pressed -> 0.93f
        editMode && !selected -> 0.9f
        else -> 1f
    }
    val scale by animateFloatAsState(target, label = "tileScale")
    val height = if (size == 2) unit * 2 + gap else unit
    val iconSize = if (size == 2) unit * 0.6f else unit * 0.42f

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = if (pressed) 6f else 0f   // efecto "tilt" de Windows Phone
                alpha = if (editMode && !selected) 0.7f else 1f
            }
            .background(color)
            .then(if (selected) Modifier.border(3.dp, Color.White) else Modifier)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = label,
            modifier = Modifier
                .align(if (size == 1) Alignment.CenterStart else Alignment.Center)
                .padding(start = if (size == 1) 14.dp else 0.dp, bottom = 8.dp)
                .size(iconSize)
        )
        Text(
            label,
            color = Color.White,
            fontSize = if (size == 2) 14.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, end = 6.dp, bottom = 4.dp)
        )
    }
}
