package com.artefgo.metro.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap
)

class AppsRepository(private val context: Context) {
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    suspend fun refresh() {
        val list = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .mapNotNull { ri ->
                    runCatching {
                        AppEntry(
                            label = ri.loadLabel(pm).toString(),
                            packageName = ri.activityInfo.packageName,
                            component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                            icon = ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap()
                        )
                    }.getOrNull()
                }
                .sortedBy { it.label.lowercase() }
        }
        _apps.value = list
    }

    fun launch(app: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(i) }
    }

    fun openAppInfo(app: AppEntry) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }

    companion object {
        /** Apps típicas de la radio que se anclan arriba la primera vez. */
        private val KEYWORDS = listOf(
            "radio", "zlink", "carplay", "android auto", "dsp", "ecualiz", "música", "music",
            "poweramp", "spotify", "video", "bluetooth", "navi", "maps", "waze", "ajustes", "settings", "configura"
        )

        fun defaultPinned(apps: List<AppEntry>): List<String> {
            val out = LinkedHashSet<String>()
            for (k in KEYWORDS) {
                apps.firstOrNull { it.label.lowercase().contains(k) && it.packageName !in out }
                    ?.let { out += it.packageName }
            }
            return out.toList()
        }
    }
}
