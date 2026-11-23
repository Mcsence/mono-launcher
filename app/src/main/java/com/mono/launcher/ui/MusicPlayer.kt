package com.mono.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.MediaInfo
import com.mono.launcher.NotificationState

private enum class MediaIcon { PREVIOUS, PLAY, PAUSE, NEXT }

/** Now-playing card: cover, title, artist and previous / play-pause / next. */
@Composable
fun MusicPlayer(media: MediaInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Raised)
            .clickable {
                // Tapping the card opens the music app.
                context.packageManager.getLaunchIntentForPackage(media.packageName)?.let {
                    runCatching { context.startActivity(it) }
                }
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val art = media.art
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Black),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(8.dp).background(Accent, CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                media.title.ifBlank { "Lecture en cours" },
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (media.artist.isNotBlank()) {
                Text(
                    media.artist,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MediaButton(MediaIcon.PREVIOUS, filled = false) { NotificationState.previous() }
        MediaButton(if (media.playing) MediaIcon.PAUSE else MediaIcon.PLAY, filled = true) {
            NotificationState.playPause()
        }
        MediaButton(MediaIcon.NEXT, filled = false) { NotificationState.next() }
    }
}

@Composable
private fun MediaButton(icon: MediaIcon, filled: Boolean, onClick: () -> Unit) {
    val accent = Accent
    Box(
        Modifier
            .padding(start = 4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(if (filled) accent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            drawMediaIcon(icon, if (filled) Black else White)
        }
    }
}

private fun DrawScope.drawMediaIcon(icon: MediaIcon, color: Color) {
    val w = size.width
    val h = size.height
    fun triangle(x0: Float, x1: Float) = Path().apply {
        moveTo(x0, 0f)
        lineTo(x1, h / 2)
        lineTo(x0, h)
        close()
    }
    when (icon) {
        MediaIcon.PLAY -> drawPath(triangle(w * 0.1f, w), color)
        MediaIcon.PAUSE -> {
            drawRect(color, Offset(w * 0.1f, 0f), Size(w * 0.28f, h))
            drawRect(color, Offset(w * 0.62f, 0f), Size(w * 0.28f, h))
        }
        MediaIcon.NEXT -> {
            drawPath(triangle(0f, w * 0.75f), color)
            drawRect(color, Offset(w * 0.8f, 0f), Size(w * 0.2f, h))
        }
        MediaIcon.PREVIOUS -> {
            drawPath(triangle(w, w * 0.25f), color)
            drawRect(color, Offset(0f, 0f), Size(w * 0.2f, h))
        }
    }
}
