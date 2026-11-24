package com.artefgo.metro.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tema del launcher.
 * @param palette null = todos los mosaicos del color de acento (como Windows Phone);
 *                lista = mosaicos multicolor.
 */
data class LauncherTheme(
    val id: Int,
    val name: String,
    val group: String,
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
    val palette: List<Color>? = null,
    val light: Boolean = false
) {
    val text: Color get() = if (light) Color(0xFF111111) else Color.White
    val subText: Color get() = if (light) Color(0xFF555555) else Color(0xFFAAAAAA)
    val panel: Color get() = if (light) Color(0x1A000000) else Color(0x24FFFFFF)

    fun tileColor(key: String): Color =
        palette?.let { it[(key.hashCode() and 0x7fffffff) % it.size] } ?: accent
}

object Themes {
    private val WP = listOf(
        "Lima" to 0xFFA4C400, "Verde" to 0xFF60A917, "Esmeralda" to 0xFF008A00, "Turquesa" to 0xFF00ABA9,
        "Cian" to 0xFF1BA1E2, "Cobalto" to 0xFF0050EF, "Índigo" to 0xFF6A00FF, "Violeta" to 0xFFAA00FF,
        "Rosa" to 0xFFF472D0, "Magenta" to 0xFFD80073, "Carmesí" to 0xFFA20025, "Rojo" to 0xFFE51400,
        "Naranja" to 0xFFFA6800, "Ámbar" to 0xFFF0A30A, "Amarillo" to 0xFFE3C800, "Café" to 0xFF825A2C,
        "Oliva" to 0xFF6D8764, "Acero" to 0xFF647687, "Malva" to 0xFF76608A, "Topo" to 0xFF87794E
    )

    private fun pal(vararg c: Long) = c.map { Color(it) }

    val all: List<LauncherTheme> = buildList {
        // 20 · Oscuro clásico: fondo negro, todos los mosaicos del mismo color
        WP.forEach { (n, c) ->
            add(LauncherTheme(size, n, "Oscuro", Color.Black, Color.Black, Color(c)))
        }
        // 10 · Claro: fondo blanco
        listOf(5, 4, 3, 2, 11, 9, 7, 12, 13, 17).forEach { i ->
            val (n, c) = WP[i]
            add(LauncherTheme(size, "$n claro", "Claro", Color(0xFFF2F2F2), Color.White, Color(c), light = true))
        }
        // 10 · Degradados
        add(LauncherTheme(size, "Atardecer", "Degradado", Color(0xFF2B1055), Color(0xFFD53369), Color(0xFFFF7E5F)))
        add(LauncherTheme(size, "Océano", "Degradado", Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF1BA1E2)))
        add(LauncherTheme(size, "Aurora", "Degradado", Color(0xFF0B3D2E), Color(0xFF1E1B4B), Color(0xFF00BFA5)))
        add(LauncherTheme(size, "Neón", "Degradado", Color(0xFF0D0221), Color(0xFF261447), Color(0xFFFF2A6D)))
        add(LauncherTheme(size, "Bosque", "Degradado", Color(0xFF0B2010), Color(0xFF1E3B20), Color(0xFF43A047)))
        add(LauncherTheme(size, "Lava", "Degradado", Color(0xFF1A0000), Color(0xFF4A0E0E), Color(0xFFFF5722)))
        add(LauncherTheme(size, "Medianoche", "Degradado", Color(0xFF020111), Color(0xFF191654), Color(0xFF5C6BC0)))
        add(LauncherTheme(size, "Uva", "Degradado", Color(0xFF1F0036), Color(0xFF4A148C), Color(0xFFAB47BC)))
        add(LauncherTheme(size, "Menta", "Degradado", Color(0xFF00241B), Color(0xFF00483A), Color(0xFF00BFA5)))
        add(LauncherTheme(size, "Carbón", "Degradado", Color(0xFF141414), Color(0xFF2B2B2B), Color(0xFF757575)))
        // 8 · Multicolor
        add(LauncherTheme(size, "Mosaico", "Multicolor", Color.Black, Color.Black, Metro.Cyan, Metro.accents))
        add(LauncherTheme(size, "Pastel", "Multicolor", Color(0xFF1B1B22), Color(0xFF26222E), Color(0xFF64B5F6),
            pal(0xFFE57373, 0xFF66BB6A, 0xFF64B5F6, 0xFFFFA726, 0xFFBA68C8, 0xFF4DB6AC, 0xFFF06292, 0xFF9575CD)))
        add(LauncherTheme(size, "Neón multicolor", "Multicolor", Color(0xFF07020F), Color(0xFF14082A), Color(0xFFF15BB5),
            pal(0xFFFF2A6D, 0xFF00BBF9, 0xFFF15BB5, 0xFF9B5DE5, 0xFF00C49A, 0xFFFF6D00)))
        add(LauncherTheme(size, "Tierra", "Multicolor", Color(0xFF1A140F), Color(0xFF2A2018), Color(0xFF8D6E63),
            pal(0xFF8D6E63, 0xFF6D4C41, 0xFF827717, 0xFF558B2F, 0xFFBF360C, 0xFF795548)))
        add(LauncherTheme(size, "Frío", "Multicolor", Color(0xFF050B18), Color(0xFF0B1A30), Color(0xFF0277BD),
            pal(0xFF0D47A1, 0xFF1565C0, 0xFF0277BD, 0xFF00838F, 0xFF00695C, 0xFF283593, 0xFF4527A0)))
        add(LauncherTheme(size, "Cálido", "Multicolor", Color(0xFF180806), Color(0xFF2A0F08), Color(0xFFEF6C00),
            pal(0xFFB71C1C, 0xFFD84315, 0xFFEF6C00, 0xFFC62828, 0xFFAD1457, 0xFFE65100)))
        add(LauncherTheme(size, "Grises", "Multicolor", Color(0xFF0E0E0E), Color(0xFF1C1C1C), Color(0xFF616161),
            pal(0xFF424242, 0xFF616161, 0xFF37474F, 0xFF546E7A, 0xFF2E2E2E)))
        add(LauncherTheme(size, "Arcoíris", "Multicolor", Color.Black, Color(0xFF101010), Color(0xFF1E88E5),
            pal(0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA)))
        // 2 · Especiales
        add(LauncherTheme(size, "Fibra de carbono", "Especial", Color(0xFF1E1E1E), Color(0xFF080808), Color(0xFFD32F2F)))
        add(LauncherTheme(size, "Cristal", "Especial", Color(0xFF1A2A3A), Color(0xFF0A1420), Color(0x33FFFFFF)))
    }

    fun byId(id: Int): LauncherTheme = all.getOrElse(id) { all[5] }
}

val LocalLauncherTheme = staticCompositionLocalOf { Themes.byId(5) }

/** true cuando hay imagen de fondo: los mosaicos se vuelven un poco translúcidos. */
val LocalHasWallpaper = staticCompositionLocalOf { false }
