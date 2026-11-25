package com.artefgo.metro.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

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

// ---------- Diseño del panel izquierdo ----------

/** Mosaico anclado. size: 0 = chico (1×1), 1 = ancho (2×1), 2 = grande (2×2). */
data class TileCfg(val pkg: String, val size: Int = 0)

data class StartLayout(
    /** null = todavía no se edita: se usan los anclados automáticos. */
    val tiles: List<TileCfg>? = null,
    val hidden: Set<String> = emptySet(),
    val labels: Map<String, String> = emptyMap(),
    val colors: Map<String, Long> = emptyMap()
)

// ---------- Widgets del panel derecho ----------

enum class WidgetType(val title: String, val defaultSpan: Int) {
    CLOCK("Hora y fecha", 1),
    WEATHER("Clima", 1),
    SPEED("Velocidad", 1),
    RPM("RPM", 1),
    ALTITUDE("Altitud", 1),
    BATTERY("Batería", 1),
    COOLANT("Temperatura del motor", 1),
    OBD_STATUS("Estado del OBD2", 2),
    MEDIA("Música", 2)
}

/** span: 1 = media columna, 2 = ancho completo. */
data class WidgetCfg(val type: WidgetType, val span: Int, val color: Long? = null)

val DEFAULT_WIDGETS = listOf(
    WidgetCfg(WidgetType.CLOCK, 1), WidgetCfg(WidgetType.WEATHER, 1),
    WidgetCfg(WidgetType.SPEED, 1), WidgetCfg(WidgetType.RPM, 1),
    WidgetCfg(WidgetType.ALTITUDE, 1), WidgetCfg(WidgetType.BATTERY, 1),
    WidgetCfg(WidgetType.OBD_STATUS, 2), WidgetCfg(WidgetType.MEDIA, 2)
)

private fun encodeStart(l: StartLayout): String = JSONObject().apply {
    l.tiles?.let { tiles ->
        put("tiles", JSONArray().apply { tiles.forEach { put(JSONObject().put("p", it.pkg).put("s", it.size)) } })
    }
    put("hidden", JSONArray().apply { l.hidden.forEach { put(it) } })
    put("labels", JSONObject().apply { l.labels.forEach { (k, v) -> put(k, v) } })
    put("colors", JSONObject().apply { l.colors.forEach { (k, v) -> put(k, v) } })
}.toString()

private fun decodeStart(json: String?): StartLayout {
    if (json == null) return StartLayout()
    return runCatching {
        val o = JSONObject(json)
        val tiles = o.optJSONArray("tiles")?.let { a ->
            (0 until a.length()).map { i -> a.getJSONObject(i).let { TileCfg(it.getString("p"), it.optInt("s", 0)) } }
        }
        val hidden = o.optJSONArray("hidden")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
        val labels = o.optJSONObject("labels")?.let { j -> j.keys().asSequence().associateWith { j.getString(it) } } ?: emptyMap()
        val colors = o.optJSONObject("colors")?.let { j -> j.keys().asSequence().associateWith { j.getLong(it) } } ?: emptyMap()
        StartLayout(tiles, hidden, labels, colors)
    }.getOrDefault(StartLayout())
}

private fun encodeWidgets(list: List<WidgetCfg>): String = JSONArray().apply {
    list.forEach { w ->
        put(JSONObject().put("t", w.type.name).put("s", w.span).apply { w.color?.let { put("c", it) } })
    }
}.toString()

private fun decodeWidgets(json: String?): List<WidgetCfg> {
    if (json == null) return DEFAULT_WIDGETS
    return runCatching {
        val a = JSONArray(json)
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            val t = runCatching { WidgetType.valueOf(o.getString("t")) }.getOrNull() ?: return@mapNotNull null
            WidgetCfg(t, o.optInt("s", t.defaultSpan).coerceIn(1, 2), if (o.has("c")) o.getLong("c") else null)
        }
    }.getOrDefault(DEFAULT_WIDGETS)
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("metro_prefs", Context.MODE_PRIVATE)

    private val _car = MutableStateFlow(readCar())
    val car: StateFlow<CarConfig> = _car

    private val _start = MutableStateFlow(decodeStart(sp.getString("startLayout", null)))
    val start: StateFlow<StartLayout> = _start

    private val _widgets = MutableStateFlow(decodeWidgets(sp.getString("widgets", null)))
    val widgets: StateFlow<List<WidgetCfg>> = _widgets

    fun setStart(l: StartLayout) {
        sp.edit().putString("startLayout", encodeStart(l)).apply()
        _start.value = l
    }

    fun setWidgets(list: List<WidgetCfg>) {
        sp.edit().putString("widgets", encodeWidgets(list)).apply()
        _widgets.value = list
    }

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

    fun setCar(c: CarConfig) {
        sp.edit().putString("brand", c.brand).putString("body", c.body.name).putLong("color", c.color).apply()
        _car.value = c
    }

    fun setObdAddress(address: String?) {
        sp.edit().putString("obd", address).apply()
        _obd.value = address
    }
}

/** Colores para personalizar un mosaico o widget (además del color del tema). */
val COLOR_CHOICES: List<Long> = listOf(
    0xFFA4C400, 0xFF60A917, 0xFF008A00, 0xFF00ABA9, 0xFF1BA1E2, 0xFF0050EF, 0xFF6A00FF, 0xFFAA00FF,
    0xFFF472D0, 0xFFD80073, 0xFFA20025, 0xFFE51400, 0xFFFA6800, 0xFFF0A30A, 0xFFE3C800, 0xFF825A2C,
    0xFF6D8764, 0xFF647687, 0xFF76608A, 0xFF87794E, 0xFF333333, 0x66FFFFFFL
)
