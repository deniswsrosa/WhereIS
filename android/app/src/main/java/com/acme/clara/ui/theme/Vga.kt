package com.acme.clara.ui.theme

import androidx.compose.ui.graphics.Color

/** The 16-colour EGA/VGA text palette — the actual DOS colours. */
object Vga {
    val Black = Color(0xFF000000)
    val Blue = Color(0xFF0000AA)
    val Green = Color(0xFF00AA00)
    val Cyan = Color(0xFF00AAAA)
    val Red = Color(0xFFAA0000)
    val Magenta = Color(0xFFAA00AA)
    val Brown = Color(0xFFAA5500)
    val LightGray = Color(0xFFAAAAAA)
    val DarkGray = Color(0xFF555555)
    val LightBlue = Color(0xFF5555FF)
    val LightGreen = Color(0xFF55FF55)
    val LightCyan = Color(0xFF55FFFF)
    val LightRed = Color(0xFFFF5555)
    val LightMagenta = Color(0xFFFF55FF)
    val Yellow = Color(0xFFFFFF55)
    val White = Color(0xFFFFFFFF)

    // Accessible semantic choices for text on the two recurring dark surfaces. Keep the original
    // 16-colour palette, but avoid AA0000 red on black (2.71:1) and white on green (3.11:1).
    val DangerOnDark = LightRed       // 6.68:1 on Black
    val TextOnGreen = Black           // 6.75:1 on Green
}
