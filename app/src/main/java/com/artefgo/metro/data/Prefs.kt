package com.artefgo.metro.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class BodyType(val label: String) {
    SEDAN("Sedán"),
    HATCHBACK("Hatchback"),
    SUV("SUV"),
    MINI_SUV("Mini SUV")
}

val BRANDS = listOf(
    "Volkswagen", "Seat", "Škoda", "Chevrolet", "Fiat", "Mazda", "Toyota", "Kia",
    "Nissan", "Honda", "Hyundai", "Ford", "Renault", "Peugeot", "MG", "BYD"
)

val CAR_COLORS: List<Long> = listOf(
    0xFFC62828, 0xFFE53935, 0xFFFF7043, 0xFFFFB300,
    0xFFF5F5F5, 0xFFB0BEC5, 0xFF607D8B, 0xFF263238,
    0xFF111111, 0xFF1565C0, 0xFF42A5F5, 0xFF00897B,
    0xFF2E7D32, 0xFF6A1B9A, 0xFF8D6E63, 0xFFD4AF37
)

data class CarConfig(val brand: String, val body: BodyType, val color: Long)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("metro_prefs", Context.MODE_PRIVATE)

    private val _car = MutableStateFlow(readCar())
    val car: StateFlow<CarConfig> = _car

    /** null = nunca configurado (se usan los anclados automáticos). */
    private val _pinned = MutableStateFlow(readPinned())
    val pinned: StateFlow<List<String>?> = _pinned

    private val _wide = MutableStateFlow(sp.getStringSet("wide", null)?.toSet())
    val wide: StateFlow<Set<String>?> = _wide

    private val _obd = MutableStateFlow(sp.getString("obd", null))
    val obdAddress: StateFlow<String?> = _obd

    private val _theme = MutableStateFlow(sp.getInt("theme", 5))
    val themeId: StateFlow<Int> = _theme

    /** Qué tanto se oscurece la imagen de fondo (0 = nada, 0.8 = casi negro). */
    private val _bgDim = MutableStateFlow(sp.getFloat("bgDim", 0.35f))
    val bgDim: StateFlow<Float> = _bgDim

    fun setTheme(id: Int) {
        sp.edit().putInt("theme", id).apply()
        _theme.value = id
    }

    fun setBgDim(v: Float) {
        sp.edit().putFloat("bgDim", v).apply()
        _bgDim.value = v
    }

    private fun readCar() = CarConfig(
        brand = sp.getString("brand", "Volkswagen") ?: "Volkswagen",
        body = runCatching { BodyType.valueOf(sp.getString("body", "HATCHBACK") ?: "HATCHBACK") }
            .getOrDefault(BodyType.HATCHBACK),
        color = sp.getLong("color", 0xFF1565C0)
    )

    private fun readPinned(): List<String>? =
        sp.getString("pinned", null)?.split('|')?.filter { it.isNotBlank() }

    fun setCar(c: CarConfig) {
        sp.edit().putString("brand", c.brand).putString("body", c.body.name).putLong("color", c.color).apply()
        _car.value = c
    }

    fun setPinned(list: List<String>) {
        sp.edit().putString("pinned", list.joinToString("|")).apply()
        _pinned.value = list
    }

    fun setWide(set: Set<String>) {
        sp.edit().putStringSet("wide", HashSet(set)).apply()
        _wide.value = set
    }

    fun setObdAddress(address: String?) {
        sp.edit().putString("obd", address).apply()
        _obd.value = address
    }
}
