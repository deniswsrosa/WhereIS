package com.acme.clara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import com.acme.clara.game.Tutorial
import com.acme.clara.ui.theme.Vga

/**
 * The Rookie tutorial coach-mark: a yellow tip card for the current step. The player reads it,
 * taps GOT IT, then performs the action — which advances the step and brings up the next tip.
 * SKIP dismisses the whole tutorial. Only shows during gameplay phases.
 */
@Composable
fun TutorialCoach(vm: ClaraViewModel) {
    val step = vm.s.tutorialStep
    val message = Tutorial.message(step) ?: return
    if (vm.s.phase !in gameplayPhases) return

    var acked by remember { mutableStateOf(-1) }
    if (acked == step) return   // dismissed for this step — the player is doing the action

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.82f).background(Vga.Yellow).border(BorderStroke(2.dp, Vga.Black))
                .padding(16.dp),
        ) {
            Text(
                "TUTORIAL   ${step + 1} / ${Tutorial.STEPS}",
                color = Vga.Red, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            )
            Text(
                message,
                color = Vga.Black, fontFamily = FontFamily.Monospace, fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.background(Vga.LightGray).border(BorderStroke(2.dp, Vga.Black))
                        .clickable { vm.skipTutorial() }.tappable("Skip tutorial")
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text("SKIP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Vga.Black, fontSize = 13.sp) }
                Box(
                    Modifier.background(Vga.Green).border(BorderStroke(2.dp, Vga.Black))
                        .clickable { acked = step }.tappable("Got it")
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text("GOT IT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Vga.White, fontSize = 13.sp) }
            }
        }
    }
}

private val gameplayPhases = setOf(Phase.BRIEFING, Phase.CITY, Phase.CRIME)
