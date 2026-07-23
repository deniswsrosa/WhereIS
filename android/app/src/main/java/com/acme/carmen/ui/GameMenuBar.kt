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
import androidx.compose.material.Text
import androidx.compose.ui.unit.dp
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
        // Dossiers menu lists the ten suspects directly, like the original
        MenuTitle(v, "Dossiers", GameData.suspects.map { su ->
            MenuItemDef(su.name.replace("\"", "")) { vm.openOverlay(Overlay.Dossier(su)) }
        })
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
        // DOS-style dropdown: white box, black border, tight monospace items that
        // highlight to a black bar (inverse video) when pressed — like the original menus.
        DropdownMenu(expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.background(Vga.White).border(BorderStroke(1.5.dp, Vga.Black))) {
            items.forEach { def ->
                var pressed by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth()
                    .background(if (pressed) Vga.Black else Vga.White)
                    .clickable { pressed = true; open = false; def.action() }
                    .padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Text(def.label, fontFamily = FontFamily.Monospace,
                        color = if (pressed) Vga.White else Vga.Black,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

/** Renders the active menu overlay as a centred VGA dialog. Returns nothing if none. */
@Composable
fun OverlayHost(v: Virtual, vm: CarmenViewModel) {
    val o = vm.s.overlay ?: return
    if (o is Overlay.Dossier) { DossierWindow(v, o.suspect) { vm.dismissOverlay() }; return }
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
        is Overlay.Dossier -> "" to emptyList()   // handled by DossierWindow above
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

/* Suspect Dossier — the authentic white window from the original's Dossiers menu:
 * a name tab on top, the framed portrait upper-left, bold black field labels beside it,
 * Feature/Other full-width below, and the text typing on progressively. Geometry measured
 * from DOSBox captures: window (25,49)-(306,189), portrait interior 61x80 at (32,59). */
@Composable
private fun DossierWindow(v: Virtual, su: Suspect, onClose: () -> Unit) {
    // typed-on effect: number of characters shown across all fields
    val fields = listOf(
        "Name:" to su.name, "Sex:" to su.sex, "Occupation:" to su.occupation,
        "Hobby:" to su.hobby, "Hair Color:" to su.hair, "Auto:" to su.auto,
        "Feature:" to su.feature1, "Other:" to su.feature2,
    )
    val total = fields.sumOf { it.second.length }
    var shown by remember(su.name) { mutableStateOf(0) }
    LaunchedEffect(su.name) { shown = 0; while (shown < total) { kotlinx.coroutines.delay(8); shown += 2 } }
    fun taken(idx: Int): String {
        var budget = shown
        for (i in 0 until idx) budget -= fields[i].second.length
        return fields[idx].second.take(budget.coerceAtLeast(0))
    }
    val label = v.text(8, color = Vga.Black, bold = true)
    val value = v.text(8, color = Vga.Black)

    Box(Modifier.fillMaxSize().clickable { onClose() }) {
        // drop shadow, then the window
        v.At(28, 52, 281, 140) { Box(Modifier.fillMaxSize().background(Vga.Black)) }
        v.At(25, 49, 281, 140) {
            Box(Modifier.fillMaxSize().background(Vga.White).border(BorderStroke(v.w(1), Vga.Black))) {
                Row(Modifier.fillMaxSize().padding(v.w(4))) {
                    // framed portrait: black outer border, white gap, black inner border
                    Column {
                        Box(Modifier.border(BorderStroke(v.w(1), Vga.Black)).padding(v.w(2))) {
                            Box(Modifier.border(BorderStroke(v.w(1), Vga.Black))) {
                                val slug = "suspect_" + su.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                if (drawableId(slug) != 0)
                                    PixelImage(slug, Modifier.size(v.w(61), v.w(80)))
                                else Box(Modifier.size(v.w(61), v.w(80)).background(Vga.DarkGray))
                            }
                        }
                    }
                    Spacer(Modifier.width(v.w(5)))
                    Column(Modifier.weight(1f)) {
                        Row { Text("Name: ", style = label); Text(taken(0), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Row { Text("Sex: ", style = label); Text(taken(1), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Occupation: ") }
                            append(taken(2))
                        }, style = value)
                        Spacer(Modifier.height(v.w(4)))
                        Row { Text("Hobby: ", style = label); Text(taken(3), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Row { Text("Hair Color: ", style = label); Text(taken(4), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Row { Text("Auto: ", style = label); Text(taken(5), style = value) }
                    }
                }
                // Feature / Other span the full window width beneath the portrait
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(start = v.w(4), end = v.w(4), bottom = v.w(4))) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Feature: ") }
                        append(taken(6))
                    }, style = value)
                    Spacer(Modifier.height(v.w(4)))
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Other: ") }
                        append(taken(7))
                    }, style = value)
                }
            }
        }
        // name tab overlapping the window's top edge
        v.At(103, 43, 170, 11) {
            Box(Modifier.background(Vga.White).border(BorderStroke(v.w(1), Vga.Black))
                .padding(horizontal = v.w(4)), contentAlignment = Alignment.Center) {
                Text(su.name.replace("\"", "").uppercase(), style = v.text(7.5f, color = Vga.Black, bold = true))
            }
        }
    }
}
