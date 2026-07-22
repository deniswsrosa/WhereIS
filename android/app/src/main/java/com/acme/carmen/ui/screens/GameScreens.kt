package com.acme.carmen.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.acme.carmen.game.Venue
import kotlinx.coroutines.delay
import com.acme.carmen.data.CityMeta
import com.acme.carmen.data.GameData
import com.acme.carmen.data.Suspect
import com.acme.carmen.game.CarmenViewModel
import com.acme.carmen.game.Overlay
import com.acme.carmen.ui.*
import com.acme.carmen.ui.theme.Vga

/* ----------------------------- TITLE ----------------------------- */
@Composable
fun TitleScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    PixelImage("title_screen", Modifier.fillMaxSize())
    v.At(0, 176, 320, 24, Alignment.Center) {
        Box(Modifier.fillMaxSize().clickable { vm.start() }, contentAlignment = Alignment.Center) {
            Text("PRESS  ANY  KEY  TO  BEGIN", style = v.text(9, color = Vga.Yellow, bold = true))
        }
    }
}

/* ----------------------------- SIGN ON ----------------------------- */
@Composable
fun SignOnScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    var name by remember { mutableStateOf("") }
    PixelImage("hq_screen", Modifier.fillMaxSize())
    // input strip overlaid at the bottom
    v.At(28, 150, 264, 44, Alignment.Center) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(v.w(4))) {
            TextField(
                value = name, onValueChange = { name = it.take(20) }, singleLine = true,
                textStyle = v.text(10, color = Vga.Black),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = TextFieldDefaults.textFieldColors(backgroundColor = Vga.White, cursorColor = Vga.Black),
                modifier = Modifier.weight(1f).height(v.w(26))
            )
            DosButton("SIGN ON", fill = Vga.Green, textColor = Vga.White, style = v.text(9, bold = true)) {
                vm.signOn(name)
            }
        }
    }
}

/* ----------------------------- BRIEFING ----------------------------- */
@Composable
fun BriefingScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    val s = vm.s
    Column(Modifier.fillMaxSize().padding(v.w(10)), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(GameData.FLASH, style = v.text(12, color = Vga.LightRed, bold = true))
        Spacer(Modifier.height(v.w(6)))
        Column(
            Modifier.fillMaxWidth().weight(1f).background(Vga.Blue)
                .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(6)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BText(v, GameData.TREASURE_STOLEN.replace("%s", s.currentCity))
            Spacer(Modifier.height(v.w(4)))
            BText(v, GameData.TREASURE_ID.replace("%s", s.treasure), Vga.Yellow)
            Spacer(Modifier.height(v.w(6)))
            BText(v, "Your assignment: recover the loot and arrest the thief.")
            Spacer(Modifier.height(v.w(3)))
            BText(v, GameData.DEADLINE, Vga.LightCyan)
            Spacer(Modifier.height(v.w(3)))
            BText(v, "Good luck, ${GameData.ranks[s.rankIndex]} ${s.detectiveName}.")
        }
        Spacer(Modifier.height(v.w(6)))
        DosButton("BEGIN INVESTIGATION", fill = Vga.Green, textColor = Vga.White,
            style = v.text(10, bold = true)) { vm.beginInvestigation() }
    }
}

@Composable
private fun BText(v: Virtual, t: String, c: androidx.compose.ui.graphics.Color = Vga.White) =
    Text(t, style = v.text(8, color = c), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

/* ----------------------------- CITY ----------------------------- */
@Composable
private fun CityPhoto(city: String, v: Virtual, modifier: Modifier) {
    val info = CityMeta.of(city)
    if (info.drawable != null) PixelImage(info.drawable, modifier)
    else VgaCityCard(city, info.region, v, modifier)
}

/** Procedural 16-colour VGA "travel postcard" for cities without a captured photo.
 *  Deterministic per city: region-tinted sky, sun/moon, dithered ground, a skyline. */
@Composable
private fun VgaCityCard(city: String, region: String, v: Virtual, modifier: Modifier) {
    val sky = when (region) {
        "Europe" -> Vga.LightBlue
        "Asia" -> Vga.Magenta
        "Africa" -> Vga.Brown
        "South America" -> Vga.Green
        "North America" -> Vga.Cyan
        "Oceania" -> Vga.LightCyan
        "the Middle East" -> Vga.Red
        else -> Vga.Blue
    }
    val ground = when (region) {
        "Africa" -> Vga.Brown; "South America" -> Vga.Green
        "Asia" -> Vga.Blue; else -> Vga.DarkGray
    }
    Box(modifier) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(sky.copy(alpha = 1f), size = androidx.compose.ui.geometry.Size(w, h))
            // horizon / ground
            val horizon = h * 0.62f
            drawRect(ground, topLeft = androidx.compose.ui.geometry.Offset(0f, horizon),
                size = androidx.compose.ui.geometry.Size(w, h - horizon))
            // sun/moon
            drawCircle(Vga.Yellow, radius = w * 0.10f,
                center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.22f))
            // skyline silhouette, deterministic from the city name
            var seed = city.hashCode().toLong() and 0xffffffffL
            fun rnd(): Float { seed = (seed * 1103515245 + 12345) and 0xffffffffL; return (seed ushr 16 and 0x7fff) / 32767f }
            val n = 9
            val bw = w / n
            for (i in 0 until n) {
                val bh = (0.14f + rnd() * 0.34f) * h
                drawRect(Vga.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(i * bw, horizon - bh),
                    size = androidx.compose.ui.geometry.Size(bw * 0.86f, bh))
                // a couple of lit windows
                if (rnd() > 0.4f) drawRect(Vga.Yellow,
                    topLeft = androidx.compose.ui.geometry.Offset(i * bw + bw * 0.3f, horizon - bh * 0.6f),
                    size = androidx.compose.ui.geometry.Size(bw * 0.18f, bh * 0.12f))
            }
        }
        // name plate
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Vga.Black.copy(alpha = 0.72f))
            .padding(vertical = v.w(2)), contentAlignment = Alignment.Center) {
            Text(city, style = v.text(9, color = Vga.White, bold = true), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CityScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    val s = vm.s
    val info = CityMeta.of(s.currentCity)
    var showVenues by remember(s.currentCity, s.progress) { mutableStateOf(false) }

    // menu bar
    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }

    // name / date box (black, white double border)
    v.At(4, 13, 141, 30, Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(2)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.currentCity, style = v.text(9, color = Vga.White, bold = true))
                Text(vm.clockLabel(), style = v.text(8, color = Vga.White))
            }
        }
    }
    // city photo
    v.At(4, 45, 141, 148) {
        Box(Modifier.fillMaxSize().border(BorderStroke(v.w(1), Vga.White))) {
            CityPhoto(s.currentCity, v, Modifier.fillMaxSize())
        }
    }
    // warrant indicator (overlaid on the photo's bottom edge)
    if (s.warrantFor != null) v.At(5, 179, 139, 13, Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
            Text("WARRANT: ${s.warrantFor!!.name}", style = v.text(7, color = Vga.Yellow, bold = true))
        }
    }
    // right panel: normally the city description; while investigating, the witness you're talking to
    if (s.openClue != null) {
        WitnessPanel(v, s.openClue!!) { vm.closeClue() }
    } else {
        v.At(149, 13, 167, 145) {
            Box(Modifier.fillMaxSize().background(Vga.Black)
                .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(4))) {
                Text(if (s.onTrack) info.description
                    else "You look around. Nothing here seems out of the ordinary...",
                    style = v.text(8.5f, color = Vga.White))
            }
        }
    }
    // toolbar: the authentic 4-icon strip (SEE · DEPART · INVESTIGATE · CRIME) below the panel, with tap zones
    v.At(150, 162, 166, 33) {
        Box(Modifier.fillMaxSize()) {
            PixelImage("toolbar_bar", Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize()) {
                ToolZone(Modifier.weight(1f)) { vm.openOverlay(Overlay.Info(s.currentCity.uppercase(), listOf(info.description))) }
                ToolZone(Modifier.weight(1f)) { vm.gotoTravel() }
                ToolZone(Modifier.weight(1f)) { showVenues = true }
                ToolZone(Modifier.weight(1f)) { vm.gotoCrime() }
            }
        }
    }

    // Investigate: pick one of 3 locations, shown as buildings + names (matches web_03)
    if (showVenues && s.openClue == null) {
        InvestigatePicker(v, s.venues, s.visited,
            onPick = { i -> showVenues = false; vm.openVenue(i) },
            onCancel = { showVenues = false })
    }
    OverlayHost(v, vm)
}

@Composable
private fun ToolZone(modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().clickable(onClick = onClick))
}

@Composable
private fun DialogPanel(v: Virtual, border: androidx.compose.ui.graphics.Color = Vga.White,
                        content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.8f).background(Vga.Blue)
                .border(BorderStroke(v.w(1), border)).padding(v.w(6))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, content = content
        )
    }
}

/* ------------------- INVESTIGATION: witness portrait + speech bubble ------------------- */
private data class Look(val hair: Color, val skin: Color, val shirt: Color)
private fun witnessLook(occupation: String): Look {
    val h = occupation.hashCode()
    val hair = listOf(Vga.Yellow, Vga.Brown, Vga.Black, Vga.LightGray, Vga.LightRed)[(h ushr 1).mod(5)]
    val skin = listOf(Color(0xFFF0C8A0), Color(0xFFE0A878), Color(0xFFC89058), Color(0xFF9C6B3F))[(h ushr 4).mod(4)]
    val shirt = listOf(Vga.LightRed, Vga.Cyan, Vga.Green, Vga.LightBlue, Vga.Magenta, Vga.Brown, Vga.LightGreen)[(h ushr 7).mod(7)]
    return Look(hair, skin, shirt)
}

/** Right-panel witness: a head-and-shoulders bust (facing right) + white speech bubble, matching carmen09. */
@Composable
private fun WitnessPanel(v: Virtual, clue: Venue, onDone: () -> Unit) {
    val look = witnessLook(clue.occupation)
    var shown by remember(clue.text) { mutableStateOf(0) }
    LaunchedEffect(clue.text) { shown = 0; while (shown < clue.text.length) { delay(20); shown++ } }
    val bob by rememberInfiniteTransition(label = "wb").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "wb")

    v.At(149, 13, 167, 145) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))
            .clickable { if (shown < clue.text.length) shown = clue.text.length else onDone() }) {
            // witness bust, lower-left, facing the bubble
            Canvas(Modifier.align(Alignment.BottomStart).padding(start = v.w(4), bottom = v.w(14)).size(v.w(50), v.w(58))) {
                drawBust(size.width, size.height, look, (bob - 0.5f) * size.height * 0.03f)
            }
            // occupation label at the bottom
            Text(clue.occupation.uppercase(), style = v.text(7.5f, color = Vga.White, bold = true),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = v.w(3), bottom = v.w(3)))
            // white rounded speech bubble (upper-right) with a tail pointing down toward the witness
            Column(Modifier.align(Alignment.TopEnd).padding(top = v.w(6), end = v.w(4), start = v.w(38)),
                horizontalAlignment = Alignment.Start) {
                Box(Modifier.background(Vga.White, RoundedCornerShape(v.w(5)))
                    .padding(horizontal = v.w(4), vertical = v.w(3))) {
                    Text(clue.text.take(shown), style = v.text(8, color = Vga.Black))
                }
                Canvas(Modifier.padding(start = v.w(6)).size(v.w(9), v.w(7))) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(0f, size.height); close()
                    }
                    drawPath(p, Vga.White)
                }
            }
            if (shown >= clue.text.length) {
                Text("▶", style = v.text(9, color = Vga.LightGreen, bold = true),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(v.w(3)))
            }
        }
    }
}

/** Head-and-shoulders caricature facing right, in DOS-portrait style. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBust(w: Float, h: Float, look: Look, bob: Float) {
    val cx = w * 0.46f
    val shoulderTop = h * 0.66f + bob
    // shoulders (shirt)
    drawRoundRect(look.shirt, topLeft = Offset(w * 0.04f, shoulderTop), size = Size(w * 0.92f, h - shoulderTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f))
    // neck
    drawRect(look.skin, topLeft = Offset(cx - w * 0.08f, shoulderTop - h * 0.10f), size = Size(w * 0.16f, h * 0.14f))
    // head (facing right)
    val headCx = cx + w * 0.05f; val headCy = h * 0.36f + bob
    val hw = w * 0.30f; val hh = h * 0.27f
    drawOval(look.skin, topLeft = Offset(headCx - hw, headCy - hh), size = Size(hw * 2, hh * 2))
    // nose bump on the right
    drawOval(look.skin, topLeft = Offset(headCx + hw * 0.72f, headCy - h * 0.01f), size = Size(w * 0.13f, h * 0.09f))
    // hair sweeping over the top and back-left
    drawArc(look.hair, 175f, 200f, true, topLeft = Offset(headCx - hw * 1.05f, headCy - hh * 1.2f), size = Size(hw * 2.0f, hh * 2.0f))
    drawRect(look.hair, topLeft = Offset(headCx - hw * 1.02f, headCy - hh * 0.3f), size = Size(w * 0.09f, hh * 1.0f))
    // eye
    drawOval(Vga.Black, topLeft = Offset(headCx + hw * 0.18f, headCy - hh * 0.28f), size = Size(w * 0.05f, h * 0.05f))
    // smiling mouth
    drawArc(Vga.Black, 15f, 55f, false, topLeft = Offset(headCx + hw * 0.05f, headCy - hh * 0.1f),
        size = Size(hw * 1.0f, hh * 1.0f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.02f))
}

/** Investigation picker: the 3 locations shown as buildings + names in a popup (matches web_03). */
@Composable
private fun InvestigatePicker(v: Virtual, venues: List<Venue>, visited: Set<Int>, onPick: (Int) -> Unit, onCancel: () -> Unit) {
    v.At(22, 46, 214, 140) {
        Column(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(3))) {
            // 3 building illustrations in a row
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(v.w(4))) {
                venues.forEachIndexed { i, _ ->
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onPick(i) }, contentAlignment = Alignment.BottomCenter) {
                        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                            drawCivicBuilding(size.width, size.height, i, i in visited)
                        }
                    }
                }
            }
            Spacer(Modifier.height(v.w(2)))
            // location names (tap the building or the name to enter)
            venues.forEachIndexed { i, venue ->
                val done = i in visited
                Box(Modifier.fillMaxWidth().clickable { onPick(i) }.padding(vertical = v.w(1)),
                    contentAlignment = Alignment.Center) {
                    Text((if (done) "✓ " else "") + venue.place,
                        style = v.text(9, color = if (done) Vga.LightGray else Vga.White, bold = true),
                        textAlign = TextAlign.Center)
                }
            }
            Box(Modifier.fillMaxWidth().clickable { onCancel() }.padding(top = v.w(1)), contentAlignment = Alignment.Center) {
                Text("Cancel", style = v.text(7, color = Vga.LightRed))
            }
        }
    }
}

private val bldgWalls = listOf(Color(0xFFD09084), Color(0xFFBCA488), Color(0xFFAEB0B8))
private val bldgRoofs = listOf(Vga.DarkGray, Vga.Red, Vga.Brown)
/** A classical civic building (columns + pediment), like the museum/library/bank icons in the game. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCivicBuilding(w: Float, h: Float, idx: Int, visited: Boolean) {
    val wall = if (visited) Vga.DarkGray else bldgWalls[idx % bldgWalls.size]
    val roof = if (visited) Vga.Black else bldgRoofs[idx % bldgRoofs.size]
    val trim = if (visited) Vga.DarkGray else Vga.LightGray
    val facTop = h * 0.42f; val baseY = h * 0.86f
    // steps
    drawRect(trim, topLeft = Offset(w * 0.04f, baseY), size = Size(w * 0.92f, h * 0.14f))
    drawRect(trim, topLeft = Offset(w * 0.16f, baseY - h * 0.05f), size = Size(w * 0.68f, h * 0.05f))
    // facade wall
    drawRect(wall, topLeft = Offset(w * 0.14f, facTop), size = Size(w * 0.72f, baseY - facTop))
    // columns
    val nCol = 4
    for (c in 0 until nCol) {
        val cx = w * 0.19f + c * (w * 0.62f / nCol)
        drawRect(Vga.White, topLeft = Offset(cx, facTop + h * 0.03f), size = Size(w * 0.055f, baseY - facTop - h * 0.05f))
    }
    // door
    drawRect(Vga.Black, topLeft = Offset(w * 0.44f, baseY - h * 0.15f), size = Size(w * 0.12f, h * 0.15f))
    // pediment (triangular roof)
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.08f, facTop); lineTo(w * 0.5f, h * 0.16f); lineTo(w * 0.92f, facTop); close()
    }
    drawPath(p, roof)
    // lit windows in the frieze
    if (!visited) {
        drawRect(Vga.Yellow, topLeft = Offset(w * 0.30f, facTop - h * 0.02f), size = Size(w * 0.06f, h * 0.05f))
        drawRect(Vga.Yellow, topLeft = Offset(w * 0.64f, facTop - h * 0.02f), size = Size(w * 0.06f, h * 0.05f))
    }
}

/* ----------------------------- TRAVEL ----------------------------- */
@Composable
fun TravelScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    val options = remember(vm.s.currentCity, vm.s.progress, vm.s.onTrack) { vm.travelOptions() }
    PixelImage("world_map", Modifier.fillMaxSize())
    // DOS-style destination list on the right (black box, white border, white text rows)
    v.At(150, 12, 166, 182) {
        Column(Modifier.fillMaxSize().background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(4))
            .verticalScroll(rememberScrollState())) {
            Text("DEPART TO:", style = v.text(9, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(3)))
            options.forEach { city ->
                val region = CityMeta.of(city).region
                Box(Modifier.fillMaxWidth().clickable { vm.travelTo(city) }.padding(vertical = v.w(2))) {
                    Text("$city", style = v.text(9, color = Vga.White, bold = true))
                    Text(region, style = v.text(6.5f, color = Vga.LightCyan),
                        modifier = Modifier.align(Alignment.CenterEnd))
                }
            }
            Spacer(Modifier.height(v.w(3)))
            Box(Modifier.fillMaxWidth().clickable { vm.gotoCity() }.padding(vertical = v.w(1))) {
                Text("Cancel", style = v.text(8, color = Vga.LightRed))
            }
        }
    }
}

/* ----------------------------- CRIME COMPUTER ----------------------------- */
private val MonitorBeige = Color(0xFFD8C4A0)
private val MonitorBase = Color(0xFFCBB894)

@Composable
fun CrimeScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    val s = vm.s
    val results = if (s.computed) vm.matches() else emptyList()
    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }
    // computer base peeking out below the monitor
    v.At(56, 150, 208, 46) { Canvas(Modifier.fillMaxSize()) { drawComputerBase(size.width, size.height) } }
    // CRT monitor: beige bezel + dark screen
    v.At(18, 13, 284, 146) {
        Box(Modifier.fillMaxSize().background(MonitorBeige, RoundedCornerShape(v.w(8))).padding(v.w(7))) {
            Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Color(0xFF6B4A2A)))
                .padding(v.w(3))) {
                Row(Modifier.fillMaxSize()) {
                    // attribute selectors
                    Column(Modifier.weight(1.1f).fillMaxHeight()) {
                        Text("CRIME COMPUTER", style = v.text(9, color = Vga.Yellow, bold = true))
                        Spacer(Modifier.height(v.w(2)))
                        CompRow(v, "SEX", s.compSex, GameData.sexes) { vm.setComp("sex", it) }
                        CompRow(v, "HOBBY", s.compHobby, GameData.hobbies) { vm.setComp("hobby", it) }
                        CompRow(v, "HAIR", s.compHair, GameData.hairColors) { vm.setComp("hair", it) }
                        CompRow(v, "FEATURE", s.compFeature, GameData.features) { vm.setComp("feature", it) }
                        CompRow(v, "VEHICLE", s.compVehicle, GameData.vehicles) { vm.setComp("vehicle", it) }
                        Spacer(Modifier.height(v.w(2)))
                        Row(horizontalArrangement = Arrangement.spacedBy(v.w(2))) {
                            DosButton("COMPUTE", fill = Vga.Green, textColor = Vga.White, style = v.text(7, bold = true)) { vm.compute() }
                            DosButton("Clues", fill = Vga.Cyan, textColor = Vga.Black, style = v.text(7)) { vm.autoFillFromClues() }
                            DosButton("Exit", fill = Vga.Brown, textColor = Vga.White, style = v.text(7)) { vm.gotoCity() }
                        }
                    }
                    Spacer(Modifier.width(v.w(3)))
                    // results (right of the CRT)
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        when {
                            !s.computed -> Text("Enter what the witnesses told you, then COMPUTE.",
                                style = v.text(7.5f, color = Vga.LightGreen))
                            !vm.anyFilterSet() -> Text("No clues entered yet.", style = v.text(7.5f, color = Vga.LightGreen))
                            results.isEmpty() -> Text(GameData.ELIMINATES_ALL, style = v.text(7.5f, color = Vga.LightRed))
                            else -> {
                                Text("${results.size} suspect(s) match:", style = v.text(7.5f, color = Vga.Yellow, bold = true))
                                Spacer(Modifier.height(v.w(2)))
                                results.forEach { su -> SuspectCard(v, su, results.size == 1) { vm.issueWarrant(su) } }
                            }
                        }
                    }
                }
            }
        }
    }
    OverlayHost(v, vm)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComputerBase(w: Float, h: Float) {
    val topH = h * 0.28f
    // stand/neck under the monitor
    drawRect(MonitorBase, topLeft = Offset(w * 0.40f, 0f), size = Size(w * 0.20f, topH))
    // base box
    drawRoundRect(MonitorBase, topLeft = Offset(0f, topH), size = Size(w, h - topH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f))
    val bh = h - topH
    // vents (left)
    for (i in 0..6) drawRect(Color(0xFF8A7250), topLeft = Offset(w * 0.06f + i * w * 0.018f, topH + bh * 0.22f),
        size = Size(w * 0.007f, bh * 0.5f))
    // floppy-drive slot
    drawRect(Vga.Black, topLeft = Offset(w * 0.52f, topH + bh * 0.24f), size = Size(w * 0.34f, bh * 0.2f))
    drawRect(Color(0xFF3050A0), topLeft = Offset(w * 0.55f, topH + bh * 0.28f), size = Size(w * 0.28f, bh * 0.1f))
}

@Composable
private fun CompRow(v: Virtual, label: String, value: String?, options: List<String>, onSet: (String?) -> Unit) {
    val cycle = listOf<String?>(null) + options
    Row(Modifier.fillMaxWidth().padding(vertical = v.w(1)), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", style = v.text(7.5f, color = Vga.Yellow, bold = true), modifier = Modifier.width(v.w(46)))
        DosButton(value ?: "— any —", Modifier.weight(1f),
            fill = if (value == null) Vga.DarkGray else Vga.LightGreen,
            textColor = if (value == null) Vga.LightGray else Vga.Black, style = v.text(8)) {
            val idx = cycle.indexOf(value)
            onSet(cycle[(idx + 1) % cycle.size])
        }
    }
}

@Composable
private fun SuspectCard(v: Virtual, su: Suspect, canWarrant: Boolean, onWarrant: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = v.w(1.5f))
        .background(Vga.Black).border(BorderStroke(v.w(1), Vga.DarkGray)).padding(v.w(2))) {
        Text(su.name, style = v.text(8, color = Vga.Yellow, bold = true))
        Text("${su.sex} · ${su.hobby} · ${su.hair}", style = v.text(7, color = Vga.White))
        Text(su.auto, style = v.text(7, color = Vga.LightGray))
        if (canWarrant) {
            Spacer(Modifier.height(v.w(2)))
            DosButton("ISSUE ARREST WARRANT", fill = Vga.Red, textColor = Vga.White, style = v.text(8, bold = true)) { onWarrant() }
        }
    }
}

/* ----------------------------- RESULT ----------------------------- */
@Composable
fun ResultScreen(vm: CarmenViewModel) = VirtualScreen { v ->
    val s = vm.s
    Column(Modifier.fillMaxSize().background(if (s.won) Vga.Blue else Vga.Black).padding(v.w(12)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (s.won) "CASE CLOSED" else "CASE FAILED",
            style = v.text(14, color = if (s.won) Vga.LightGreen else Vga.LightRed, bold = true))
        Spacer(Modifier.height(v.w(6)))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            s.resultLines.forEach {
                Text(it, style = v.text(8.5f, color = Vga.White), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = v.w(1.5f)))
            }
            Spacer(Modifier.height(v.w(4)))
            Text("Rank: ${GameData.ranks[s.rankIndex]}    Cases solved: ${s.casesSolved}",
                style = v.text(8, color = Vga.LightCyan))
        }
        Spacer(Modifier.height(v.w(4)))
        DosButton("NEXT CASE", fill = Vga.Green, textColor = Vga.White, style = v.text(10, bold = true)) {
            vm.toBriefingForNext()
        }
    }
}
