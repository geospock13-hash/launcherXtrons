package com.artefgo.metro.scene

import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.artefgo.metro.data.BodyType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Siluetas laterales dibujadas a mano (el carro mira a la derecha).
 * Coordenadas en "unidades": x de 0 (defensa trasera) a 100 (defensa delantera),
 * y hacia arriba desde el piso.
 */
private class CarSpec(
    val wheelR: Float,
    val wheelX: FloatArray,
    val body: FloatArray,
    val windows: List<FloatArray>,
    val belt: Float,
    val doors: FloatArray,
    val head: FloatArray,   // x1, y1, x2, y2
    val tail: FloatArray,   // x1, y1, x2, y2
    val cladding: Boolean,
    val rails: FloatArray?, // x1, x2, y
    val height: Float
)

private val SPECS: Map<BodyType, CarSpec> = mapOf(
    BodyType.SEDAN to CarSpec(
        wheelR = 9f, wheelX = floatArrayOf(20f, 79f),
        body = floatArrayOf(2f, 8f, 1f, 19f, 4f, 24f, 23f, 26f, 35f, 38f, 58f, 39f, 72f, 27f, 96f, 23f, 100f, 17f, 99f, 8f),
        windows = listOf(
            floatArrayOf(26f, 26.8f, 35.5f, 36.6f, 46f, 37.2f, 46f, 26.8f),
            floatArrayOf(48f, 26.8f, 48f, 37.3f, 57.5f, 37.4f, 69.5f, 27.2f)
        ),
        belt = 25.5f, doors = floatArrayOf(25f, 47f, 70f),
        head = floatArrayOf(93f, 18.5f, 99f, 22f), tail = floatArrayOf(1.2f, 15f, 5f, 20f),
        cladding = false, rails = null, height = 39f
    ),
    BodyType.HATCHBACK to CarSpec(
        wheelR = 9f, wheelX = floatArrayOf(19f, 78f),
        body = floatArrayOf(2f, 8f, 1f, 22f, 3f, 33f, 10f, 38.5f, 55f, 39f, 70f, 27f, 96f, 23f, 100f, 17f, 99f, 8f),
        windows = listOf(
            floatArrayOf(6f, 26.5f, 10.5f, 36.5f, 30f, 37.2f, 30f, 26.5f),
            floatArrayOf(32f, 26.5f, 32f, 37.3f, 54f, 37.4f, 67.5f, 27.2f)
        ),
        belt = 25.5f, doors = floatArrayOf(31f, 68f),
        head = floatArrayOf(93f, 18.5f, 99f, 22f), tail = floatArrayOf(1f, 22f, 4f, 29f),
        cladding = false, rails = null, height = 39f
    ),
    BodyType.SUV to CarSpec(
        wheelR = 11f, wheelX = floatArrayOf(19f, 80f),
        body = floatArrayOf(1f, 10f, 0f, 28f, 1.5f, 46f, 7f, 50f, 64f, 50f, 76f, 34f, 97f, 31f, 100f, 24f, 99f, 10f),
        windows = listOf(
            floatArrayOf(4.5f, 34.5f, 5f, 46.5f, 25.5f, 47.5f, 25.5f, 34.5f),
            floatArrayOf(27.5f, 34.5f, 27.5f, 47.5f, 50f, 47.5f, 50f, 34.5f),
            floatArrayOf(52f, 34.5f, 52f, 47.5f, 62.5f, 47.5f, 73f, 34.5f)
        ),
        belt = 33f, doors = floatArrayOf(26.5f, 51f, 74f),
        head = floatArrayOf(94f, 25f, 99.5f, 29.5f), tail = floatArrayOf(0.5f, 30f, 3f, 40f),
        cladding = true, rails = floatArrayOf(10f, 58f, 51.8f), height = 52f
    ),
    BodyType.MINI_SUV to CarSpec(
        wheelR = 10f, wheelX = floatArrayOf(20f, 79f),
        body = floatArrayOf(1f, 9f, 0f, 26f, 3f, 41f, 10f, 45f, 58f, 45f, 71f, 31f, 96f, 28f, 100f, 21f, 99f, 9f),
        windows = listOf(
            floatArrayOf(6f, 31f, 9.5f, 41.5f, 32f, 42.5f, 32f, 31f),
            floatArrayOf(34f, 31f, 34f, 42.5f, 56.5f, 42.5f, 68.5f, 31.2f)
        ),
        belt = 29.5f, doors = floatArrayOf(33f, 69f),
        head = floatArrayOf(93f, 22.5f, 99f, 26.5f), tail = floatArrayOf(0.5f, 27f, 3f, 35f),
        cladding = true, rails = floatArrayOf(13f, 55f, 46.5f), height = 46.5f
    )
)

/**
 * Dibuja el carro.
 * @param left borde izquierdo en px, @param groundY línea del piso en px, @param length largo en px.
 * @param daylight 0 = noche, 1 = pleno día.
 */
fun DrawScope.drawCar(
    body: BodyType,
    paint: Color,
    left: Float,
    groundY: Float,
    length: Float,
    wheelAngle: Float,
    daylight: Float,
    bounce: Float,
    lightsOn: Boolean
) {
    val s = SPECS.getValue(body)
    val u = length / 100f
    val d = daylight.coerceIn(0f, 1f)
    val lit = 0.38f + 0.62f * d
    val base = lerp(Color.Black, paint, lit)

    fun x(v: Float) = left + v * u
    fun y(v: Float) = groundY - v * u - bounce
    fun wy(v: Float) = groundY - v * u

    drawIntoCanvas { cc ->
        val c = cc.nativeCanvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sombra
        p.color = Color.Black.copy(alpha = 0.38f).toArgb()
        c.drawOval(x(-3f), groundY - 2.2f * u, x(103f), groundY + 2.4f * u, p)

        // Carrocería
        val b = s.body
        val bodyPath = Path().apply {
            moveTo(x(b[0]), y(b[1]))
            var i = 2
            while (i < b.size) {
                lineTo(x(b[i]), y(b[i + 1])); i += 2
            }
            close()
        }
        p.pathEffect = CornerPathEffect(3.5f * u)
        p.shader = LinearGradient(
            0f, y(s.height), 0f, y(6f),
            intArrayOf(
                lerp(base, Color.White, 0.25f).toArgb(),
                base.toArgb(),
                lerp(base, Color.Black, 0.45f).toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        c.drawPath(bodyPath, p)
        p.shader = null

        // Molduras plásticas inferiores (SUV / mini SUV)
        if (s.cladding) {
            p.color = lerp(Color.Black, Color(0xFF2B2B2B), lit).toArgb()
            p.pathEffect = CornerPathEffect(1.5f * u)
            c.drawRect(x(1.5f), y(b[1] + 3.5f), x(98.5f), y(b[1]), p)
        }
        p.pathEffect = null

        // Líneas de puertas
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.35f * u
        p.color = Color.Black.copy(alpha = 0.35f).toArgb()
        for (dx in s.doors) c.drawLine(x(dx), y(s.belt), x(dx), y(b[1] + 1.5f), p)

        // Brillo de la línea de cintura
        p.strokeWidth = 0.6f * u
        p.color = Color.White.copy(alpha = 0.22f * lit).toArgb()
        c.drawLine(x(5f), y(s.belt - 1.6f), x(92f), y(s.belt - 1.6f), p)

        // Barras de techo
        s.rails?.let { r ->
            p.strokeWidth = 1.3f * u
            p.color = lerp(Color.Black, Color(0xFF3A3A3A), lit).toArgb()
            c.drawLine(x(r[0]), y(r[2]), x(r[1]), y(r[2]), p)
            p.strokeWidth = 0.9f * u
            c.drawLine(x(r[0] + 1f), y(r[2]), x(r[0] + 1f), y(r[2] - 1.9f), p)
            c.drawLine(x(r[1] - 1f), y(r[2]), x(r[1] - 1f), y(r[2] - 1.9f), p)
        }
        p.style = Paint.Style.FILL

        // Ventanas
        p.pathEffect = CornerPathEffect(1.4f * u)
        p.shader = LinearGradient(
            0f, y(s.height), 0f, y(s.belt),
            intArrayOf(
                lerp(Color(0xFF0B1016), Color(0xFF7E93A8), lit).toArgb(),
                lerp(Color(0xFF05080B), Color(0xFF1B2631), lit).toArgb()
            ),
            null, Shader.TileMode.CLAMP
        )
        for (w in s.windows) {
            val wp = Path().apply {
                moveTo(x(w[0]), y(w[1]))
                var i = 2
                while (i < w.size) {
                    lineTo(x(w[i]), y(w[i + 1])); i += 2
                }
                close()
            }
            c.drawPath(wp, p)
        }
        p.shader = null
        p.pathEffect = null

        // Manijas
        p.color = lerp(base, Color.Black, 0.35f).toArgb()
        for (k in 0 until s.doors.size - 1) {
            c.drawRoundRect(
                x(s.doors[k] + 2.5f), y(s.belt - 2.2f), x(s.doors[k] + 6.5f), y(s.belt - 3.4f),
                0.6f * u, 0.6f * u, p
            )
        }

        // Espejo
        val fw = s.windows.last()
        val mx = fw[fw.size - 2]
        p.color = lerp(base, Color.Black, 0.2f).toArgb()
        c.drawRoundRect(x(mx - 4f), y(s.belt + 3.2f), x(mx - 0.5f), y(s.belt + 0.4f), 0.8f * u, 0.8f * u, p)

        // Faros
        val h = s.head
        p.color = (if (lightsOn) Color(0xFFFFF4C2) else lerp(Color(0xFF444444), Color(0xFFE6ECF0), lit)).toArgb()
        c.drawRoundRect(x(h[0]), y(h[3]), x(h[2]), y(h[1]), 1f * u, 1f * u, p)
        val t = s.tail
        p.color = (if (lightsOn) Color(0xFFFF2A2A) else lerp(Color(0xFF3A0000), Color(0xFFB71C1C), lit)).toArgb()
        c.drawRoundRect(x(t[0]), y(t[3]), x(t[2]), y(t[1]), 0.8f * u, 0.8f * u, p)

        if (lightsOn) {
            val hx = x(h[2]); val hy = (y(h[1]) + y(h[3])) / 2f
            p.shader = RadialGradient(hx, hy, 7f * u, intArrayOf(0xAAFFF1B8.toInt(), 0x00FFF1B8), null, Shader.TileMode.CLAMP)
            c.drawCircle(hx, hy, 7f * u, p)
            val tx = x(t[0]); val ty = (y(t[1]) + y(t[3])) / 2f
            p.shader = RadialGradient(tx, ty, 6f * u, intArrayOf(0x99FF2020.toInt(), 0x00FF2020), null, Shader.TileMode.CLAMP)
            c.drawCircle(tx, ty, 6f * u, p)
            // Haz de luz sobre el camino
            val beam = Path().apply {
                moveTo(hx, hy - 1.2f * u)
                lineTo(hx + 75f * u, hy - 9f * u)
                lineTo(hx + 75f * u, groundY + 3f * u)
                lineTo(hx, hy + 1.2f * u)
                close()
            }
            p.shader = LinearGradient(hx, 0f, hx + 75f * u, 0f, 0x66FFF1B8, 0x00FFF1B8, Shader.TileMode.CLAMP)
            c.drawPath(beam, p)
            p.shader = null
        }

        // Pasos de rueda (recortados a la carrocería)
        val r = s.wheelR * u
        c.save()
        c.clipPath(bodyPath)
        for (wx in s.wheelX) {
            if (s.cladding) {
                p.color = lerp(Color.Black, Color(0xFF2B2B2B), lit).toArgb()
                c.drawCircle(x(wx), wy(s.wheelR), r * 1.3f, p)
            }
            p.color = 0xFF0A0A0A.toInt()
            c.drawCircle(x(wx), wy(s.wheelR), r * 1.12f, p)
        }
        c.restore()

        // Ruedas
        for (wx in s.wheelX) {
            val cx = x(wx); val cy = wy(s.wheelR)
            p.color = 0xFF141414.toInt()
            c.drawCircle(cx, cy, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.06f * r
            p.color = 0xFF2C2C2C.toInt()
            c.drawCircle(cx, cy, r * 0.9f, p)
            p.style = Paint.Style.FILL
            p.color = lerp(Color(0xFF2A2D30), Color(0xFFC9CED3), lit).toArgb()
            c.drawCircle(cx, cy, r * 0.63f, p)
            p.color = lerp(Color(0xFF101112), Color(0xFF545A60), lit).toArgb()
            c.drawCircle(cx, cy, r * 0.52f, p)
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 0.15f * r
            p.color = lerp(Color(0xFF2A2D30), Color(0xFFC9CED3), lit).toArgb()
            for (k in 0 until 5) {
                val a = wheelAngle + k * (2f * Math.PI.toFloat() / 5f)
                val ca = cos(a); val sa = sin(a)
                c.drawLine(cx + ca * r * 0.12f, cy + sa * r * 0.12f, cx + ca * r * 0.56f, cy + sa * r * 0.56f, p)
            }
            p.style = Paint.Style.FILL
            c.drawCircle(cx, cy, r * 0.15f, p)
            p.color = 0xFF202020.toInt()
            c.drawCircle(cx, cy, r * 0.05f, p)
        }
    }
}

/** Altura total aproximada del carro en unidades (para encuadrarlo). */
fun carHeightUnits(body: BodyType): Float = SPECS.getValue(body).height
