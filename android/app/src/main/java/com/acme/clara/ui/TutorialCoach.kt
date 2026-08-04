package com.acme.clara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import com.acme.clara.game.Tutorial
import com.acme.clara.ui.theme.Vga

/**
 * The Rookie tutorial coach-mark: a framed "case file" tip card for the current step. The player
 * reads it, taps GOT IT, then performs the action — which advances the step and brings up the next
 * tip. SKIP dismisses the whole tutorial. Only shows during gameplay phases.
 */
@Composable
fun TutorialCoach(vm: ClaraViewModel) {
    // Hoisted above every early return so its slot is stable: otherwise flying (a non-gameplay
    // TRAVEL phase makes this composable bail before the remember) re-initialises `acked` on the
    // way back, and an already-dismissed tip pops up again at the next city.
    var acked by remember { mutableStateOf(-1) }

    val step = vm.s.tutorialStep
    val message = Tutorial.message(step) ?: return
    if (vm.s.phase !in gameplayPhases) return
    if (acked == step) return   // dismissed for this step — the player is doing the action

    val mono = FontFamily.Monospace

    Box(
        Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        // The card: dark VGA panel with a crisp white frame and a thin yellow inner rule, so it
        // reads as an in-world dispatch rather than a generic dialog.
        Column(
            Modifier.fillMaxWidth(0.86f)
                .border(BorderStroke(2.dp, Vga.White))
                .background(Vga.Black)
                .padding(3.dp)
                .border(BorderStroke(1.dp, Vga.Yellow))
                .background(Vga.Black),
        ) {
            // ---- header bar: label + live step progress dots ----
            Row(
                Modifier.fillMaxWidth().background(Vga.Yellow).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "DETECTIVE  TRAINING",
                    color = Vga.Black, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until Tutorial.STEPS) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(if (i <= step) Vga.Red else Vga.Black.copy(alpha = 0.25f))
                                .border(BorderStroke(1.dp, Vga.Black), CircleShape),
                        )
                    }
                }
            }

            // ---- body: step number eyebrow + the tip text ----
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "STEP ${step + 1} OF ${Tutorial.STEPS}",
                    color = Vga.Yellow, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    color = Vga.White, fontFamily = mono, fontSize = 15.sp, lineHeight = 21.sp,
                )
            }

            // ---- footer: SKIP (ghost) + GOT IT (primary) ----
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostButton("SKIP", Modifier.weight(1f)) { vm.skipTutorial() }
                PrimaryButton("GOT IT", Modifier.weight(1f)) { acked = step }
            }
        }
    }
}

/** Subdued outlined action — visually recedes so it never competes with the primary. */
@Composable
private fun GhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .border(BorderStroke(1.dp, Vga.LightGray))
            .clickable(onClick = onClick).tappable("Skip tutorial")
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Vga.LightGray, fontSize = 13.sp)
    }
}

/** Primary action styled like the game's DOS button: yellow fill, black frame, a hard drop
 *  shadow for a pressed-in look, and bold red label. */
@Composable
private fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier) {
        // hard shadow slab behind the button face
        Box(
            Modifier.fillMaxWidth().height(38.dp)
                .padding(start = 2.dp, top = 2.dp)
                .background(Vga.Black),
        )
        Box(
            Modifier.fillMaxWidth().height(38.dp)
                .padding(end = 2.dp, bottom = 2.dp)
                .background(Vga.Yellow)
                .border(BorderStroke(2.dp, Vga.Black))
                .clickable(onClick = onClick).tappable("Got it"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                color = Vga.Red, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

private val gameplayPhases = setOf(Phase.BRIEFING, Phase.CITY, Phase.CRIME)
