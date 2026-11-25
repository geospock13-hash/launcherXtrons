package com.artefgo.metro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artefgo.metro.LauncherViewModel
import com.artefgo.metro.data.MediaHub
import com.artefgo.metro.scene.CarScene
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var timeOverride by remember { mutableStateOf<Float?>(null) }
    var editMode by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionsResult()
    }
    LaunchedEffect(Unit) {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = perms.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }
    // El botón "atrás" nunca sale del launcher; solo cierra la configuración.
    BackHandler { showSettings = false; editMode = false }

    val apps by vm.appsRepo.apps.collectAsStateWithLifecycle()
    val start by vm.prefs.start.collectAsStateWithLifecycle()
    val widgets by vm.prefs.widgets.collectAsStateWithLifecycle()
    val car by vm.prefs.car.collectAsStateWithLifecycle()
    val gps by vm.location.state.collectAsStateWithLifecycle()
    val obd by vm.obd.state.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val media by MediaHub.nowPlaying.collectAsStateWithLifecycle()
    val hasMedia by MediaHub.hasAccess.collectAsStateWithLifecycle()
    val themeId by vm.prefs.themeId.collectAsStateWithLifecycle()
    val bgImage by vm.bgImage.collectAsStateWithLifecycle()
    val bgDim by vm.prefs.bgDim.collectAsStateWithLifecycle()
    val theme = Themes.byId(themeId)

    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000)
        }
    }
    val realHour = now.hour + now.minute / 60f + now.second / 3600f
    val sceneHour = timeOverride ?: realHour
    val speed: Float? = obd.speedKmh?.toFloat() ?: gps.speedKmh

    CompositionLocalProvider(LocalLauncherTheme provides theme, LocalHasWallpaper provides (bgImage != null)) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))) {
        bgImage?.let { img ->
            Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDim)))
        }
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TilesPanel(
                apps = apps,
                layout = start,
                defaultTiles = remember(apps) { vm.defaultTiles() },
                editMode = editMode,
                onEditMode = { editMode = it },
                actions = remember {
                    TileActions(
                        launch = { vm.appsRepo.launch(it) },
                        togglePin = vm::togglePin,
                        move = vm::moveTile,
                        cycleSize = vm::cycleTileSize,
                        cycleColor = vm::cycleTileColor,
                        rename = vm::renameTile,
                        hide = vm::hideApp,
                        uninstall = { vm.appsRepo.uninstall(it) },
                        info = { vm.appsRepo.openAppInfo(it) }
                    )
                },
                onOpenSettings = { showSettings = true },
                modifier = Modifier.weight(0.31f).fillMaxHeight()
            )
            CarScene(
                car = car,
                hour = sceneHour,
                speedKmh = speed,
                weatherCode = weather?.code,
                modifier = Modifier
                    .weight(0.39f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .clickable { showSettings = true }
            )
            InfoPanel(
                widgets = widgets,
                editMode = editMode,
                onEditMode = { editMode = it },
                actions = remember {
                    WidgetActions(
                        move = vm::moveWidget,
                        cycleSpan = vm::cycleWidgetSpan,
                        cycleColor = vm::cycleWidgetColor,
                        remove = vm::removeWidget,
                        add = vm::addWidget
                    )
                },
                now = now,
                weather = weather,
                gps = gps,
                obd = obd,
                media = media,
                hasMediaAccess = hasMedia,
                onGrantMedia = { context.safeStart(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
                onOpenMediaApp = { MediaHub.openCurrentApp(context) },
                modifier = Modifier.weight(0.30f).fillMaxHeight()
            )
        }
        if (showSettings) {
            SettingsDialog(
                vm = vm,
                timeOverride = timeOverride,
                onTimeOverride = { timeOverride = it },
                sceneHour = sceneHour,
                onDismiss = { showSettings = false }
            )
        }
    }
}
    }
}
