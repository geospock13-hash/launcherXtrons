package com.artefgo.metro.scene

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artefgo.metro.data.CarConfig
import com.artefgo.metro.data.WeatherRepository
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Visor central: carro avanzando por la carretera; el cielo sigue la hora real. */
@Composable
fun CarScene(
    car: CarConfig,
    hour: Float,
    speedKmh: Float?,
    weatherCode: Int?,
    modifier: Modifier = Modifier
) {
    var distance by remember { mutableDoubleStateOf(0.0) }
    var time by remember { mutableDoubleStateOf(0.0) }
    val speed by rememberUpdatedState(speedKmh)

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9).coerceIn(0.0, 0.1)
                    time += dt
                    val kmh = (speed ?: 0f).coerceIn(0f, 220f).toDouble()
                    distance += dt * (140.0 + kmh * 5.0)
                }
                last = now
            }
        }
    }

    val cloudiness = weatherCode?.let { WeatherRepository.cloudiness(it) } ?: 0.3f
    val raining = weatherCode?.let { WeatherRepository.isRain(it) } ?: false
    val shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1f, 2f), 6f)

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawScene(car, hour, distance, time, cloudiness, raining)
        }
        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
            Text(
                car.brand.lowercase(),
                style = TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, shadow = shadow)
            )
            Text(
                car.body.label,
                style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, shadow = shadow)
            )
        }
        Text(
            "toca para personalizar",
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, shadow = shadow)
        )
    }
}

// ---------- Cielo según la hora ----------

private class SkyKey(val h: Float, val top: Long, val bottom: Long, val day: Float)

private val SKY = listOf(
    SkyKey(0f, 0xFF070B1E, 0xFF18203F, 0f),
    SkyKey(5.2f, 0xFF070B1E, 0xFF18203F, 0f),
    SkyKey(6.2f, 0xFF2C3E77, 0xFFF4A06B, 0.45f),
    SkyKey(7.5f, 0xFF3F88D8, 0xFFBFE3F7, 0.9f),
    SkyKey(12f, 0xFF2477E0, 0xFFA9D6F5, 1f),
    SkyKey(15f, 0xFF3577C9, 0xFFCFE2EE, 0.95f),
    SkyKey(16.6f, 0xFF4065A6, 0xFFF2C487, 0.75f),   // tarde: más cálido y oscuro
    SkyKey(17.5f, 0xFF35386E, 0xFFF0804F, 0.5f),    // atardecer
    SkyKey(18f, 0xFF1C2050, 0xFF6B3F6E, 0.25f),     // 18:00 empieza la noche
    SkyKey(19f, 0xFF0A0F2A, 0xFF1D2547, 0.05f),
    SkyKey(24f, 0xFF070B1E, 0xFF18203F, 0f)
)

class SkyState(val top: Color, val bottom: Color, val day: Float)

fun skyAt(hour: Float): SkyState {
    val h = ((hour % 24f) + 24f) % 24f
    for (i in 0 until SKY.size - 1) {
        val a = SKY[i]; val b = SKY[i + 1]
        if (h >= a.h && h <= b.h) {
            val t = if (b.h == a.h) 0f else (h - a.h) / (b.h - a.h)
            return SkyState(
                lerp(Color(a.top), Color(b.top), t),
                lerp(Color(a.bottom), Color(b.bottom), t),
                a.day + (b.day - a.day) * t
            )
        }
    }
    return SkyState(Color(SKY[0].top), Color(SKY[0].bottom), 0f)
}

// ---------- Utilidades ----------

private fun hash(n: Int): Float {
    val x = sin(n * 12.9898) * 43758.5453
    return (x - floor(x)).toFloat()
}

private fun shade(c: Color, day: Float): Color =
    lerp(Color(c.red * 0.16f, c.green * 0.18f, c.blue * 0.30f + 0.02f), c, day.coerceIn(0f, 1f))

private inline fun repeatAlong(offset: Double, spacing: Float, width: Float, block: (x: Float, idx: Long) -> Unit) {
    val first = floor(offset / spacing).toLong() - 1
    val last = first + (width / spacing).toLong() + 3
    for (idx in first..last) {
        block((idx.toDouble() * spacing - offset).toFloat(), idx)
    }
}

private fun DrawScope.drawCloud(x: Float, y: Float, r: Float, color: Color) {
    drawCircle(color, r, Offset(x, y))
    drawCircle(color, r * 1.15f, Offset(x + r * 0.95f, y - r * 0.4f))
    drawCircle(color, r * 0.9f, Offset(x + r * 1.95f, y))
    drawCircle(color, r * 0.95f, Offset(x + r * 0.95f, y + r * 0.2f))
}

private fun DrawScope.ridge(
    offset: Double, base: Float, amp: Float, seed: Int, bottom: Float, color: Color
) {
    val w = size.width
    val path = Path()
    path.moveTo(0f, bottom)
    var x = 0f
    while (x <= w + 8f) {
        val xs = x + offset
        val v = sin(xs * 0.006 + seed) * 0.5 + sin(xs * 0.017 + seed * 2.3) * 0.3 + sin(xs * 0.041 + seed * 0.7) * 0.2
        path.lineTo(x, base - (amp * (0.6 + 0.4 * v)).toFloat())
        x += 6f
    }
    path.lineTo(w + 8f, bottom)
    path.close()
    drawPath(path, color)
}

// ---------- Escena ----------

fun DrawScope.drawScene(
    car: CarConfig,
    hour: Float,
    distance: Double,
    time: Double,
    cloudiness: Float,
    raining: Boolean
) {
    val w = size.width
    val h = size.height
    val k = min(w, h) / 450f            // escala relativa
    val sky = skyAt(hour)
    val d = sky.day
    val horizon = h * 0.60f
    val roadTop = h * 0.70f
    val roadBottom = h * 0.88f
    val hh = ((hour % 24f) + 24f) % 24f

    // Cielo
    drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom), startY = 0f, endY = horizon), size = Size(w, horizon + 2f))

    // Estrellas
    val starA = ((0.6f - d) / 0.6f).coerceIn(0f, 1f) * (1f - cloudiness * 0.6f)
    if (starA > 0.01f) {
        for (i in 0 until 90) {
            val tw = 0.55f + 0.45f * sin(time * 1.7 + i).toFloat()
            drawCircle(
                Color.White.copy(alpha = (starA * tw * (0.4f + 0.6f * hash(i + 300))).coerceIn(0f, 1f)),
                radius = (0.6f + 1.4f * hash(i + 500)) * k,
                center = Offset(hash(i * 2 + 1) * w, hash(i * 2 + 7) * horizon * 0.85f)
            )
        }
    }

    // Sol o luna sobre un arco de horizonte a horizonte
    val arcH = horizon * 0.78f
    val bodyR = min(w, h) * 0.055f
    if (hh in 6f..18f) {
        val t = (hh - 6f) / 12f
        val lift = sin(PI * t).toFloat()
        val c = Offset(w * (0.12f + 0.76f * t), horizon - lift * arcH + bodyR * 0.5f)
        val sunColor = lerp(Color(0xFFFFF3B0), Color(0xFFFF8A3D), (1f - lift) * (1f - lift))
        drawCircle(
            Brush.radialGradient(listOf(sunColor.copy(alpha = 0.55f), Color.Transparent), center = c, radius = bodyR * 4f),
            radius = bodyR * 4f, center = c
        )
        drawCircle(sunColor, bodyR, c)
    } else {
        val t = (((hh - 18f) + 24f) % 24f) / 12f
        val lift = sin(PI * t).toFloat()
        val c = Offset(w * (0.12f + 0.76f * t), horizon - lift * arcH + bodyR * 0.5f)
        drawCircle(
            Brush.radialGradient(listOf(Color(0x55BFD4FF), Color.Transparent), center = c, radius = bodyR * 3.5f),
            radius = bodyR * 3.5f, center = c
        )
        drawCircle(Color(0xFFEDEFF5), bodyR * 0.9f, c)
        drawCircle(Color(0xFFCDD2DC), bodyR * 0.22f, c + Offset(-bodyR * 0.3f, -bodyR * 0.2f))
        drawCircle(Color(0xFFCDD2DC), bodyR * 0.15f, c + Offset(bodyR * 0.35f, bodyR * 0.25f))
        drawCircle(Color(0xFFD6DAE2), bodyR * 0.11f, c + Offset(bodyR * 0.05f, bodyR * 0.45f))
    }

    // Nubes
    val nClouds = 2 + (cloudiness * 5).toInt()
    val cloudBase = lerp(lerp(Color(0xFF2E3454), Color.White, d), Color(0xFF8E959E), cloudiness * 0.55f * d)
    val cloudColor = lerp(sky.bottom, cloudBase, 0.6f + 0.4f * cloudiness)
    val span = w + 260f * k
    for (i in 0 until nClouds) {
        val baseX = hash(i + 40) * span
        val raw = (baseX - distance * 0.04 - time * 6.0) % span
        val x = ((raw + span) % span).toFloat() - 130f * k
        val y = horizon * (0.12f + 0.45f * hash(i + 60))
        drawCloud(x, y, (0.7f + 0.8f * hash(i + 80)) * 26f * k, cloudColor)
    }

    // Pasto (fondo)
    drawRect(
        Brush.verticalGradient(
            listOf(shade(Color(0xFF6FAF5A), d), shade(Color(0xFF3F7D3A), d)),
            startY = horizon, endY = h
        ),
        topLeft = Offset(0f, horizon), size = Size(w, h - horizon)
    )

    // Montañas lejanas y colinas
    ridge(distance * 0.08, horizon + 2f, h * 0.17f, 1, horizon + 2f, lerp(shade(Color(0xFF7E9CC0), d), sky.bottom, 0.35f))
    ridge(distance * 0.22, horizon + h * 0.05f, h * 0.08f, 5, roadTop, shade(Color(0xFF4E8A4A), d))

    // Árboles
    val treeBase = roadTop - 2f * k
    repeatAlong(distance * 0.55, 120f * k, w) { x, idx ->
        val j = hash(idx.toInt() * 3 + 11)
        val tx = x + j * 60f * k
        val sz = (0.7f + 0.6f * hash(idx.toInt() * 5 + 3)) * k
        val trunk = shade(Color(0xFF6D4C41), d)
        drawRect(trunk, Offset(tx - 3f * sz, treeBase - 22f * sz), Size(6f * sz, 22f * sz))
        if (hash(idx.toInt() + 77) > 0.5f) {
            val leaf = shade(Color(0xFF2E7D32), d)
            drawCircle(leaf, 16f * sz, Offset(tx, treeBase - 34f * sz))
            drawCircle(leaf, 12f * sz, Offset(tx - 11f * sz, treeBase - 26f * sz))
            drawCircle(leaf, 12f * sz, Offset(tx + 11f * sz, treeBase - 27f * sz))
        } else {
            val pine = Path().apply {
                moveTo(tx, treeBase - 62f * sz)
                lineTo(tx + 15f * sz, treeBase - 16f * sz)
                lineTo(tx - 15f * sz, treeBase - 16f * sz)
                close()
            }
            drawPath(pine, shade(Color(0xFF1B5E20), d))
        }
    }

    // Postes de luz (se encienden de noche)
    val night = d < 0.45f
    repeatAlong(distance, 330f * k, w) { x, _ ->
        val pole = shade(Color(0xFF55595E), d)
        val topY = roadTop - 95f * k
        drawLine(pole, Offset(x, roadTop), Offset(x, topY), strokeWidth = 3f * k)
        drawLine(pole, Offset(x, topY), Offset(x + 18f * k, topY + 3f * k), strokeWidth = 3f * k, cap = StrokeCap.Round)
        val lamp = Offset(x + 18f * k, topY + 5f * k)
        if (night) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0x88FFD27A), Color.Transparent), center = lamp, radius = 34f * k),
                radius = 34f * k, center = lamp
            )
            val cone = Path().apply {
                moveTo(lamp.x - 3f * k, lamp.y)
                lineTo(lamp.x + 3f * k, lamp.y)
                lineTo(lamp.x + 40f * k, roadBottom)
                lineTo(lamp.x - 34f * k, roadBottom)
                close()
            }
            drawPath(cone, Brush.verticalGradient(listOf(Color(0x40FFD27A), Color.Transparent), startY = lamp.y, endY = roadBottom))
            drawCircle(Color(0xFFFFE9B0), 3.5f * k, lamp)
        } else {
            drawCircle(shade(Color(0xFFDDDDDD), d), 3f * k, lamp)
        }
    }

    // Carretera
    drawRect(shade(Color(0xFF9E9E9E), d), Offset(0f, roadTop - 3f * k), Size(w, 4f * k))
    drawRect(shade(Color(0xFF45474D), d), Offset(0f, roadTop), Size(w, roadBottom - roadTop))
    val line = shade(Color(0xFFEDEDED), d)
    drawRect(line, Offset(0f, roadTop + 5f * k), Size(w, 2.5f * k))
    drawRect(line, Offset(0f, roadBottom - 8f * k), Size(w, 2.5f * k))
    val laneY = roadTop + (roadBottom - roadTop) * 0.36f
    val dash = shade(Color(0xFFF2C94C), d)
    repeatAlong(distance, 90f * k, w) { x, _ ->
        drawRect(dash, Offset(x, laneY), Size(50f * k, 4f * k))
    }

    // Carro
    val len = min(w * 0.82f, h * 0.62f)
    val u = len / 100f
    val groundY = roadTop + (roadBottom - roadTop) * 0.74f
    val bounce = ((sin(time * 11.0) * 0.18 + sin(time * 3.3) * 0.15) * u).toFloat()
    val wheelR = (if (car.body.name.contains("SUV")) 10.5f else 9f) * u
    val angle = ((distance % (2 * PI * wheelR)) / wheelR).toFloat()
    drawCar(car.body, Color(car.color), (w - len) / 2f, groundY, len, angle, d, bounce, lightsOn = d < 0.55f)

    // Acotamiento con pasto en primer plano
    drawRect(shade(Color(0xFF356B31), d), Offset(0f, roadBottom), Size(w, h - roadBottom))
    val tuft = shade(Color(0xFF4C8C42), d)
    repeatAlong(distance * 1.3, 26f * k, w) { x, idx ->
        val tx = x + hash(idx.toInt() + 900) * 14f * k
        val ty = roadBottom + (8f + hash(idx.toInt() + 901) * (h - roadBottom - 14f * k) / k) * k
        drawLine(tuft, Offset(tx, ty), Offset(tx - 3f * k, ty - 8f * k), strokeWidth = 2f * k)
        drawLine(tuft, Offset(tx + 3f * k, ty), Offset(tx + 4f * k, ty - 9f * k), strokeWidth = 2f * k)
    }

    // Lluvia
    if (raining) {
        drawRect(Color.Black.copy(alpha = 0.15f), size = size)
        val drop = Color.White.copy(alpha = 0.35f)
        for (i in 0 until 140) {
            val x = ((hash(i + 1200) * w + time * 40.0) % w).toFloat()
            val y = ((hash(i + 1500) * h + time * (700.0 + 300.0 * hash(i + 1700))) % h).toFloat()
            drawLine(drop, Offset(x, y), Offset(x - 4f * k, y + 14f * k), strokeWidth = 1.3f * k)
        }
    }
}
