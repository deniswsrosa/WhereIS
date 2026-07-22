package com.acme.carmen.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.acme.carmen.data.Suspect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.acme.carmen.data.GameData
import com.acme.carmen.game.CarmenViewModel
import com.acme.carmen.game.Overlay
import com.acme.carmen.ui.theme.Vga

private data class MenuItemDef(val label: String, val action: () -> Unit)

@Composable
fun GameMenuBar(v: Virtual, vm: CarmenViewModel) {
    val s = vm.s
    Row(
        Modifier.fillMaxWidth().height(v.w(11)).background(Vga.LightGray)
            .padding(horizontal = v.w(3)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(v.w(6))
    ) {
        MenuTitle(v, "Game", listOf(
            MenuItemDef("About Carmen...") { vm.openOverlay(Overlay.About) },
            MenuItemDef("New Case") { vm.menuNewCase() },
            MenuItemDef("Quit to Title") { vm.menuQuitToTitle() },
        ))
        MenuTitle(v, "Options", listOf(
            MenuItemDef(if (s.soundOn) "Sound: On" else "Sound: Off") { vm.toggleSound() },
            MenuItemDef("Joystick") { vm.showJoystick() },
        ))
        MenuTitle(v, "Acme", listOf(
            MenuItemDef("Detective Roster") { vm.openOverlay(Overlay.Roster) },
            MenuItemDef("Hall of Fame") { vm.openOverlay(Overlay.HallOfFame) },
        ))
        MenuTitle(v, "Dossiers", listOf(
            MenuItemDef("Suspect Dossiers") { vm.openOverlay(Overlay.Dossiers) },
        ))
    }
}

@Composable
private fun MenuTitle(v: Virtual, title: String, items: List<MenuItemDef>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Vga.Red, textDecoration = TextDecoration.Underline)) { append(title.first()) }
                append(title.drop(1))
            },
            style = v.text(8, color = Vga.Black, bold = true),
            modifier = Modifier.clickable { open = true }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.background(Vga.LightGray)) {
            items.forEach { it ->
                DropdownMenuItem(onClick = { open = false; it.action() }) {
                    Text(it.label, fontFamily = FontFamily.Monospace, color = Vga.Black,
                        fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }
        }
    }
}

/** Renders the active menu overlay as a centred VGA dialog. Returns nothing if none. */
@Composable
fun OverlayHost(v: Virtual, vm: CarmenViewModel) {
    val o = vm.s.overlay ?: return
    if (o is Overlay.Dossiers) { DossiersOverlay(v, vm); return }
    val (title, lines) = when (o) {
        Overlay.About -> "ABOUT" to listOf(
            "Where in the World is", "Carmen Sandiego?  (Enhanced)", "MS-DOS Version 2.1",
            "Copyright 1990 Broderbund", "", "Design: Gene Portwood &", "  Lauren Elliott",
            "Programming: Glenn Axworthy", "Music & Sound: Tom Rettig",
        )
        Overlay.Roster -> "ACME DETECTIVE ROSTER" to listOf(
            "Detective: ${vm.s.detectiveName}", "Rank: ${GameData.ranks[vm.s.rankIndex]}",
            "Cases solved: ${vm.s.casesSolved}",
        )
        Overlay.HallOfFame -> "HALL OF FAME" to if (vm.s.casesSolved == 0)
            listOf("The Hall of Fame is empty.")
        else listOf("${vm.s.detectiveName}", "${GameData.ranks[vm.s.rankIndex]} — ${vm.s.casesSolved} case(s)")
        Overlay.Dossiers -> "" to emptyList()   // handled by DossiersOverlay above
        is Overlay.Info -> o.title to o.lines
    }

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.86f)
                .background(Vga.Blue)
                .padding(v.w(6)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = v.text(10, color = Vga.Yellow, bold = true), textAlign = TextAlign.Center)
            Spacer(Modifier.height(v.w(4)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                lines.forEach {
                    Text(it, style = v.text(7.5f, color = Vga.White), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = v.w(0.6f)))
                }
            }
            Spacer(Modifier.height(v.w(4)))
            DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

/* Suspect Dossiers — per-suspect portrait + full stats, matching the original (web_05). */
@Composable
private fun DossiersOverlay(v: Virtual, vm: CarmenViewModel) {
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.94f).background(Vga.Blue)
            .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(4)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SUSPECT DOSSIERS", style = v.text(10, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(2)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                GameData.suspects.forEach { su -> SuspectDossierCard(v, su) }
            }
            Spacer(Modifier.height(v.w(2)))
            DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White, style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

@Composable
private fun SuspectDossierCard(v: Virtual, su: Suspect) {
    Row(Modifier.fillMaxWidth().padding(vertical = v.w(1)).background(Vga.Black)
        .border(BorderStroke(v.w(1), Vga.DarkGray)).padding(v.w(2))) {
        Canvas(Modifier.size(v.w(34), v.w(40))) { drawSuspectBust(size.width, size.height, su) }
        Spacer(Modifier.width(v.w(3)))
        Column(Modifier.weight(1f)) {
            Text(su.name, style = v.text(8.5f, color = Vga.Yellow, bold = true))
            Text("Sex: ${su.sex}    Hair: ${su.hair}", style = v.text(6.5f, color = Vga.White))
            Text("Occupation: ${su.occupation}", style = v.text(6.5f, color = Vga.White))
            Text("Hobby: ${su.hobby}", style = v.text(6.5f, color = Vga.White))
            Text("Auto: ${su.auto}", style = v.text(6.5f, color = Vga.White))
            Text("Feature: ${su.feature1}", style = v.text(6.5f, color = Vga.LightCyan))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSuspectBust(w: Float, h: Float, su: Suspect) {
    val hl = su.hair.lowercase()
    val hair = when {
        "red" in hl && "brown" in hl -> Color(0xFFA0522D)
        "red" in hl -> Vga.LightRed
        "blond" in hl -> Vga.Yellow
        "black" in hl || "raven" in hl -> Vga.Black
        "brun" in hl || "brown" in hl -> Vga.Brown
        else -> Vga.DarkGray
    }
    val skin = Color(0xFFF0C8A0)
    val shirt = if (su.sex == "Female") Vga.Magenta else Vga.Blue
    drawRoundRect(shirt, topLeft = Offset(w * 0.08f, h * 0.62f), size = Size(w * 0.84f, h * 0.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f))
    drawRect(skin, topLeft = Offset(w * 0.42f, h * 0.52f), size = Size(w * 0.16f, h * 0.14f))
    drawOval(skin, topLeft = Offset(w * 0.24f, h * 0.16f), size = Size(w * 0.52f, h * 0.42f))
    drawArc(hair, 180f, 180f, true, topLeft = Offset(w * 0.22f, h * 0.10f), size = Size(w * 0.56f, h * 0.34f))
    drawRect(Vga.Black, topLeft = Offset(w * 0.38f, h * 0.32f), size = Size(w * 0.05f, h * 0.04f))
    drawRect(Vga.Black, topLeft = Offset(w * 0.55f, h * 0.32f), size = Size(w * 0.05f, h * 0.04f))
}
