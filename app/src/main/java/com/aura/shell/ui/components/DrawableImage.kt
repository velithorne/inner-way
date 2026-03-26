package com.aura.shell.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap

@Composable
fun DrawableImage(
    drawable: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val density = LocalDensity.current
    val painter = remember(drawable, density) {
        val size = (48 * density.density).toInt().coerceAtLeast(1)
        val bitmap = drawable.toBitmap(width = size, height = size)
        BitmapPainter(bitmap.asImageBitmap())
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
