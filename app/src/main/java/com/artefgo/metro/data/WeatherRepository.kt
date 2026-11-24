package com.artefgo.metro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class Weather(
    val tempC: Double,
    val code: Int,
    val isDay: Boolean,
    val humidity: Int?,
    val windKmh: Double?
)

/** Clima con Open-Meteo (gratis, sin clave). */
object WeatherRepository {
    suspend fun fetch(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                        "&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m&timezone=auto",
                    lat, lon
                )
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val c = JSONObject(text).getJSONObject("current")
                Weather(
                    tempC = c.getDouble("temperature_2m"),
                    code = c.getInt("weather_code"),
                    isDay = c.optInt("is_day", 1) == 1,
                    humidity = if (c.has("relative_humidity_2m")) c.getInt("relative_humidity_2m") else null,
                    windKmh = if (c.has("wind_speed_10m")) c.getDouble("wind_speed_10m") else null
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        in 51..57 -> "Llovizna"
        in 61..67 -> "Lluvia"
        in 71..77 -> "Nieve"
        in 80..82 -> "Chubascos"
        85, 86 -> "Chubascos de nieve"
        in 95..99 -> "Tormenta"
        else -> "—"
    }

    fun isRain(code: Int) = code in 51..67 || code in 80..82 || code in 95..99

    fun cloudiness(code: Int): Float = when {
        code == 0 -> 0.1f
        code == 1 -> 0.3f
        code == 2 -> 0.55f
        code == 3 || code == 45 || code == 48 -> 0.85f
        isRain(code) || code in 71..86 -> 1f
        else -> 0.3f
    }
}
