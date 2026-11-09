package com.example.glasslauncher

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

fun Context.go(i: Intent) {
    try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (e: Exception) { Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show() }
}

fun Context.openPlayStore() {
    val pm = packageManager
    val i = pm.getLeanbackLaunchIntentForPackage("com.android.vending") ?: pm.getLaunchIntentForPackage("com.android.vending")
    if (i != null) go(i) else Toast.makeText(this, "Play Store is not installed on this device", Toast.LENGTH_LONG).show()
}
fun Context.storeSearch(q: String) = go(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q))))
fun Context.openSetting(action: String) = go(Intent(action))
fun Context.uninstall(pkg: String) = go(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
fun Context.appInfo(pkg: String) = go(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

/** Download an APK from a link, then open the system installer. Use only links you trust. */
fun Context.downloadApk(url: String) {
    val app = applicationContext
    if (!packageManager.canRequestPackageInstalls()) {
        Toast.makeText(this, "Allow \"Install unknown apps\" for this launcher, then try again", Toast.LENGTH_LONG).show()
        go(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))); return
    }
    val f = File(getExternalFilesDir("apk"), "install_${System.currentTimeMillis()}.apk")
    val dm = getSystemService(DownloadManager::class.java)
    val id = dm.enqueue(DownloadManager.Request(Uri.parse(url)).setDestinationUri(Uri.fromFile(f))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE))
    Toast.makeText(this, "Downloading…", Toast.LENGTH_SHORT).show()
    val r = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
            c.unregisterReceiver(this)
            if (!f.exists() || f.length() == 0L) { Toast.makeText(c, "Download failed", Toast.LENGTH_LONG).show(); return }
            val uri = FileProvider.getUriForFile(c, "${c.packageName}.files", f)
            c.go(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
    }
    ContextCompat.registerReceiver(app, r, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
}

val SystemSettings = listOf(
    "📶" to ("Wi-Fi" to Settings.ACTION_WIFI_SETTINGS),
    "🔵" to ("Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS),
    "🖥" to ("Display" to Settings.ACTION_DISPLAY_SETTINGS),
    "🔊" to ("Sound" to Settings.ACTION_SOUND_SETTINGS),
    "🕒" to ("Date & time" to Settings.ACTION_DATE_SETTINGS),
    "🌐" to ("Language" to Settings.ACTION_LOCALE_SETTINGS),
    "📦" to ("Apps" to Settings.ACTION_APPLICATION_SETTINGS),
    "💾" to ("Storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
    "♿" to ("Accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS),
    "🏠" to ("Default home" to Settings.ACTION_HOME_SETTINGS),
    "⚙️" to ("All settings" to Settings.ACTION_SETTINGS))
