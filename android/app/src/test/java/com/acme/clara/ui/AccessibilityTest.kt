package com.acme.clara.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.acme.clara.ui.theme.Vga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AccessibilityTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val first = luminance(a)
        val second = luminance(b)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    @Test fun recurringTextPairsMeetWcagAa() {
        val pairs = mapOf(
            "body on black" to (Vga.White to Vga.Black),
            "muted on black" to (Vga.LightGray to Vga.Black),
            "accent on black" to (Vga.Yellow to Vga.Black),
            "danger on black" to (Vga.DangerOnDark to Vga.Black),
            "dialog on blue" to (Vga.White to Vga.Blue),
            "paper copy" to (Vga.Black to Vga.White),
            "yellow action" to (Vga.Black to Vga.Yellow),
            "green action" to (Vga.TextOnGreen to Vga.Green),
        )
        for ((name, pair) in pairs) {
            assertTrue("$name contrast was ${contrast(pair.first, pair.second)}",
                contrast(pair.first, pair.second) >= 4.5)
        }
    }

    @Test fun canvasTextScaleHonoursPreferenceWithinSafeLayoutCap() {
        assertEquals(1f, boundedCanvasTextScale(0.85f), 0.001f)
        assertEquals(1f, boundedCanvasTextScale(1f), 0.001f)
        assertTrue(boundedCanvasTextScale(1.15f) > 1f)
        assertEquals(1.10f, boundedCanvasTextScale(2f), 0.001f)

        val v = Virtual(1.dp, Density(density = 1f, fontScale = 2f), boundedCanvasTextScale(2f))
        assertTrue(v.readingText(8).fontSize.value > v.text(8).fontSize.value)
    }
}
