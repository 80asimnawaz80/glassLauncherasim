package com.example.glasslauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Home() }
    }
}

private val Cyan = Color(0xFF35E0FF)
private val Sub = Color(0xFFB9C4EE)
private val Themes = listOf(
    listOf(Color(0xFF3A2BB5), Color(0xFF0B1033), Color(0xFF8A1F66)),   // violet
    listOf(Color(0xFF0B5C7A), Color(0xFF071A2E), Color(0xFF0B7A5A)),   // ocean
    listOf(Color(0xFF7A2B1F), Color(0xFF1A0B20), Color(0xFF3A2BB5)))   // sunset
private val ThemeNames = listOf("Violet", "Ocean", "Sunset")
private val IconDp = listOf(44.dp, 56.dp, 72.dp)
private val Types = mapOf("pip" to "PiP Video (TV/YouTube)", "clock" to "Clock",
    "weather" to "Weather", "date" to "Date", "system" to "System")

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Glass(onClick: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null,
          content: @Composable BoxScope.() -> Unit) {
    Surface(onClick = onClick, onLongClick = onLongClick, modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = .10f), focusedContainerColor = Color.White.copy(alpha = .22f)),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = .28f))),
            focusedBorder = Border(BorderStroke(3.dp, Cyan)))
    ) { Box(Modifier.fillMaxSize(), content = content) }
}

@Composable private fun Head(t: String) = Text(t, color = Sub, fontSize = 14.sp)

@Composable
private fun Tile(emoji: String, label: String, onClick: () -> Unit) =
    Glass(onClick, Modifier.size(140.dp, 104.dp)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 30.sp); Text(label, color = Color.White, fontSize = 14.sp, maxLines = 1)
        }
    }

@Composable
private fun Panel(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) = Dialog(onDismiss) {
    Column(Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF1E245A)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun Btn(text: String, color: Color = Color.White, onClick: () -> Unit) =
    Glass(onClick, Modifier.fillMaxWidth().height(52.dp)) {
        Text(text, Modifier.align(Alignment.CenterStart).padding(start = 18.dp), color = color)
    }

@Composable
private fun InputDialog(title: String, hint: String, initial: String, ok: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    Panel(onDismiss) {
        Text(title, color = Color.White, fontSize = 18.sp)
        Text(hint, color = Sub, fontSize = 13.sp)
        TextField(text, { text = it }, singleLine = true)
        Btn(ok) { onOk(text.trim()); onDismiss() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Home() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { Store(ctx) }
    var widgets by remember { mutableStateOf(store.widgets) }
    var pipSrc by remember { mutableStateOf(store.pip) }
    var theme by remember { mutableStateOf(store.theme) }
    var names by remember { mutableStateOf(store.showNames) }
    var icon by remember { mutableStateOf(store.iconSize) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var menu by remember { mutableStateOf<Int?>(null) }          // widget menu: index, -1 = add
    var appMenu by remember { mutableStateOf<AppItem?>(null) }
    var dlg by remember { mutableStateOf("") }                    // pip | search | apk | prefs
    var now by remember { mutableStateOf(Date()) }

    fun reload() = scope.launch { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }
    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(30_000) } }
    // refresh the list the moment an app is installed / removed
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { reload() } }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package") }
        ContextCompat.registerReceiver(ctx, r, f, ContextCompat.RECEIVER_EXPORTED)
        onDispose { ctx.unregisterReceiver(r) }
    }

    LazyColumn(Modifier.fillMaxSize().background(Brush.linearGradient(Themes[theme])),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {

        item { Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), fontSize = 44.sp,
            fontWeight = FontWeight.Light, color = Color.White) }

        item {
            Head("HOME WIDGETS (${widgets.size}/$MAX_WIDGETS)")
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                widgets.forEachIndexed { i, t ->
                    Glass({ menu = i }, Modifier.width(if (t == "pip") 268.dp else 372.dp).height(150.dp)) {
                        when (t) {
                            "pip" -> Pip(pipSrc, Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)))
                            "clock" -> Label("CLOCK", SimpleDateFormat("HH:mm", Locale.getDefault()).format(now))
                            "weather" -> Label("WEATHER", "27°C ⛅")   // TODO: hook a weather API
                            "date" -> Label("TODAY", SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now))
                            else -> Label("SYSTEM", "RAM ${Runtime.getRuntime().totalMemory() * 100 / Runtime.getRuntime().maxMemory()}%")
                        }
                    }
                }
                if (widgets.size < MAX_WIDGETS) Glass({ menu = -1 }, Modifier.width(372.dp).height(150.dp)) {
                    Text("+  Add widget", Modifier.align(Alignment.Center), color = Color.White, fontSize = 20.sp)
                }
            }
        }

        item {
            Head("APPS  (hold OK on an app for Uninstall / Info)")
            LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(8.dp)) {
                items(apps, key = { it.pkg }) { a ->
                    Glass({ (ctx.packageManager.getLeanbackLaunchIntentForPackage(a.pkg) ?: ctx.packageManager.getLaunchIntentForPackage(a.pkg))?.let { ctx.go(it) } },
                        Modifier.size(140.dp, if (names) 128.dp else 104.dp), onLongClick = { appMenu = a }) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(a.icon, a.label, Modifier.size(IconDp[icon]))
                            if (names) Text(a.label, color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        item {
            Head("STORE & INSTALL")
            LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(8.dp)) {
                item { Tile("🛍", "Play Store") { ctx.openPlayStore() } }
                item { Tile("🔎", "Find & install") { dlg = "search" } }
                item { Tile("⬇️", "Install APK link") { dlg = "apk" } }
                item { Tile("🗑", "Uninstall apps") { ctx.openSetting(android.provider.Settings.ACTION_APPLICATION_SETTINGS) } }
            }
        }

        item {
            Head("SETTINGS")
            LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(8.dp)) {
                item { Tile("🎨", "Launcher look") { dlg = "prefs" } }
                items(SystemSettings) { (e, p) -> Tile(e, p.first) { ctx.openSetting(p.second) } }
            }
        }
    }

    menu?.let { idx ->
        Panel({ menu = null }) {
            Text(if (idx < 0) "Add widget" else "Change widget", color = Color.White, fontSize = 20.sp)
            if (idx >= 0 && widgets[idx] == "pip") Btn("Change video source") { menu = null; dlg = "pip" }
            Types.forEach { (k, name) ->
                Btn(name) {
                    widgets = (if (idx < 0) widgets + k else widgets.toMutableList().also { it[idx] = k }).take(MAX_WIDGETS)
                    store.widgets = widgets; menu = null
                    if (k == "pip" && pipSrc.isBlank()) dlg = "pip"
                }
            }
            if (idx >= 0) Btn("Remove widget", Color(0xFFFF4FA8)) { widgets = widgets.filterIndexed { j, _ -> j != idx }; store.widgets = widgets; menu = null }
        }
    }

    appMenu?.let { a ->
        Panel({ appMenu = null }) {
            Text(a.label, color = Color.White, fontSize = 20.sp)
            Btn("Open") { (ctx.packageManager.getLeanbackLaunchIntentForPackage(a.pkg) ?: ctx.packageManager.getLaunchIntentForPackage(a.pkg))?.let { ctx.go(it) }; appMenu = null }
            Btn("App info") { ctx.appInfo(a.pkg); appMenu = null }
            Btn("Uninstall", Color(0xFFFF4FA8)) { ctx.uninstall(a.pkg); appMenu = null }
        }
    }

    when (dlg) {
        "pip" -> InputDialog("Video source", "YouTube link / ID, or live stream (.m3u8)", pipSrc, "Play in widget",
            { pipSrc = it; store.pip = it }) { dlg = "" }
        "search" -> InputDialog("Find & install", "Type an app name – the Play Store opens with results", "", "Search Play Store",
            { if (it.isNotBlank()) ctx.storeSearch(it) }) { dlg = "" }
        "apk" -> InputDialog("Install from APK link", "Only use links you trust (https recommended)", "https://", "Download & install",
            { if (it.startsWith("http")) ctx.downloadApk(it) }) { dlg = "" }
        "prefs" -> Panel({ dlg = "" }) {
            Text("Launcher look", color = Color.White, fontSize = 20.sp)
            Btn("Background: ${ThemeNames[theme]}") { theme = (theme + 1) % Themes.size; store.theme = theme }
            Btn("App names: ${if (names) "On" else "Off"}") { names = !names; store.showNames = names }
            Btn("Icon size: ${listOf("Small", "Medium", "Large")[icon]}") { icon = (icon + 1) % 3; store.iconSize = icon }
            Btn("Set as default home screen") { ctx.openSetting(android.provider.Settings.ACTION_HOME_SETTINGS) }
        }
    }
}

@Composable
private fun BoxScope.Label(title: String, value: String) {
    Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 22.dp)) {
        Text(title, color = Sub, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
    }
}
