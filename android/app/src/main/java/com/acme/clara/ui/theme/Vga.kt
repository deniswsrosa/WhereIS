package com.acme.clara.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
}

/** DOS-flavoured monospace text styles. */
object DosType {
    private val fam = FontFamily.Monospace
    val body = TextStyle(fontFamily = fam, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Vga.White)
    val small = TextStyle(fontFamily = fam, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Vga.White)
    val heading = TextStyle(fontFamily = fam, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Vga.Yellow)
    val menu = TextStyle(fontFamily = fam, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Vga.LightGray)
}

/** A beveled DOS panel: light top/left, dark bottom/right feel via a double border. */
@Composable
fun DosPanel(
    modifier: Modifier = Modifier,
    fill: Color = Vga.Blue,
    border: Color = Vga.White,
    padding: PaddingValues = PaddingValues(10.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .background(fill, RectangleShape)
            .border(BorderStroke(2.dp, border), RectangleShape)
            .padding(padding)
    ) { content() }
}
