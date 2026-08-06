package com.acme.clara.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.acme.clara.data.CountryShapes
import com.acme.clara.data.WorldMap
import com.acme.clara.data.Suspect
import com.acme.clara.game.SpacedRepetition
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import com.acme.clara.data.GameData
import com.acme.clara.game.Achievements
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.MostWanted
import com.acme.clara.game.Overlay
import com.acme.clara.game.WantedEntry
import com.acme.clara.i18n.Strings
import com.acme.clara.ui.theme.Vga

private data class MenuItemDef(val label: String, val enabled: Boolean = true, val action: () -> Unit)

@Composable
fun GameMenuBar(v: Virtual, vm: ClaraViewModel) {
    val s = vm.s
    val menuCtx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().height(v.w(11)).background(Vga.White)
            .padding(horizontal = v.w(3)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(v.w(6))
    ) {
        // Menu contents match the DOS EXE menu resources: Game = About Clara... / New /
        // Save (always grayed — no save slots in the remake yet) / Quit (with the original
        // "Do you really want to quit?" confirm); Options = Sound / Joystick.
        // A leading check/space marks Options toggle state; keep it out of the translated label.
        fun chk(on: Boolean, label: String) = (if (on) "√" else " ") + Strings.ui(label)
        MenuTitle(v, Strings.ui("Game"), listOf(
            MenuItemDef(Strings.ui("About Clara...")) { vm.openOverlay(Overlay.About) },
            MenuItemDef(Strings.ui("New")) { vm.menuNewCase() },
            MenuItemDef(Strings.ui("New Game")) { vm.newGameFlow() },
            MenuItemDef(Strings.ui("Quit")) { vm.openOverlay(Overlay.ConfirmQuit) },
        ))
        MenuTitle(v, Strings.ui("Options"), listOf(
            MenuItemDef(chk(s.soundOn, "Sound")) { vm.toggleSound() },
            MenuItemDef(chk(s.hapticsOn, "Haptics")) { vm.toggleHaptics() },
            MenuItemDef(chk(s.captionsOn, "Captions")) { vm.toggleCaptions() },
            MenuItemDef(chk(com.acme.clara.notify.Reminders.enabled(menuCtx), "Reminders")) {
                com.acme.clara.notify.Reminders.setEnabled(menuCtx, !com.acme.clara.notify.Reminders.enabled(menuCtx))
            },
            MenuItemDef(" " + Strings.ui("Joystick")) { vm.showJoystick() },
            MenuItemDef(" " + Strings.ui("Language...")) { vm.openOverlay(Overlay.Language) },
        ))
        MenuTitle(v, Strings.ui("Acme"), listOf(
            MenuItemDef(Strings.ui("Hint")) { vm.requestHint() },
            MenuItemDef(Strings.ui("Detective Roster")) { vm.openOverlay(Overlay.Roster) },
            MenuItemDef(Strings.ui("World Database")) { vm.openOverlay(Overlay.Almanac) },
            MenuItemDef(Strings.ui("Passport")) { vm.openOverlay(Overlay.Passport) },
            MenuItemDef(Strings.ui("Most Wanted")) { vm.openOverlay(Overlay.MostWanted) },
            MenuItemDef(Strings.ui("Commendations")) { vm.openOverlay(Overlay.Commendations) },
        ))
        // Dossiers menu lists the ten suspects under their EXE short names
        MenuTitle(v, Strings.ui("Dossiers"), GameData.suspects.mapIndexed { i, su ->
            MenuItemDef(GameData.dossierMenuNames.getOrElse(i) { su.name }) {
                vm.openOverlay(Overlay.Dossier(su))
            }
        })
        // Dev-only test shortcuts — never compiled into a release build. Labels stay in plain
        // English on purpose (a developer tool, not player-facing content).
        if (com.acme.clara.BuildConfig.DEBUG) {
            MenuTitle(v, "Debug", listOf(
                MenuItemDef("Jump: hideout doorstep") { vm.devJumpToHideoutDoorstep() },
                MenuItemDef("Jump: result (win+promo)") { vm.devJumpToResultWin(true) },
                MenuItemDef("Jump: result (win)") { vm.devJumpToResultWin(false) },
            ))
        }
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
                    .clickable(enabled = def.enabled) { pressed = true; open = false; def.action() }
                    .labelled(def.label.trim())
                    .padding(horizontal = 14.dp, vertical = 5.dp)) {
                    // disabled items render gray, like the DOS grayed Save entry
                    Text(def.label, fontFamily = FontFamily.Monospace,
                        color = if (!def.enabled) Vga.LightGray
                            else if (pressed) Vga.White else Vga.Black,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

/** Renders the active menu overlay as a centred VGA dialog. Returns nothing if none. */
@Composable
fun OverlayHost(v: Virtual, vm: ClaraViewModel) {
    val o = vm.s.overlay ?: return
    if (o is Overlay.Dossier) { DossierWindow(v, o.suspect) { vm.dismissOverlay() }; return }
    if (o is Overlay.ConfirmQuit) { ConfirmQuitDialog(v, vm); return }
    if (o is Overlay.Language) { LanguageWindow(v, vm); return }
    if (o is Overlay.MostWanted) { MostWantedWindow(v, vm); return }
    if (o is Overlay.Commendations) { CommendationsWindow(v, vm); return }
    if (o is Overlay.Almanac) { AlmanacWindow(v, vm); return }
    if (o is Overlay.Passport) { PassportWindow(v, vm); return }
    val (title, lines) = when (o) {
        Overlay.About -> Strings.ui("ABOUT") to listOf(
            "Where in the World is", "Clara San Diego?  (Enhanced)", "MS-DOS Version 2.1",
            "Copyright 2026 Denis Inc",
        )
        Overlay.Roster -> Strings.ui("ACME DETECTIVE ROSTER") to listOf(
            Strings.ui("Detective: {0}", vm.s.detectiveName),
            Strings.ui("Rank: {0}", Strings.label("rank", GameData.ranks[vm.s.rankIndex])),
            Strings.ui("Cases solved: {0}", vm.s.casesSolved),
        )
        is Overlay.Dossier -> "" to emptyList()   // handled by DossierWindow above
        Overlay.ConfirmQuit -> "" to emptyList()  // handled by ConfirmQuitDialog above
        Overlay.Language -> "" to emptyList()     // handled by LanguageWindow above
        Overlay.MostWanted -> "" to emptyList()   // handled by MostWantedWindow above
        Overlay.Commendations -> "" to emptyList()// handled by CommendationsWindow above
        Overlay.Almanac -> "" to emptyList()      // handled by AlmanacWindow above
        Overlay.Passport -> "" to emptyList()     // handled by PassportWindow above
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

/** Game > Quit: the original's "Do you really want to quit?" dialog with yellow Yes/No
 *  buttons (red bold text, black border + drop shadow — same style as the sign-on Y/N). */
@Composable
private fun ConfirmQuitDialog(v: Virtual, vm: ClaraViewModel) {
    Box(Modifier.fillMaxSize().clickable { vm.dismissOverlay() }) {
        v.At(78, 82, 170, 52) { Box(Modifier.fillMaxSize().background(Vga.Black)) }   // shadow
        v.At(75, 79, 170, 52) {
            Column(Modifier.fillMaxSize().background(Vga.White)
                .border(BorderStroke(v.w(1), Vga.Black)).padding(v.w(4)),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Do you really want to quit?", style = v.text(8, color = Vga.Black, bold = true))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(v.w(10))) {
                    QuitButton(v, "Yes") { vm.menuQuitToTitle() }
                    QuitButton(v, "No") { vm.dismissOverlay() }
                }
            }
        }
    }
}

@Composable
private fun QuitButton(v: Virtual, label: String, onClick: () -> Unit) {
    Box(Modifier.background(Vga.Yellow).border(BorderStroke(v.w(1), Vga.Black))
        .clickable(onClick = onClick).padding(horizontal = v.w(10), vertical = v.w(2)),
        contentAlignment = Alignment.Center) {
        Text(label, style = v.text(8.5f, color = Vga.Red, bold = true))
    }
}

/** Options > Language — pick the interface language; each row is shown in its own language. */
@Composable
private fun LanguageWindow(v: Virtual, vm: ClaraViewModel) {
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.9f).background(Vga.Blue).padding(v.w(6)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(com.acme.clara.i18n.Strings.get("menu.language.title"),
                style = v.text(10, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(4)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                com.acme.clara.i18n.Strings.LANGUAGES.forEach { (code, name) ->
                    val sel = code == com.acme.clara.i18n.Strings.language
                    Box(Modifier.fillMaxWidth().padding(vertical = v.w(0.8f))
                        .then(if (sel) Modifier.background(Vga.White) else Modifier)
                        .clickable { com.acme.clara.i18n.Strings.setLanguage(code); vm.dismissOverlay() }
                        .padding(vertical = v.w(2)),
                        contentAlignment = Alignment.Center) {
                        Text(name, style = v.text(9, color = if (sel) Vga.Blue else Vga.White, bold = sel))
                    }
                }
            }
            Spacer(Modifier.height(v.w(3)))
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
    val reduce = reducedMotion()
    var shown by remember(su.name) { mutableStateOf(0) }
    LaunchedEffect(su.name) {
        if (reduce) shown = total                   // reduced-motion: show the dossier at once
        else { shown = 0; while (shown < total) { kotlinx.coroutines.delay(8); shown += 2 } }
    }
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
                                // 61x75: the capture's bottom 5 rows were baked page-rule bars,
                                // cropped out of the assets
                                if (spriteExists(slug))
                                    PixelImage(slug, Modifier.size(v.w(61), v.w(75)))
                                else Box(Modifier.size(v.w(61), v.w(75)).background(Vga.DarkGray))
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

/* Most-Wanted gallery — the fixed villain roster as a grid of mugshot tiles. Captured
 * crooks show their portrait and name; the rest stay locked behind a "?" until jailed. */
@Composable
private fun MostWantedWindow(v: Virtual, vm: ClaraViewModel) {
    val gallery = MostWanted.gallery(vm.s.capturedVillains)
    val caught = MostWanted.capturedCount(vm.s.capturedVillains)
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.9f).background(Vga.Blue).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MOST WANTED", style = v.text(10, color = Vga.Yellow, bold = true))
            Text("$caught / ${gallery.size} captured", style = v.text(7, color = Vga.LightCyan))
            Spacer(Modifier.height(v.w(3)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                gallery.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(v.w(2))) {
                        row.forEach { entry -> WantedTile(v, entry, Modifier.weight(1f)) }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(v.w(2)))
                }
            }
            Spacer(Modifier.height(v.w(3)))
            DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

@Composable
private fun WantedTile(v: Virtual, entry: WantedEntry, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val slug = "suspect_" + entry.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        Box(Modifier.fillMaxWidth().aspectRatio(0.82f).border(BorderStroke(v.w(0.7f), Vga.Black))
            .background(Vga.DarkGray), contentAlignment = Alignment.Center) {
            if (entry.captured && spriteExists(slug)) PixelImage(slug, Modifier.fillMaxSize())
            else Text(if (entry.captured) "◆" else "?",
                style = v.text(15, color = Vga.LightGray, bold = true))
        }
        Text(if (entry.captured) entry.name.replace("\"", "") else "AT LARGE",
            style = v.text(5.5f, color = if (entry.captured) Vga.White else Vga.LightGray),
            maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/* Commendations + career stats — the persistent record beside the rank ladder. Earned
 * achievements glow; the rest stay dim with their unlock condition on show. */
@Composable
private fun CommendationsWindow(v: Virtual, vm: ClaraViewModel) {
    val s = vm.s
    val ids = Achievements.catalog.map { it.id }.toSet()
    val earned = s.unlockedAchievements.count { it in ids }
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).background(Vga.Blue).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("COMMENDATIONS", style = v.text(10, color = Vga.Yellow, bold = true))
            Text("$earned / ${Achievements.catalog.size} earned", style = v.text(7, color = Vga.LightCyan))
            Spacer(Modifier.height(v.w(3)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                StatRow(v, "Detective", s.detectiveName.ifBlank { "—" })
                StatRow(v, "Rank", GameData.ranks.getOrElse(s.rankIndex) { "Rookie" })
                StatRow(v, "Cases solved", "${s.casesSolved}")
                StatRow(v, "Villains caught", "${MostWanted.capturedCount(s.capturedVillains)} / ${MostWanted.total()}")
                StatRow(v, "Hint-free solves", "${s.hintFreeSolves}")
                // H4: the case-a-day streak (with any banked weekly freeze)
                StatRow(v, "Daily streak", buildString {
                    append(if (s.streakDays > 0) "🔥 ${s.streakDays} day(s)" else "—")
                    if (s.streakFreezes > 0) append("  ❄ freeze ready")
                })
                Spacer(Modifier.height(v.w(3)))
                // P2 endowed progress: a bar toward the next rank that opens with a head start
                RankProgress(v, s)
                Spacer(Modifier.height(v.w(3)))
                Text("— COMMENDATIONS —", style = v.text(6.5f, color = Vga.Yellow, bold = true),
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(v.w(1)))
                Achievements.catalog.forEach { a ->
                    val got = a.id in s.unlockedAchievements
                    Column(Modifier.fillMaxWidth().padding(vertical = v.w(1))) {
                        Text((if (got) "★ " else "☆ ") + a.title,
                            style = v.text(7.5f, color = if (got) Vga.Yellow else Vga.LightGray, bold = true))
                        Text(a.desc, style = v.text(6.2f, color = if (got) Vga.White else Vga.LightGray))
                    }
                }
            }
            Spacer(Modifier.height(v.w(3)))
            DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

@Composable
private fun StatRow(v: Virtual, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = v.w(0.6f)),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = v.text(7, color = Vga.LightCyan))
        Text(value, style = v.text(7, color = Vga.White, bold = true))
    }
}

/* P2 endowed progress: a bar toward the next rank. The tutorial + guided first case are
 * credited as a one-case head start, so it opens partly filled (never a cold zero) — the
 * loyalty-card effect that lifts first-promotion completion. */
@Composable
internal fun RankProgress(v: Virtual, s: com.acme.clara.game.GameState) {
    val thresholds = listOf(1, 5, 9, 13)
    val solved = s.casesSolved
    val next = thresholds.firstOrNull { it > solved }
    val label: String
    val frac: Float
    if (next == null) {
        label = "${GameData.ranks.last()} — top rank"
        frac = 1f
    } else {
        val prev = thresholds.lastOrNull { it <= solved } ?: 0
        val band = next - prev
        val inBand = solved - prev
        val head = 1                                   // the credited head start
        frac = ((inBand + head).toFloat() / (band + head)).coerceIn(0f, 1f)
        val nextRank = GameData.ranks.getOrElse(s.rankIndex + 1) { GameData.ranks.last() }
        label = "Toward $nextRank — ${inBand + head} of ${band + head}"
    }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = v.text(6.5f, color = Vga.LightCyan))
        Spacer(Modifier.height(v.w(1)))
        Box(Modifier.fillMaxWidth().height(v.w(4)).background(Vga.DarkGray)
            .border(BorderStroke(v.w(0.6f), Vga.LightGray))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Vga.LightGreen))
        }
    }
}

/* World Database (the in-game almanac) — a browsable Interpol reference over CityMeta.
 * Master list of cities → tap one for its region, landmark and facts. It names places, not
 * the next city, so it's a reference the player still has to reason from. */
@Composable
private fun AlmanacWindow(v: Virtual, vm: ClaraViewModel) {
    var selected by remember { mutableStateOf<String?>(null) }
    val cities = remember { com.acme.clara.data.CityMeta.all.values.sortedBy { it.name } }

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).background(Vga.Blue).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("INTERPOL DATABASE", style = v.text(10, color = Vga.Yellow, bold = true))

            val entry = selected?.let { name -> cities.firstOrNull { it.name == name } }
            if (entry == null) {
                Text("${cities.size} places on file", style = v.text(7, color = Vga.LightCyan))
                Spacer(Modifier.height(v.w(3)))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    cities.forEach { info ->
                        // L4: flag where each fact sits on the spaced-repetition curve.
                        val fresh = SpacedRepetition.isFresh(info.name, vm.s.cityLastSeen, vm.s.casesSolved)
                        val due = SpacedRepetition.isDue(info.name, vm.s.cityLastSeen, vm.s.casesSolved)
                        val marker = when { fresh -> "seen recently"; due -> "review due"; else -> "" }
                        Row(Modifier.fillMaxWidth().clickable { selected = info.name }
                            .labelled("${info.name}, ${info.region}${if (marker.isNotEmpty()) ", $marker" else ""}")
                            .padding(vertical = v.w(1.4f)),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(info.name, style = v.text(7.5f, color = Vga.White, bold = true))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(info.region, style = v.text(6.5f, color = Vga.LightCyan))
                                if (marker.isNotEmpty())
                                    Text(marker, style = v.text(5.5f,
                                        color = if (fresh) Vga.LightGreen else Vga.Yellow))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(v.w(3)))
                DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                    style = v.text(9, bold = true)) { vm.dismissOverlay() }
            } else {
                Text(entry.name.uppercase(), style = v.text(8.5f, color = Vga.LightGreen, bold = true))
                Spacer(Modifier.height(v.w(2)))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    StatRow(v, "Region", entry.region)
                    StatRow(v, "Landmark", entry.landmark)
                    Spacer(Modifier.height(v.w(3)))
                    Text(entry.description, style = v.text(7.5f, color = Vga.White))
                }
                Spacer(Modifier.height(v.w(3)))
                Row(horizontalArrangement = Arrangement.spacedBy(v.w(6))) {
                    DosButton("◀ BACK", fill = Vga.LightGray, textColor = Vga.Black,
                        style = v.text(9, bold = true)) { selected = null }
                    DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                        style = v.text(9, bold = true)) { vm.dismissOverlay() }
                }
            }
        }
    }
}

/* Passport (C4) — a painted world map of the countries the detective has visited.
 * Country border silhouettes (data.CountryShapes, projected through the same Mercator
 * transform as the DEPART map) fill in green as you land in each place; micro-states with
 * no usable polygon are stamped as a dot. Visits are tracked from day one on the free tier
 * (GameState.visitedPlaces); the free Passport shows only the original 30 destinations —
 * the paid expansion adds its 68 to the world and paints in everything already visited. */
@Composable
private fun PassportWindow(v: Virtual, vm: ClaraViewModel) {
    val s = vm.s
    val paid = s.expansionUnlocked
    // Free tier tracks silently but reveals nothing: the passport stays sealed until the
    // paid unlock, which then paints in every country already visited (see PassportSealed).
    if (!paid) { PassportSealed(v, vm); return }
    val placeCountry = CountryShapes.placeCountry
    val universe = placeCountry.values.toSet()
    val visitedPlaces = s.visitedPlaces.filter { it in placeCountry }
    val visitedCountries = visitedPlaces.mapNotNull { placeCountry[it] }.toSet()

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f).background(Vga.Blue).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PASSPORT", style = v.text(10, color = Vga.Yellow, bold = true))
            Text("${visitedCountries.size} of ${universe.size} countries",
                style = v.text(7, color = Vga.LightCyan))
            Spacer(Modifier.height(v.w(3)))

            // the painted map — the raster DEPART interior with visited countries filled in
            Box(Modifier.fillMaxWidth().aspectRatio(WorldMap.WV / WorldMap.HV)
                .background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(1))) {
                PixelImage("world_map_clean", Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    // filled silhouettes for visited countries that have a polygon
                    visitedCountries.forEach { code ->
                        val rings = CountryShapes.rings(code)
                        if (rings.isNotEmpty()) {
                            val path = Path()
                            rings.forEach { ring ->
                                ring.firstOrNull()?.let { p0 -> path.moveTo(p0.x * w, p0.y * h) }
                                for (i in 1 until ring.size) path.lineTo(ring[i].x * w, ring[i].y * h)
                                path.close()
                            }
                            drawPath(path, Vga.Green.copy(alpha = 0.6f))
                            drawPath(path, Vga.LightGreen, style = Stroke(width = h * 0.008f))
                        }
                    }
                    // micro-states / Antarctica: stamp the visited place's dot instead
                    val r = h * 0.03f
                    visitedPlaces.forEach { place ->
                        val code = placeCountry[place] ?: return@forEach
                        if (CountryShapes.rings(code).isEmpty()) {
                            WorldMap.of(place)?.let { pos ->
                                val cx = pos.x * w; val cy = pos.y * h
                                drawRect(Vga.Black, Offset(cx - r, cy - r), Size(r * 2, r * 2))
                                drawRect(Vga.LightGreen,
                                    Offset(cx - r * 0.6f, cy - r * 0.6f), Size(r * 1.2f, r * 1.2f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(v.w(3)))

            // the stamps: visited countries by name (the collection, also for accessibility)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (visitedCountries.isEmpty()) {
                    Text("No stamps yet — solve a case to start filling your passport.",
                        style = v.text(7, color = Vga.LightGray))
                } else {
                    visitedCountries
                        .map { CountryShapes.countryName[it] ?: it }
                        .sorted()
                        .forEach { name ->
                            Text("✓ ${name.uppercase()}",
                                style = v.text(7, color = Vga.LightGreen, bold = true),
                                modifier = Modifier.fillMaxWidth().padding(vertical = v.w(0.5f)))
                        }
                }
            }
            Spacer(Modifier.height(v.w(3)))
            DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

/* Free tier: the passport is sealed. Visits are still recorded (GameState.visitedPlaces),
 * but nothing about the collection is shown — the paid unlock reveals and paints it all. */
@Composable
private fun PassportSealed(v: Virtual, vm: ClaraViewModel) {
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.86f).background(Vga.Blue).padding(v.w(7)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PASSPORT", style = v.text(10, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(4)))
            Box(Modifier.size(v.w(44), v.w(30)).background(Vga.DarkGray)
                .border(BorderStroke(v.w(1.5f), Vga.LightGray)), contentAlignment = Alignment.Center) {
                Text("🔒", style = v.text(20, color = Vga.Yellow))
            }
            Spacer(Modifier.height(v.w(4)))
            listOf(
                "Your passport is sealed.",
                "Every place you visit is",
                "quietly being recorded.",
                "",
                "Unlock the Expansion to reveal",
                "your painted map of the world.",
            ).forEach {
                Text(it, style = v.text(7.5f, color = Vga.White), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(v.w(5)))
            DosButton("CLOSE", fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}
