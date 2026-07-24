package com.acme.carmen.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.material.Text
import com.acme.carmen.ui.theme.Vga

/** Scope for laying out inside a virtual 320x200 DOS screen. `unit` = one virtual pixel, in Dp. */
class Virtual(val unit: Dp, val density: Density) {
    fun w(n: Number): Dp = unit * n.toFloat()
    fun sp(n: Number): TextUnit = with(density) { (unit * n.toFloat()).toSp() }
    fun text(px: Number, color: Color = Vga.White, bold: Boolean = false) = TextStyle(
        fontFamily = FontFamily.Monospace, fontSize = sp(px), color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, lineHeight = sp(px.toFloat() * 1.15f)
    )
}

/** A box positioned at virtual (x,y) with virtual (w,h). */
@Composable
fun Virtual.At(x: Number, y: Number, wv: Number, hv: Number,
               align: Alignment = Alignment.TopStart, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.offset(w(x), w(y)).size(w(wv), w(hv)), contentAlignment = align, content = content)
}

/** The whole screen: black letterbox with a centred 320x200 canvas. */
@Composable
fun VirtualScreen(content: @Composable BoxScope.(Virtual) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Vga.Black), contentAlignment = Alignment.Center) {
        val wv = maxWidth.value
        val hv = maxHeight.value
        // Guard against transient 0 / unbounded / NaN constraints (e.g. mid-rotation) that
        // would otherwise produce NaN or Infinity sizes and crash Compose layout.
        if (!wv.isFinite() || !hv.isFinite() || wv <= 0f || hv <= 0f) {
            return@BoxWithConstraints
        }
        val ratio = 320f / 200f
        val fitW: Dp = if (wv / hv > ratio) maxHeight * ratio else maxWidth
        val unit = fitW / 320f
        if (!unit.value.isFinite() || unit.value <= 0f) return@BoxWithConstraints
        val density = LocalDensity.current
        val v = remember(unit) { Virtual(unit, density) }
        Box(Modifier.size(unit * 320f, unit * 200f).background(Vga.Black)) { content(v) }
    }
}

/** Look up a drawable by resource name; returns 0 if absent. */
@Composable
fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return remember(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

@Composable
fun PixelImage(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.FillBounds) {
    val id = drawableId(name)
    if (id != 0) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id),
            contentDescription = name, modifier = modifier, contentScale = scale,
        )
    } else {
        Box(modifier.background(Vga.Cyan), contentAlignment = Alignment.Center) {
            Text("NO SIGNAL", style = TextStyle(fontFamily = FontFamily.Monospace, color = Vga.Black))
        }
    }
}

/** A chunky DOS push-button (used outside the virtual canvas, e.g. dialogs). */
@Composable
fun DosButton(
    label: String, modifier: Modifier = Modifier, fill: Color = Vga.LightGray,
    textColor: Color = Vga.Black, style: TextStyle? = null, enabled: Boolean = true, onClick: () -> Unit,
) {
    Box(
        modifier
            .background(if (enabled) fill else Vga.DarkGray)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = (style ?: TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
            .copy(color = if (enabled) textColor else Vga.LightGray), textAlign = TextAlign.Center)
    }
}
