package com.artefgo.metro.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores de acento de Windows Phone. */
object Metro {
    val Cobalt = Color(0xFF0050EF)
    val Emerald = Color(0xFF008A00)
    val Crimson = Color(0xFFA20025)
    val Amber = Color(0xFFF0A30A)
    val Teal = Color(0xFF00ABA9)
    val Magenta = Color(0xFFD80073)
    val Indigo = Color(0xFF6A00FF)
    val Orange = Color(0xFFFA6800)
    val Cyan = Color(0xFF1BA1E2)
    val Lime = Color(0xFFA4C400)
    val Mauve = Color(0xFF76608A)
    val Olive = Color(0xFF6D8764)
    val Red = Color(0xFFE51400)
    val Steel = Color(0xFF647687)
    val Violet = Color(0xFFAA00FF)
    val Brown = Color(0xFF825A2C)

    val accents = listOf(Cobalt, Emerald, Crimson, Amber, Teal, Magenta, Indigo, Orange, Cyan, Lime, Mauve, Olive, Red, Steel, Violet, Brown)

    fun accentFor(key: String): Color = accents[(key.hashCode() and 0x7fffffff) % accents.size]

    val Panel = Color(0xFF161616)
    val SubText = Color(0xFFAAAAAA)
}

@Composable
fun MetroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Metro.Cyan,
            secondary = Metro.Cobalt,
            background = Color.Black,
            surface = Color(0xFF1A1A1A)
        ),
        content = content
    )
}

fun Context.safeStart(vararg actions: String) {
    for (a in actions) {
        val ok = runCatching { startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (ok) return
    }
    runCatching { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
