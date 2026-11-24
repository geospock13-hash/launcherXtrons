package com.artefgo.metro.ui

import androidx.compose.animation.core.animateFloatAsState
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

/** Panel derecho: hora, clima, velocímetro, RPM, altitud, batería y la canción actual. */
@Composable
fun InfoPanel(
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
    fun tile(key: String) = theme.tileColor(key).let { if (wp) it.copy(alpha = it.alpha * 0.85f) else it }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hora + clima
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    now.format(HOUR_FMT), color = theme.text, fontSize = 58.sp,
                    fontWeight = FontWeight.Thin, lineHeight = 60.sp
                )
                Text(now.format(DATE_FMT).lowercase(ES), color = theme.subText, fontSize = 14.sp)
            }
            WeatherBox(weather)
        }

        // Velocidad y RPM
        val speed: Float? = obd.speedKmh?.toFloat() ?: gps.speedKmh
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GaugeTile(
                title = "velocidad", value = speed?.roundToInt()?.toString() ?: "—", unit = "km/h",
                progress = (speed ?: 0f) / 200f, color = tile("velocidad"), modifier = Modifier.weight(1f)
            )
            GaugeTile(
                title = "rpm", value = obd.rpm?.toString() ?: "—", unit = "rev/min",
                progress = (obd.rpm ?: 0) / 7000f, color = tile("rpm"), modifier = Modifier.weight(1f)
            )
        }

        // Altitud y batería
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatTile(
                title = "altitud", value = gps.altitudeM?.roundToInt()?.toString() ?: "—", unit = "m snm",
                color = tile("altitud"), modifier = Modifier.weight(1f)
            )
            val v = obd.voltage
            StatTile(
                title = "batería",
                value = v?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                unit = "V",
                color = when {
                    v == null -> tile("bateria")
                    v < 12.0f -> Metro.Red
                    v < 12.6f -> Metro.Amber
                    else -> Metro.Teal
                },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            obd.status + (obd.coolantC?.let { " · motor $it °C" } ?: ""),
            color = theme.subText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.weight(1f))
        NowPlayingCard(media, hasMediaAccess, onGrantMedia, onOpenMediaApp)
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
    onOpen: () -> Unit
) {
    val theme = LocalLauncherTheme.current
    Column(Modifier.fillMaxWidth().background(theme.panel).padding(8.dp)) {
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
