package com.artefgo.metro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.artefgo.metro.data.WidgetCfg
import com.artefgo.metro.data.WidgetType
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artefgo.metro.data.GpsState
import com.artefgo.metro.data.MediaHub
import com.artefgo.metro.data.NowPlaying
import com.artefgo.metro.data.ObdState
import com.artefgo.metro.data.Weather
import com.artefgo.metro.data.WeatherRepository
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val ES = Locale.forLanguageTag("es-MX")
private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ES)

/** Acciones de edición de los widgets. */
class WidgetActions(
    val move: (Int, Int) -> Unit,
    val cycleSpan: (Int) -> Unit,
    val cycleColor: (Int) -> Unit,
    val remove: (Int) -> Unit,
    val add: (WidgetType) -> Unit
)

/** Panel derecho: widgets que se pueden mover, cambiar de tamaño/color, quitar y añadir. */
@Composable
fun InfoPanel(
    widgets: List<WidgetCfg>,
    editMode: Boolean,
    onEditMode: (Boolean) -> Unit,
    actions: WidgetActions,
    now: LocalDateTime,
    weather: Weather?,
    gps: GpsState,
    obd: ObdState,
    media: NowPlaying?,
    hasMediaAccess: Boolean,
    onGrantMedia: () -> Unit,
    onOpenMediaApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalLauncherTheme.current
    val wp = LocalHasWallpaper.current
    var selected by remember { mutableStateOf<Int?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    LaunchedEffect(editMode) { if (!editMode) selected = null }

    fun colorFor(cfg: WidgetCfg, key: String): Color =
        (cfg.color?.let { Color(it) } ?: theme.tileColor(key)).let { if (wp) it.copy(alpha = it.alpha * 0.85f) else it }

    val speed: Float? = obd.speedKmh?.toFloat() ?: gps.speedKmh

    Column(modifier) {
        if (editMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "editar widgets", color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Light,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onEditMode(false) }) { Text("Listo", fontSize = 16.sp) }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(
                widgets,
                key = { _, w -> w.type.name },
                span = { _, w -> GridItemSpan(w.span) }
            ) { i, w ->
                WidgetFrame(
                    editMode = editMode,
                    selected = selected == i,
                    onClick = {
                        if (editMode) selected = i
                        else if (w.type == WidgetType.MEDIA) onOpenMediaApp()
                    },
                    onLongClick = { onEditMode(true); selected = i }
                ) {
                    val bg = w.color?.let { Color(it) }
                    when (w.type) {
                        WidgetType.CLOCK -> Column(
                            Modifier.fillMaxWidth().then(if (bg != null) Modifier.background(bg).padding(8.dp) else Modifier)
                        ) {
                            val big = if (w.span == 2) 58.sp else 44.sp
                            Text(now.format(HOUR_FMT), color = theme.text, fontSize = big, fontWeight = FontWeight.Thin, lineHeight = big)
                            Text(now.format(DATE_FMT).lowercase(ES), color = theme.subText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        WidgetType.WEATHER -> Box(
                            Modifier.fillMaxWidth().then(if (bg != null) Modifier.background(bg).padding(8.dp) else Modifier)
                        ) {
                            Box(Modifier.align(if (w.span == 1) Alignment.TopEnd else Alignment.TopStart)) { WeatherBox(weather) }
                        }
                        WidgetType.SPEED -> GaugeTile(
                            "velocidad", speed?.roundToInt()?.toString() ?: "—", "km/h",
                            (speed ?: 0f) / 200f, colorFor(w, "velocidad"), Modifier.fillMaxWidth()
                        )
                        WidgetType.RPM -> GaugeTile(
                            "rpm", obd.rpm?.toString() ?: "—", "rev/min",
                            (obd.rpm ?: 0) / 7000f, colorFor(w, "rpm"), Modifier.fillMaxWidth()
                        )
                        WidgetType.ALTITUDE -> StatTile(
                            "altitud", gps.altitudeM?.roundToInt()?.toString() ?: "—", "m snm",
                            colorFor(w, "altitud"), Modifier.fillMaxWidth()
                        )
                        WidgetType.BATTERY -> {
                            val v = obd.voltage
                            val c = when {
                                w.color != null || v == null -> colorFor(w, "bateria")
                                v < 12.0f -> Metro.Red
                                v < 12.6f -> Metro.Amber
                                else -> Metro.Teal
                            }
                            StatTile("batería", v?.let { String.format(Locale.US, "%.1f", it) } ?: "—", "V", c, Modifier.fillMaxWidth())
                        }
                        WidgetType.COOLANT -> StatTile(
                            "motor", obd.coolantC?.toString() ?: "—", "°C", colorFor(w, "motor"), Modifier.fillMaxWidth()
                        )
                        WidgetType.OBD_STATUS -> Text(
                            obd.status, color = if (bg != null) Color.White else theme.subText, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().then(if (bg != null) Modifier.background(bg).padding(6.dp) else Modifier)
                        )
                        WidgetType.MEDIA -> NowPlayingCard(media, hasMediaAccess, onGrantMedia, onOpenMediaApp, bg)
                    }
                }
            }
        }

        if (editMode) {
            val sel = selected?.takeIf { it in widgets.indices }
            EditToolbar(
                title = sel?.let { "Widget: ${widgets[it].type.title}" } ?: "Toca un widget para editarlo",
                enabled = sel != null
            ) {
                if (sel != null) {
                    ToolButton(Icons.Filled.KeyboardArrowUp, "Subir") {
                        actions.move(sel, -1); selected = (sel - 1).coerceAtLeast(0)
                    }
                    ToolButton(Icons.Filled.KeyboardArrowDown, "Bajar") {
                        actions.move(sel, 1); selected = (sel + 1).coerceAtMost(widgets.size - 1)
                    }
                    ToolButton(Icons.Filled.SwapHoriz, "Ancho") { actions.cycleSpan(sel) }
                    ToolButton(Icons.Filled.Palette, "Color") { actions.cycleColor(sel) }
                    ToolButton(Icons.Filled.Delete, "Quitar") { actions.remove(sel); selected = null }
                }
                val missing = WidgetType.entries.filter { t -> widgets.none { it.type == t } }
                if (missing.isNotEmpty()) {
                    Box {
                        ToolButton(Icons.Filled.Add, "Añadir") { addMenu = true }
                        DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                            missing.forEach { t ->
                                DropdownMenuItem(text = { Text(t.title) }, onClick = { actions.add(t); addMenu = false })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetFrame(
    editMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (editMode && !selected) 0.7f else 1f }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        content()
        if (editMode) {
            // En modo edición una capa encima captura los toques (para no activar botones de música, etc.)
            Box(
                Modifier
                    .matchParentSize()
                    .then(if (selected) Modifier.border(3.dp, Color.White) else Modifier)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            )
        }
    }
}

@Composable
private fun WeatherBox(weather: Weather?) {
    val theme = LocalLauncherTheme.current
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 8.dp)) {
        if (weather == null) {
            Text("clima", color = theme.subText, fontSize = 14.sp)
            Text("buscando…", color = theme.subText, fontSize = 12.sp)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(weatherIcon(weather), contentDescription = null, tint = theme.text, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(6.dp))
            Text("${weather.tempC.roundToInt()}°", color = theme.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
        Text(WeatherRepository.describe(weather.code).lowercase(ES), color = theme.subText, fontSize = 12.sp)
        val extra = listOfNotNull(
            weather.humidity?.let { "$it% hum" },
            weather.windKmh?.let { "${it.roundToInt()} km/h" }
        ).joinToString(" · ")
        if (extra.isNotEmpty()) Text(extra, color = theme.subText, fontSize = 11.sp)
    }
}

private fun weatherIcon(w: Weather): ImageVector = when {
    w.code in 95..99 -> Icons.Filled.FlashOn
    w.code in 71..77 || w.code == 85 || w.code == 86 -> Icons.Filled.AcUnit
    WeatherRepository.isRain(w.code) -> Icons.Filled.Umbrella
    w.code == 3 || w.code == 45 || w.code == 48 -> Icons.Filled.Cloud
    w.code == 2 -> Icons.Filled.WbCloudy
    !w.isDay -> Icons.Filled.NightsStay
    else -> Icons.Filled.WbSunny
}

@Composable
private fun GaugeTile(
    title: String,
    value: String,
    unit: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), label = "gauge")
    Box(modifier.height(92.dp).background(color).padding(8.dp)) {
        Canvas(Modifier.size(58.dp).align(Alignment.TopEnd)) {
            val sw = 6.dp.toPx()
            val stroke = Stroke(sw, cap = StrokeCap.Round)
            val inset = sw / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
            val tl = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.25f), 135f, 270f, false, topLeft = tl, size = arcSize, style = stroke)
            drawArc(Color.White, 135f, 270f * p, false, topLeft = tl, size = arcSize, style = stroke)
        }
        Text(title, color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.TopStart))
        Row(Modifier.align(Alignment.BottomStart), verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
            Spacer(Modifier.width(4.dp))
            Text(unit, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.height(66.dp).background(color).padding(8.dp)) {
        Text(title, color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.TopStart))
        Row(Modifier.align(Alignment.BottomStart), verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light, lineHeight = 28.sp)
            Spacer(Modifier.width(4.dp))
            Text(unit, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun NowPlayingCard(
    media: NowPlaying?,
    hasAccess: Boolean,
    onGrant: () -> Unit,
    onOpen: () -> Unit,
    bg: Color? = null
) {
    val theme = LocalLauncherTheme.current
    Column(Modifier.fillMaxWidth().background(bg ?: theme.panel).padding(8.dp)) {
        if (!hasAccess) {
            Text("reproduciendo", color = Metro.Cyan, fontSize = 12.sp)
            Text(
                "Para ver la canción de Spotify, YouTube Music, Poweramp, Tidal, Deezer, etc. permite el acceso a notificaciones.",
                color = theme.text, fontSize = 12.sp
            )
            TextButton(onClick = onGrant) { Text("Permitir acceso") }
            return@Column
        }
        if (media == null || (media.title.isBlank() && !media.isPlaying)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = theme.subText)
                Spacer(Modifier.width(8.dp))
                Text("sin música en reproducción", color = theme.subText, fontSize = 14.sp)
            }
            return@Column
        }
        val accent = theme.tileColor(media.packageName).copy(alpha = 1f)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpen)) {
            Box(Modifier.size(56.dp).background(accent), contentAlignment = Alignment.Center) {
                val art = media.art
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = theme.text)
                }
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(media.appName.lowercase(ES), color = accent, fontSize = 12.sp, maxLines = 1)
                Text(
                    media.title.ifBlank { media.appName }, color = theme.text, fontSize = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (media.artist.isNotBlank()) {
                    Text(media.artist, color = theme.subText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (media.durationMs > 0) {
            val pos by produceState(media.currentPosition(), media) {
                while (true) {
                    value = media.currentPosition()
                    delay(500)
                }
            }
            LinearProgressIndicator(
                progress = { (pos.toFloat() / media.durationMs).coerceIn(0f, 1f) },
                color = accent,
                trackColor = theme.subText.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { MediaHub.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", tint = theme.text)
            }
            IconButton(onClick = { MediaHub.playPause() }) {
                Icon(
                    if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir/Pausa", tint = theme.text, modifier = Modifier.size(34.dp)
                )
            }
            IconButton(onClick = { MediaHub.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente", tint = theme.text)
            }
        }
    }
}
