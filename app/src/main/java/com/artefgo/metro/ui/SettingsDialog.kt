package com.artefgo.metro.ui

import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artefgo.metro.LauncherViewModel
import com.artefgo.metro.data.BRANDS
import com.artefgo.metro.data.BodyType
import com.artefgo.metro.data.CAR_COLORS
import com.artefgo.metro.scene.drawScene

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    vm: LauncherViewModel,
    timeOverride: Float?,
    onTimeOverride: (Float?) -> Unit,
    sceneHour: Float,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val car by vm.prefs.car.collectAsStateWithLifecycle()
    val obdAddr by vm.prefs.obdAddress.collectAsStateWithLifecycle()
    val devices = remember { vm.obd.pairedDevices() }
    val themeId by vm.prefs.themeId.collectAsStateWithLifecycle()
    val start by vm.prefs.start.collectAsStateWithLifecycle()
    val apps by vm.appsRepo.apps.collectAsStateWithLifecycle()
    val bgImage by vm.bgImage.collectAsStateWithLifecycle()
    val bgDim by vm.prefs.bgDim.collectAsStateWithLifecycle()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setBackground(uri)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.92f)
                .background(Color(0xFF111111))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("configuración", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)

            // Vista previa del carro
            Canvas(Modifier.fillMaxWidth().height(170.dp).padding(top = 10.dp)) {
                drawScene(car, sceneHour, 0.0, 0.0, 0.3f, false)
            }

            Section("marca")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(BRANDS) { b -> Chip(b, car.brand == b) { vm.prefs.setCar(car.copy(brand = b)) } }
            }

            Section("tipo de carro")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyType.entries.forEach { t -> Chip(t.label, car.body == t) { vm.prefs.setCar(car.copy(body = t)) } }
            }

            Section("color")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CAR_COLORS) { c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (car.color == c) 3.dp else 1.dp, if (car.color == c) Metro.Cyan else Color(0xFF555555), CircleShape)
                            .clickable { vm.prefs.setCar(car.copy(color = c)) }
                    )
                }
            }

            Section("tema · ${Themes.all.size} opciones")
            var lastGroup = ""
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Themes.all.forEach { t ->
                    if (t.group != lastGroup) {
                        lastGroup = t.group
                        Text(
                            t.group.lowercase(), color = Metro.SubText, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                    ThemeSwatch(t, selected = t.id == themeId) { vm.prefs.setTheme(t.id) }
                }
            }

            Section("fondo con imagen")
            Text(
                "Usa una foto tuya como fondo; el tema elegido sigue dando el color de los mosaicos.",
                color = Metro.SubText, fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(if (bgImage == null) "Elegir imagen…" else "Cambiar imagen…", false) { pickImage.launch("image/*") }
                if (bgImage != null) Chip("Quitar imagen", false) { vm.clearBackground() }
                bgImage?.let {
                    Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(96.dp, 54.dp))
                }
            }
            if (bgImage != null) {
                Text("Oscurecer la imagen: ${(bgDim * 100).toInt()}%", color = Metro.SubText, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                Slider(value = bgDim, onValueChange = { vm.prefs.setBgDim(it) }, valueRange = 0f..0.8f)
            }

            Section("hora de la escena")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = timeOverride == null, onCheckedChange = { auto -> onTimeOverride(if (auto) null else 12f) })
                Spacer(Modifier.width(10.dp))
                Text("Automática (según la hora real)", color = Color.White, fontSize = 14.sp)
            }
            if (timeOverride != null) {
                val hh = timeOverride.toInt().coerceIn(0, 23)
                val mm = ((timeOverride - timeOverride.toInt()) * 60).toInt()
                Text(String.format("Vista previa: %02d:%02d", hh, mm), color = Metro.SubText, fontSize = 13.sp)
                Slider(value = timeOverride, onValueChange = { onTimeOverride(it) }, valueRange = 0f..23.99f)
            }

            Section("OBD2 · RPM, voltaje y velocidad")
            Text(
                "Empareja primero tu adaptador ELM327 Bluetooth en los ajustes de Bluetooth de la radio y luego elígelo aquí.",
                color = Metro.SubText, fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Chip("Ninguno", obdAddr == null) { vm.setObd(null) } }
                items(devices) { (name, addr) -> Chip(name, obdAddr == addr) { vm.setObd(addr) } }
            }
            if (devices.isEmpty()) {
                Text("No hay dispositivos Bluetooth emparejados (o falta el permiso).", color = Metro.SubText, fontSize = 12.sp)
            }

            Section("inicio y widgets")
            Text(
                "Mantén presionado un mosaico o widget (o toca el lápiz) para moverlo, cambiar tamaño o color, renombrarlo, ocultarlo o quitarlo.",
                color = Metro.SubText, fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Restablecer inicio", false) { vm.resetStart() }
                Chip("Restablecer widgets", false) { vm.resetWidgets() }
            }
            if (start.hidden.isNotEmpty()) {
                Text("Apps ocultas (toca para mostrarla otra vez):", color = Metro.SubText, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(start.hidden.toList()) { pkg ->
                        val name = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                        Chip(name, false) { vm.unhideApp(pkg) }
                    }
                }
            }

            Section("sistema")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Launcher predeterminado", false) { ctx.safeStart(Settings.ACTION_HOME_SETTINGS) }
                Chip("Acceso a notificaciones", false) { ctx.safeStart(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                Chip("Ajustes de Android", false) { ctx.safeStart(Settings.ACTION_SETTINGS) }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Listo", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title, color = Metro.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Light,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Metro.Cobalt else Color(0xFF262626))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun ThemeSwatch(t: LauncherTheme, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(78.dp, 50.dp)
                .background(Brush.verticalGradient(listOf(t.bgTop, t.bgBottom)))
                .border(if (selected) 3.dp else 1.dp, if (selected) Metro.Cyan else Color(0xFF444444))
                .padding(6.dp)
        ) {
            val colors = t.palette?.take(3) ?: listOf(t.accent, t.accent, t.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.align(Alignment.BottomStart)) {
                colors.forEachIndexed { i, c ->
                    Box(Modifier.size(if (i == 0) 26.dp else 12.dp, 12.dp).background(c))
                }
            }
        }
        Text(t.name, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}
