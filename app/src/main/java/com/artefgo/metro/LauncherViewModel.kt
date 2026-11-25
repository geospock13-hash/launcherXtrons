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
import com.artefgo.metro.data.AppsRepository
import com.artefgo.metro.data.COLOR_CHOICES
import com.artefgo.metro.data.DEFAULT_WIDGETS
import com.artefgo.metro.data.StartLayout
import com.artefgo.metro.data.TileCfg
import com.artefgo.metro.data.WidgetCfg
import com.artefgo.metro.data.WidgetType
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

    // ---------- Edición del panel izquierdo ----------

    fun defaultTiles(): List<TileCfg> =
        AppsRepository.defaultPinned(appsRepo.apps.value).mapIndexed { i, p -> TileCfg(p, if (i < 2) 1 else 0) }

    /** Diseño actual con los anclados automáticos ya resueltos. */
    private fun startNow(): StartLayout {
        val l = prefs.start.value
        return if (l.tiles != null) l else l.copy(tiles = defaultTiles())
    }

    private fun editTiles(block: (MutableList<TileCfg>) -> Unit) {
        val l = startNow()
        val t = l.tiles.orEmpty().toMutableList()
        block(t)
        prefs.setStart(l.copy(tiles = t))
    }

    fun isPinned(pkg: String) = startNow().tiles.orEmpty().any { it.pkg == pkg }

    fun togglePin(pkg: String) = editTiles { t ->
        if (t.any { it.pkg == pkg }) t.removeAll { it.pkg == pkg } else t.add(TileCfg(pkg))
    }

    fun moveTile(pkg: String, delta: Int) = editTiles { t ->
        val i = t.indexOfFirst { it.pkg == pkg }
        val j = i + delta
        if (i >= 0 && j in t.indices) t.add(j, t.removeAt(i))
    }

    fun cycleTileSize(pkg: String) = editTiles { t ->
        val i = t.indexOfFirst { it.pkg == pkg }
        if (i >= 0) t[i] = t[i].copy(size = (t[i].size + 1) % 3)
    }

    fun cycleTileColor(pkg: String) {
        val l = startNow()
        val next = nextColor(l.colors[pkg])
        prefs.setStart(l.copy(colors = if (next == null) l.colors - pkg else l.colors + (pkg to next)))
    }

    fun renameTile(pkg: String, label: String?) {
        val l = startNow()
        val clean = label?.trim().orEmpty()
        prefs.setStart(l.copy(labels = if (clean.isEmpty()) l.labels - pkg else l.labels + (pkg to clean)))
    }

    fun hideApp(pkg: String) {
        val l = startNow()
        prefs.setStart(l.copy(tiles = l.tiles.orEmpty().filter { it.pkg != pkg }, hidden = l.hidden + pkg))
    }

    fun unhideApp(pkg: String) {
        val l = startNow()
        prefs.setStart(l.copy(hidden = l.hidden - pkg))
    }

    fun resetStart() = prefs.setStart(StartLayout())

    // ---------- Edición del panel derecho ----------

    private fun editWidgets(block: (MutableList<WidgetCfg>) -> Unit) {
        val w = prefs.widgets.value.toMutableList()
        block(w)
        prefs.setWidgets(w)
    }

    fun moveWidget(index: Int, delta: Int) = editWidgets { w ->
        val j = index + delta
        if (index in w.indices && j in w.indices) w.add(j, w.removeAt(index))
    }

    fun cycleWidgetSpan(index: Int) = editWidgets { w ->
        if (index in w.indices) w[index] = w[index].copy(span = if (w[index].span == 1) 2 else 1)
    }

    fun cycleWidgetColor(index: Int) = editWidgets { w ->
        if (index in w.indices) w[index] = w[index].copy(color = nextColor(w[index].color))
    }

    fun removeWidget(index: Int) = editWidgets { w -> if (index in w.indices) w.removeAt(index) }

    fun addWidget(type: WidgetType) = editWidgets { w ->
        if (w.none { it.type == type }) w.add(WidgetCfg(type, type.defaultSpan))
    }

    fun resetWidgets() = prefs.setWidgets(DEFAULT_WIDGETS)

    /** null (color del tema) → colores Metro → null. */
    private fun nextColor(current: Long?): Long? {
        val choices = COLOR_CHOICES
        val i = if (current == null) -1 else choices.indexOf(current)
        return if (i + 1 >= choices.size) null else choices[i + 1]
    }

    override fun onCleared() {
        location.stop()
        obd.stop()
    }
}
