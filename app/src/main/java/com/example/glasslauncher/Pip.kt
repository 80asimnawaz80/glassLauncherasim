package com.example.glasslauncher

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val YT = Regex("""(?:youtu\.be/|v=|embed/|live/)([\w-]{11})""")

@Composable
fun Pip(src: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val yt = YT.find(src)?.groupValues?.get(1) ?: src.takeIf { Regex("^[\\w-]{11}$").matches(it) }
    when {
        yt != null -> AndroidView(modifier = modifier, factory = {
            WebView(it).apply {
                isFocusable = false; isFocusableInTouchMode = false
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                loadUrl("https://www.youtube.com/embed/$yt?autoplay=1&mute=1&controls=0&playsinline=1")
            }
        }, onRelease = { it.destroy() })
        src.startsWith("http") -> {   // live TV (HLS .m3u8) or any video URL
            val player = remember { ExoPlayer.Builder(ctx).build().apply { volume = 0f } }
            LaunchedEffect(src) { player.setMediaItem(MediaItem.fromUri(src)); player.prepare(); player.playWhenReady = true }
            DisposableEffect(Unit) { onDispose { player.release() } }
            AndroidView(modifier = modifier, factory = { PlayerView(it).apply {
                this.player = player; useController = false; isFocusable = false } })
        }
        else -> Text("Set a source", modifier = modifier)
    }
}
