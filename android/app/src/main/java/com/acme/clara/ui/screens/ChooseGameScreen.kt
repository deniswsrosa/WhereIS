package com.acme.clara.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.Text
import com.acme.clara.data.GameData
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.ui.theme.Vga

/**
 * The "Choose a game" picker — shown at launch only when two or more careers exist.
 * Each row resumes a detective; the ✕ deletes one; the last row starts a new career.
 */
@Composable
fun ChooseGameScreen(vm: ClaraViewModel) {
    var refresh by remember { mutableStateOf(0) }
    val games = remember(refresh) { vm.savedGames() }

    Column(
        Modifier.fillMaxSize().background(Vga.Black).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "CHOOSE A DETECTIVE",
            color = Vga.Yellow, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 18.sp,
        )
        Spacer(Modifier.height(16.dp))

        games.forEach { m ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.resumeById(m.id) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(m.name, color = Vga.White, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                    Text(
                        "${GameData.ranks.getOrElse(m.rankIndex) { "Rookie" }} · ${m.casesSolved} solved",
                        color = Vga.Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    )
                }
                Text(
                    "✕",
                    color = Vga.Red, fontFamily = FontFamily.Monospace, fontSize = 16.sp,
                    modifier = Modifier.clickable { vm.deleteGame(m.id); refresh++ }.padding(horizontal = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "— New Game —",
            color = Vga.LightGreen, fontFamily = FontFamily.Monospace, fontSize = 15.sp,
            modifier = Modifier.clickable { vm.newGameFlow() },
        )
    }
}
