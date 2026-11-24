package com.artefgo.metro

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artefgo.metro.data.AppEntry
import com.artefgo.metro.data.AppsRepository
import com.artefgo.metro.data.LocationRepository
import com.artefgo.metro.data.MediaHub
import com.artefgo.metro.data.ObdManager
import com.artefgo.metro.data.Prefs
import com.artefgo.metro.data.Weather
import com.artefgo.metro.data.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val appsRepo = AppsRepository(app)
    val location = LocationRepository(app)
    val obd = ObdManager(app)

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather

    private val bgFile get() = File(getApplication<Application>().filesDir, "fondo.jpg")
    private val _bgImage = MutableStateFlow<ImageBitmap?>(null)
    val bgImage: StateFlow<ImageBitmap?> = _bgImage

    /** Copia la imagen elegida (reducida a máx. 2560×1440) como fondo del launcher. */
    fun setBackground(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val bmp: Bitmap? = runCatching {
                if (Build.VERSION.SDK_INT >= 28) {
                    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                        var s = 1
                        while (info.size.width / s > 2560 || info.size.height / s > 1440) s *= 2
                        decoder.setTargetSampleSize(s)
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var s = 1
                    while (bounds.outWidth / s > 2560 || bounds.outHeight / s > 1440) s *= 2
                    val opts = BitmapFactory.Options().apply { inSampleSize = s }
                    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                }
            }.getOrNull()
            if (bmp != null) {
                runCatching { bgFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
                _bgImage.value = bmp.asImageBitmap()
            }
        }
    }

    fun clearBackground() {
        runCatching { bgFile.delete() }
        _bgImage.value = null
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (bgFile.exists()) {
                BitmapFactory.decodeFile(bgFile.absolutePath)?.let { _bgImage.value = it.asImageBitmap() }
            }
        }
        // Clima cada 15 min usando la ubicación del GPS
        viewModelScope.launch {
            while (isActive) {
                val s = location.state.value
                val lat = s.lat
                val lon = s.lon
                if (lat != null && lon != null) {
                    val w = WeatherRepository.fetch(lat, lon)
                    if (w != null) {
                        _weather.value = w
                        delay(15 * 60_000L)
                    } else {
                        delay(60_000L)
                    }
                } else {
                    delay(15_000L)
                }
            }
        }
        restartObd()
    }

    private fun restartObd() = obd.start(viewModelScope, prefs.obdAddress.value)

    fun setObd(address: String?) {
        prefs.setObdAddress(address)
        restartObd()
    }

    fun onResume() {
        viewModelScope.launch { appsRepo.refresh() }
        location.start()
        MediaHub.start(getApplication())
    }

    fun onPause() {
        location.stop()
    }

    fun onPermissionsResult() {
        location.start()
        restartObd()
    }

    private fun currentPinned(): List<String> =
        prefs.pinned.value ?: AppsRepository.defaultPinned(appsRepo.apps.value)

    fun togglePin(app: AppEntry) {
        val p = currentPinned().toMutableList()
        if (app.packageName in p) p.remove(app.packageName) else p.add(app.packageName)
        prefs.setPinned(p)
    }

    fun toggleWide(app: AppEntry) {
        val w = (prefs.wide.value ?: currentPinned().take(2).toSet()).toMutableSet()
        if (!w.add(app.packageName)) w.remove(app.packageName)
        prefs.setWide(w)
    }

    override fun onCleared() {
        location.stop()
        obd.stop()
    }
}
