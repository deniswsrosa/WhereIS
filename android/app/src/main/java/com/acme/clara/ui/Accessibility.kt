package com.acme.clara.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.SoundCue
import com.acme.clara.ui.theme.Vga
import kotlinx.coroutines.delay

/**
 * Small accessibility helpers shared across the UI:
 *  - reduced-motion detection (honour the system "remove animations" setting),
 *  - a 48dp minimum touch target + screen-reader label for controls,
 *  - a spoken/printed caption for each audio cue (for deaf / hard-of-hearing players).
 */

/** True when the user has turned animations off system-wide (animator duration scale = 0). */
fun isReducedMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun reducedMotion(): Boolean = isReducedMotion(LocalContext.current)

/** A screen-reader label only — safe on the fixed pixel canvas, where enlarging would break layout. */
fun Modifier.labelled(label: String): Modifier = semantics { contentDescription = label }

/** A 48dp minimum touch target plus a label — for dialog / picker controls off the strict canvas. */
fun Modifier.tappable(label: String): Modifier =
    this.minimumInteractiveComponentSize().semantics { contentDescription = label }

/** A short caption for an audio cue, shown when Options ▸ Captions is on. Null = no caption. */
fun captionFor(cue: SoundCue): String? = when (cue) {
    SoundCue.CLUE -> "New clue"
    SoundCue.DANGER -> "Danger — a warning"
    SoundCue.WARRANT -> "Warrant issued"
    SoundCue.FLASH -> "Crime computer"
    SoundCue.TRAVEL -> "Departing"
    SoundCue.ARRIVE -> "Arrived"
    SoundCue.CHASE -> "The chase is on"
    SoundCue.WIN -> "Case solved"
    SoundCue.WRONG_ARREST -> "The thief got away"
    SoundCue.OUT_OF_TIME -> "Out of time"
    SoundCue.BRIEFING -> null
}

/** A brief on-screen caption for each audio cue, shown only when Options ▸ Captions is on. */
@Composable
fun CaptionOverlay(vm: ClaraViewModel) {
    var caption by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(vm.soundCue) {
        val cue = vm.soundCue ?: return@LaunchedEffect
        if (!vm.s.captionsOn) return@LaunchedEffect
        caption = captionFor(cue.second) ?: return@LaunchedEffect
        delay(1600)
        caption = null
    }
    caption?.let { cap ->
        Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
            Text(
                "♪ $cap",
                color = Vga.Yellow, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier
                    .background(Vga.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}
