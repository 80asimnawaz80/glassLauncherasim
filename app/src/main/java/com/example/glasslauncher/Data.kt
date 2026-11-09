package com.example.glasslauncher

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

const val MAX_WIDGETS = 3

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("glass", Context.MODE_PRIVATE)
    var widgets: List<String>
        get() = p.getString("w", "clock,weather,date")!!.split(",").filter { it.isNotBlank() }.take(MAX_WIDGETS)
        set(v) { p.edit().putString("w", v.take(MAX_WIDGETS).joinToString(",")).apply() }
    var pip: String
        get() = p.getString("pip", "")!!
        set(v) { p.edit().putString("pip", v).apply() }
    var theme: Int                       // 0..2 background look
        get() = p.getInt("theme", 0)
        set(v) { p.edit().putInt("theme", v).apply() }
    var showNames: Boolean
        get() = p.getBoolean("names", true)
        set(v) { p.edit().putBoolean("names", v).apply() }
    var iconSize: Int                    // 0 small, 1 medium, 2 large
        get() = p.getInt("icon", 1)
        set(v) { p.edit().putInt("icon", v).apply() }
}

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap)

// Always call on a background thread so the UI never hangs.
fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    var list = pm.queryIntentActivities(tv, 0)
    if (list.isEmpty())   // phone / tablet: fall back to normal launcher apps
        list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    return list
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName,
                       it.loadIcon(pm).toBitmap(128, 128).asImageBitmap()) }
        .sortedBy { it.label.lowercase() }
}
