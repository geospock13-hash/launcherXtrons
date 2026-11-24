package com.artefgo.metro.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GpsState(
    val speedKmh: Float? = null,
    val altitudeM: Double? = null,
    val lat: Double? = null,
    val lon: Double? = null
)

class LocationRepository(private val context: Context) : LocationListener {
    private val lm = context.getSystemService(LocationManager::class.java)
    private val _state = MutableStateFlow(GpsState())
    val state: StateFlow<GpsState> = _state
    private var running = false

    fun hasPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || lm == null || !hasPermission()) return
        try {
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (lm.allProviders.contains(p)) {
                    lm.requestLocationUpdates(p, 1000L, 0f, this, Looper.getMainLooper())
                    lm.getLastKnownLocation(p)?.let { onLocationChanged(it) }
                }
            }
            running = true
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!running) return
        runCatching { lm?.removeUpdates(this) }
        running = false
    }

    override fun onLocationChanged(location: Location) {
        val old = _state.value
        val gps = location.provider == LocationManager.GPS_PROVIDER
        _state.value = GpsState(
            speedKmh = if (location.hasSpeed()) location.speed * 3.6f else if (gps) 0f else old.speedKmh,
            altitudeM = if (location.hasAltitude()) location.altitude else old.altitudeM,
            lat = location.latitude,
            lon = location.longitude
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
