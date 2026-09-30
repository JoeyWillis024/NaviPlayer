package com.antiwilly.naviplayer.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

object PaletteExtractor {

    data class ExtractedColors(
        val dominantColor: Color = Color(0xFF1E1E1E),
        val vibrantColor: Color = Color(0xFF1DB954),
        val onDominantColor: Color = Color.White
    )

    fun extractColors(bitmap: Bitmap?): ExtractedColors {
        if (bitmap == null) return ExtractedColors()
        val palette = Palette.from(bitmap).generate()

        val dominant = palette.getDominantColor(0xFF1E1E1E.toInt())
        val vibrant = palette.getVibrantColor(
            palette.getLightVibrantColor(
                palette.getDarkVibrantColor(dominant)
            )
        )

        return ExtractedColors(
            dominantColor = Color(dominant),
            vibrantColor = Color(vibrant),
            onDominantColor = if (isColorDark(dominant)) Color.White else Color.Black
        )
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * android.graphics.Color.red(color) +
                0.587 * android.graphics.Color.green(color) +
                0.114 * android.graphics.Color.blue(color)) / 255
        return darkness >= 0.5
    }
}
