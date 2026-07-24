package com.acme.clara.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.acme.clara.ui.theme.Vga

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

/**
 * The whole screen: black letterbox with a centred 320x200 canvas.
 *
 * @param keepVirtualYAboveIme when the soft keyboard is open, the canvas is panned upward
 *   (at full size — never scaled down) just enough to keep this virtual-Y line above the
 *   keyboard. Null (default) leaves the canvas centred. Used by the HQ printer so the paper
 *   stays visible while you type your name, instead of the scene collapsing into a tiny box.
 */
@Composable
fun VirtualScreen(
    keepVirtualYAboveIme: Float? = null,
    content: @Composable BoxScope.(Virtual) -> Unit,
) {
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

        // Pan the canvas up so `keepVirtualYAboveIme` clears the keyboard (no scaling).
        var shift: Dp = 0.dp
        if (keepVirtualYAboveIme != null) {
            val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
            if (imeBottomPx > 0f) with(density) {
                val screenHpx = maxHeight.toPx()
                val canvasHpx = (unit * 200f).toPx()
                val canvasTopPx = (screenHpx - canvasHpx) / 2f            // vertical-centre offset
                val unitPx = unit.toPx()
                val margin = unitPx * 6f
                val keepBottomPx = canvasTopPx + keepVirtualYAboveIme * unitPx
                val kbTopPx = screenHpx - imeBottomPx
                val overlapPx = keepBottomPx + margin - kbTopPx
                if (overlapPx > 0f) shift = -overlapPx.toDp()
            }
        }
        Box(Modifier.offset(y = shift).size(unit * 320f, unit * 200f).background(Vga.Black)) { content(v) }
    }
}

/** Game sprites live under assets/sprites/<category>/<name>.png — real folders, one per
 *  sprite family (cities, witnesses, suspects, venues, sightings, animations, screens,
 *  intro, ui). Categories are organisational only: names stay globally unique and every
 *  lookup goes through this index, so callers keep using bare names. */
private object Sprites {
    @Volatile private var index: Map<String, String>? = null
    private val bitmaps = HashMap<String, androidx.compose.ui.graphics.ImageBitmap?>()

    private fun indexFor(ctx: android.content.Context): Map<String, String> =
        index ?: synchronized(this) {
            index ?: buildIndex(ctx).also { index = it }
        }

    private fun buildIndex(ctx: android.content.Context): Map<String, String> {
        val m = HashMap<String, String>()
        val am = ctx.assets
        for (dir in am.list("sprites").orEmpty()) {
            for (f in am.list("sprites/$dir").orEmpty()) {
                if (f.endsWith(".png")) m[f.removeSuffix(".png")] = "sprites/$dir/$f"
            }
        }
        return m
    }

    fun exists(ctx: android.content.Context, name: String) = indexFor(ctx).containsKey(name)

    fun bitmap(ctx: android.content.Context, name: String): androidx.compose.ui.graphics.ImageBitmap? =
        synchronized(bitmaps) {
            bitmaps.getOrPut(name) {
                indexFor(ctx)[name]?.let { p ->
                    ctx.assets.open(p).use { s ->
                        android.graphics.BitmapFactory.decodeStream(s)?.asImageBitmap()
                    }
                }
            }
        }
}

/** True if a sprite with this name exists in assets/sprites/<any category>/. */
@Composable
fun spriteExists(name: String): Boolean {
    val ctx = LocalContext.current
    return remember(name) { Sprites.exists(ctx, name) }
}

@Composable
fun PixelImage(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.FillBounds,
               alignment: Alignment = Alignment.Center) {
    val ctx = LocalContext.current
    val bmp = remember(name) { Sprites.bitmap(ctx, name) }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = name, modifier = modifier, contentScale = scale, alignment = alignment,
        )
    } else {
        Box(modifier.background(Vga.Cyan), contentAlignment = Alignment.Center) {
            Text("NO SIGNAL", style = TextStyle(fontFamily = FontFamily.Monospace, color = Vga.Black))
        }
    }
}

/** Aspect ratio (h/w) of a sprite's bitmap, or `fallback` if it is missing.
 *  Used to draw sprites at a fixed virtual width with their native proportions, regardless
 *  of the scale the asset was captured at. */
@Composable
fun drawableAspect(name: String, fallback: Float = 1f): Float {
    val ctx = LocalContext.current
    val bmp = remember(name) { Sprites.bitmap(ctx, name) }
    return if (bmp != null && bmp.width > 0 && bmp.height > 0)
        bmp.height.toFloat() / bmp.width.toFloat() else fallback
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
