package com.acme.clara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import com.acme.clara.data.CountryShapes
import com.acme.clara.data.AlmanacFlags
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
import com.acme.clara.data.Masterminds
import com.acme.clara.game.Achievements
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.MostWanted
import com.acme.clara.game.Overlay
import com.acme.clara.game.WantedEntry
import com.acme.clara.i18n.Strings
import com.acme.clara.ui.screens.CityPhoto
import com.acme.clara.ui.theme.Vga
import kotlinx.coroutines.launch

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
        // Menu contents follow the DOS EXE menu resources, adapted for the remake:
        // Game = About Clara... / New Case (the EXE's "New": re-deal the case in this career) /
        // Load Game (the saved-career picker) / New Game (fresh career) / Quit (with the original
        // confirm). The EXE's Joystick item was dropped — a touch game has no joystick to calibrate.
        // A leading check/space marks Options toggle state; keep it out of the translated label.
        fun chk(on: Boolean, label: String) = (if (on) "√" else " ") + Strings.ui(label)
        MenuTitle(v, Strings.ui("Game"), listOf(
            MenuItemDef(Strings.ui("About Clara...")) { vm.openOverlay(Overlay.About) },
            MenuItemDef(Strings.ui("New Case"), enabled = s.phase != com.acme.clara.game.Phase.SIGN_ON) {
                vm.menuNewCase()
            },
            MenuItemDef(Strings.ui("Load Game")) { vm.toChooseGame() },
            MenuItemDef(Strings.ui("New Game")) { vm.newGameFlow() },
            MenuItemDef(Strings.ui("Quit")) { vm.openOverlay(Overlay.ConfirmQuit) },
        ))
        MenuTitle(v, Strings.ui("Options"), listOf(
            MenuItemDef(chk(s.soundOn, "Sound")) { vm.toggleSound() },
            MenuItemDef(chk(s.hapticsOn, "Haptics")) { vm.toggleHaptics() },
            MenuItemDef(chk(s.captionsOn, "Captions")) { vm.toggleCaptions() },
            MenuItemDef(chk(s.travelBufferEnabled, "+8h travel buffer"), enabled = s.expansionUnlocked) {
                vm.toggleTravelBuffer()
            },
            MenuItemDef(chk(com.acme.clara.notify.Reminders.enabled(menuCtx), "Reminders")) {
                com.acme.clara.notify.Reminders.setEnabled(menuCtx, !com.acme.clara.notify.Reminders.enabled(menuCtx))
            },
            MenuItemDef(" " + Strings.ui("Language...")) { vm.openOverlay(Overlay.Language) },
        ))
        MenuTitle(v, Strings.ui("Bureau"), listOf(
            MenuItemDef(Strings.ui("Hint")) { vm.requestHint() },
            MenuItemDef(Strings.ui("Case Planner")) { vm.openCasePlanner() },
            MenuItemDef(Strings.ui("Detective Roster")) { vm.openOverlay(Overlay.Roster) },
            MenuItemDef(Strings.ui("World Database")) { vm.openOverlay(Overlay.Almanac) },
            MenuItemDef(Strings.ui("Passport")) { vm.openOverlay(Overlay.Passport) },
            MenuItemDef(Strings.ui("Most Wanted")) { vm.openOverlay(Overlay.MostWanted) },
            MenuItemDef(Strings.ui("Commendations")) { vm.openOverlay(Overlay.Commendations) },
        ))
        // Dossiers lists the original and expanded roster; appended entries keep older i18n
        // dossier indices stable.
        MenuTitle(v, Strings.ui("Dossiers"), GameData.suspects.mapIndexed { i, su ->
            MenuItemDef(GameData.dossierMenuNames.getOrElse(i) { su.name }) {
                vm.openOverlay(Overlay.Dossier(su))
            }
        })
        // Persistent Campaign entry: it opens the shared offer while locked and wave status once
        // owned. It remains hidden only in a sales-disabled free build.
        if (com.acme.clara.billing.BillingManager.SALES_ENABLED || s.expansionUnlocked) {
            MenuTitle(v, Strings.ui("Campaign"), listOf(
                MenuItemDef(if (s.expansionUnlocked) Strings.ui("World Campaign unlocked")
                    else Strings.ui("Unlock the World Campaign...")) {
                    if (s.expansionUnlocked) {
                        val freeRemaining = (com.acme.clara.game.GameState.CAREER_CASES - s.casesSolved).coerceAtLeast(0)
                        if (freeRemaining > 0) {
                            vm.openOverlay(Overlay.Info(Strings.ui("WORLD CAMPAIGN"), listOf(
                                Strings.ui("Wave 1 is unlocked and ready."),
                                Strings.ui("{0} free-career cases remain before the campaign story begins.", freeRemaining),
                            )))
                        } else {
                            val campaignSolved = s.casesSolved - com.acme.clara.game.GameState.CAREER_CASES
                            val wave = if (s.pendingPromotion)
                                Masterminds.arcForCampaignCase(campaignSolved)?.waveIndex
                                    ?: Masterminds.unlockedMaxWave(s.rankIndex, true)
                                else Masterminds.unlockedMaxWave(s.rankIndex, true)
                            vm.openOverlay(Overlay.Info(Strings.ui("WORLD CAMPAIGN"), listOf(
                                Strings.ui("Wave {0} of 10", wave + 1),
                                if (s.pendingPromotion) Strings.ui("Finale complete — promotion quiz pending.")
                                else Strings.ui("{0} cases until this wave's finale",
                                    Masterminds.waveCases[wave] - Masterminds.casesIntoCurrentWave(campaignSolved)),
                            )))
                        }
                    } else vm.openOverlay(Overlay.PurchaseOffer("Persistent menu bar"))
                },
            ))
        }
        // Dev-only test shortcuts — never compiled into a release build. Labels stay in plain
        // English on purpose (a developer tool, not player-facing content).
        if (com.acme.clara.BuildConfig.DEBUG) {
            MenuTitle(v, "Debug", listOf(
                MenuItemDef("Jump: hideout doorstep") { vm.devJumpToHideoutDoorstep() },
                MenuItemDef("Jump: result (win+promo)") { vm.devJumpToResultWin(true) },
                MenuItemDef("Jump: result (win)") { vm.devJumpToResultWin(false) },
                MenuItemDef((if (s.expansionUnlocked) "√ " else "  ") + "Paid version") {
                    vm.devTogglePaid()
                },
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
                    // Adjacent popup rows used to clip Compose's automatic 48dp touch
                    // expansion down to the text's ~28dp line box. Give every menu item
                    // its own full target while leaving the DOS typography unchanged.
                    .heightIn(min = 48.dp)
                    .clickable(enabled = def.enabled) { pressed = true; open = false; def.action() }
                    .labelled(def.label.trim())
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                    contentAlignment = Alignment.CenterStart) {
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
    if (o is Overlay.PurchaseOffer) { PurchaseOfferWindow(v, vm); return }
    if (o is Overlay.UnlockCeremony) { UnlockCeremonyWindow(v, vm); return }
    if (o is Overlay.CasePlanner) { CasePlannerWindow(v, vm); return }
    val (title, lines) = when (o) {
        Overlay.About -> Strings.ui("ABOUT") to listOf(
            "Where in the World is", "Clara San Diego?  (Enhanced)", "MS-DOS Version 2.1",
            "Copyright 2026 Denis Inc",
        )
        Overlay.Roster -> Strings.ui("WDB DETECTIVE ROSTER") to listOf(
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
        is Overlay.PurchaseOffer -> "" to emptyList() // handled by PurchaseOfferWindow above
        Overlay.UnlockCeremony -> "" to emptyList() // handled by UnlockCeremonyWindow above
        Overlay.CasePlanner -> "" to emptyList() // handled by CasePlannerWindow above
        is Overlay.Info -> o.title to o.lines
    }

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.86f)
                .background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))
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
            DosButton(Strings.ui("CLOSE"), fill = Vga.Green, textColor = Vga.White,
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
                Text(Strings.ui("Do you really want to quit?"), style = v.text(8, color = Vga.Black, bold = true))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(v.w(10))) {
                    QuitButton(v, Strings.ui("Yes")) { vm.menuQuitToTitle() }
                    QuitButton(v, Strings.ui("No")) { vm.dismissOverlay() }
                }
            }
        }
    }
}

@Composable
private fun QuitButton(v: Virtual, label: String, onClick: () -> Unit) {
    Box(Modifier.background(Vga.Yellow).border(BorderStroke(v.w(1), Vga.Black))
        .clickable(onClick = onClick).tappable(label)
        .padding(horizontal = v.w(10), vertical = v.w(2)),
        contentAlignment = Alignment.Center) {
        Text(label, style = v.text(8.5f, color = Vga.Red, bold = true))
    }
}

/** Options > Language — pick the interface language; each row is shown in its own language. */
@Composable
private fun LanguageWindow(v: Virtual, vm: ClaraViewModel) {
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.9f).background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(6)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(com.acme.clara.i18n.Strings.get("menu.language.title"),
                style = v.text(10, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(4)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                com.acme.clara.i18n.Strings.LANGUAGES.forEach { (code, name) ->
                    val sel = code == com.acme.clara.i18n.Strings.language
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .then(if (sel) Modifier.background(Vga.White) else Modifier)
                        .clickable {
                            // setLanguage() parses a per-language catalog (up to ~336KB) off the
                            // main thread — the same reasoning as the cold-start Humor/save loads.
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) { com.acme.clara.i18n.Strings.setLanguage(code) }
                            vm.dismissOverlay()
                        }
                        .labelled(name).padding(vertical = v.w(2)),
                        contentAlignment = Alignment.Center) {
                        Text(name, style = v.text(9, color = if (sel) Vga.Black else Vga.White, bold = sel))
                    }
                }
            }
            Spacer(Modifier.height(v.w(3)))
            DosButton(Strings.ui("CLOSE"), fill = Vga.Green, textColor = Vga.White,
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
    // Dossier facts are Kotlin data (EXE-faithful English); localized via a per-suspect overlay
    // keyed by roster index — names stay untranslated.
    val idx = GameData.suspects.indexOf(su)
    fun d(field: String, en: String) = Strings.opt("suspect.$idx.$field") ?: en
    // typed-on effect: number of characters shown across all fields
    val fields = listOf(
        "Name:" to su.name, "Sex:" to d("sex", su.sex), "Occupation:" to d("occupation", su.occupation),
        "Hobby:" to d("hobby", su.hobby), "Hair Color:" to d("hair", su.hair), "Auto:" to d("auto", su.auto),
        "Feature:" to d("feature1", su.feature1), "Other:" to d("feature2", su.feature2),
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
                        Row { Text(Strings.ui("Name:") + " ", style = label); Text(taken(0), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Row { Text(Strings.ui("Sex:") + " ", style = label); Text(taken(1), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Strings.ui("Occupation:") + " ") }
                            append(taken(2))
                        }, style = value)
                        Spacer(Modifier.height(v.w(4)))
                        Row { Text(Strings.ui("Hobby:") + " ", style = label); Text(taken(3), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Row { Text(Strings.ui("Hair Color:") + " ", style = label); Text(taken(4), style = value) }
                        Spacer(Modifier.height(v.w(2)))
                        Row { Text(Strings.ui("Auto:") + " ", style = label); Text(taken(5), style = value) }
                    }
                }
                // Feature / Other span the full window width beneath the portrait
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(start = v.w(4), end = v.w(4), bottom = v.w(4))) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Strings.ui("Feature:") + " ") }
                        append(taken(6))
                    }, style = value)
                    Spacer(Modifier.height(v.w(4)))
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Strings.ui("Other:") + " ") }
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
        Column(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.9f).background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Strings.ui("MOST WANTED"), style = v.text(10, color = Vga.Yellow, bold = true))
            Text(Strings.ui("{0} / {1} captured", caught, gallery.size), style = v.text(7, color = Vga.LightCyan))
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
            DosButton(Strings.ui("CLOSE"), fill = Vga.Green, textColor = Vga.White,
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
        Text(if (entry.captured) entry.name.replace("\"", "") else Strings.ui("AT LARGE"),
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
        Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Strings.ui("COMMENDATIONS"), style = v.text(10, color = Vga.Yellow, bold = true))
            Text(Strings.ui("{0} / {1} earned", earned, Achievements.catalog.size), style = v.text(7, color = Vga.LightCyan))
            Spacer(Modifier.height(v.w(3)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                StatRow(v, Strings.ui("Detective"), s.detectiveName.ifBlank { "—" })
                StatRow(v, Strings.ui("Rank"), Strings.label("rank", GameData.ranks.getOrElse(s.rankIndex) { "Rookie" }))
                StatRow(v, Strings.ui("Cases solved"), "${s.casesSolved}")
                StatRow(v, Strings.ui("Villains caught"), "${MostWanted.capturedCount(s.capturedVillains)} / ${MostWanted.total()}")
                StatRow(v, Strings.ui("Hint-free solves"), "${s.hintFreeSolves}")
                // H4: the case-a-day streak (with any banked weekly freeze)
                StatRow(v, Strings.ui("Daily streak"), buildString {
                    append(if (s.streakDays > 0) Strings.ui("🔥 {0} day(s)", s.streakDays) else "—")
                    if (s.streakFreezes > 0) append("  " + Strings.ui("❄ freeze ready"))
                })
                Spacer(Modifier.height(v.w(3)))
                // P2 endowed progress: a bar toward the next rank that opens with a head start
                RankProgress(v, s)
                Spacer(Modifier.height(v.w(3)))
                Text("— " + Strings.ui("COMMENDATIONS") + " —", style = v.text(6.5f, color = Vga.Yellow, bold = true),
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(v.w(1)))
                Achievements.catalog.forEach { a ->
                    val got = a.id in s.unlockedAchievements
                    Column(Modifier.fillMaxWidth().padding(vertical = v.w(1))) {
                        Text((if (got) "★ " else "☆ ") + (Strings.opt("achv.${a.id}.title") ?: a.title),
                            style = v.text(7.5f, color = if (got) Vga.Yellow else Vga.LightGray, bold = true))
                        Text(Strings.opt("achv.${a.id}.desc") ?: a.desc,
                            style = v.text(6.2f, color = if (got) Vga.White else Vga.LightGray))
                    }
                }
            }
            Spacer(Modifier.height(v.w(3)))
            DosButton(Strings.ui("CLOSE"), fill = Vga.Green, textColor = Vga.White,
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

/* Rank progress: exact free-promotion bands, followed by each authored campaign wave's own
 * length. Case 14 is the boundary, so Wave 1 begins at a truthful 0 of 8. */
@Composable
internal fun RankProgress(v: Virtual, s: com.acme.clara.game.GameState) {
    val solved = s.casesSolved
    val freeThresholds = listOf(1, 5, 9, 13)
    val label: String
    val frac: Float
    if (s.pendingPromotion) {
        if (solved <= com.acme.clara.game.GameState.CAREER_CASES) {
            val previous = freeThresholds.lastOrNull { it < solved } ?: 0
            val total = solved - previous
            val nextRank = GameData.ranks.getOrElse(s.rankIndex + 1) { GameData.ranks.last() }
            label = Strings.ui("Toward {0} — {1} of {2}", Strings.label("rank", nextRank), total, total)
            frac = 1f
        } else {
            val campaignSolved = solved - com.acme.clara.game.GameState.CAREER_CASES
            val wave = Masterminds.arcForCampaignCase(campaignSolved)?.waveIndex
                ?: Masterminds.unlockedMaxWave(s.rankIndex, true)
            val total = Masterminds.waveCases[wave]
            val nextRank = GameData.ranks[Masterminds.arcs[wave].patentRank]
            label = Strings.ui("Toward {0} — {1} of {2}", Strings.label("rank", nextRank), total, total)
            frac = 1f
        }
    } else if (s.careerOver) {
        label = Strings.ui("{0} — top rank", Strings.label("rank", GameData.ranks.last()))
        frac = 1f
    } else if (s.expansionUnlocked && solved >= com.acme.clara.game.GameState.CAREER_CASES) {
        val campaignSolved = solved - com.acme.clara.game.GameState.CAREER_CASES
        val wave = Masterminds.unlockedMaxWave(s.rankIndex, true)
        val inWave = Masterminds.casesIntoCurrentWave(campaignSolved)
        val total = Masterminds.waveCases[wave]
        val nextRank = GameData.ranks[Masterminds.arcs[wave].patentRank]
        label = Strings.ui("Toward {0} — {1} of {2}", Strings.label("rank", nextRank), inWave, total)
        frac = inWave.toFloat() / total
    } else {
        val next = freeThresholds.firstOrNull { it > solved }
        if (next == null) {
            label = Strings.ui("{0} — top rank", Strings.label("rank", GameData.ranks[s.rankIndex]))
            frac = 1f
        } else {
            val prev = freeThresholds.lastOrNull { it <= solved } ?: 0
            val band = next - prev
            val inBand = solved - prev
            frac = (inBand.toFloat() / band).coerceIn(0f, 1f)
            val nextRank = GameData.ranks.getOrElse(s.rankIndex + 1) { GameData.ranks.last() }
            label = Strings.ui("Toward {0} — {1} of {2}", Strings.label("rank", nextRank), inBand, band)
        }
    }
    val campaignSolved = (solved - com.acme.clara.game.GameState.CAREER_CASES).coerceAtLeast(0)
    val campaignWave = if (s.pendingPromotion)
        Masterminds.arcForCampaignCase(campaignSolved)?.waveIndex
            ?: Masterminds.unlockedMaxWave(s.rankIndex, true)
        else Masterminds.unlockedMaxWave(s.rankIndex, true)
    val arc = if (s.expansionUnlocked && !s.careerOver)
        Masterminds.arcForWave(campaignWave) else null
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = v.text(6.5f, color = Vga.LightCyan))
        if (arc != null) {
            Text(
                Strings.ui("Case target: {0} — {1}",
                    Strings.label("mastermind.family", arc.family), Strings.label("mastermind.role", arc.role)),
                style = v.text(6f, color = Vga.Yellow),
            )
            Text(Strings.ui("Wave {0} of 10", arc.waveIndex + 1), style = v.text(6f, color = Vga.LightCyan))
        }
        Spacer(Modifier.height(v.w(1)))
        Box(Modifier.fillMaxWidth().height(v.w(4)).background(Vga.DarkGray)
            .border(BorderStroke(v.w(0.6f), Vga.LightGray))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Vga.LightGreen))
        }
    }
}

/** One almanac fact: a small cyan label over its white value — stacked, so long values
 *  (flag descriptions especially) wrap naturally in the detail pane's column. */
@Composable
private fun AlmanacFact(v: Virtual, label: String, value: String) {
    Text(label, style = v.text(6, color = Vga.LightCyan, bold = true))
    Text(value, style = v.text(7.5f, color = Vga.White))
    Spacer(Modifier.height(v.w(2.5f)))
}

/* World Database (the in-game almanac) — a browsable reference over every place the game
 * knows: the original 30 plus both expansion rosters. Free careers see the full list, but
 * only countries represented in the original 30 open — other countries stay dimmed until
 * the paid unlock. It
 * names places, not the next city, so it's a reference the player still has to reason from. */
@Composable
private fun AlmanacWindow(v: Virtual, vm: ClaraViewModel) {
    var selected by remember { mutableStateOf<String?>(null) }
    val paid = vm.s.expansionUnlocked
    val freeNames = remember { com.acme.clara.data.CityMeta.all.keys }
    // Almanac access follows countries, not individual postcard records. If the free roster
    // already includes a country (Cairo -> Egypt), its other reference cards (Abu Simbel) stay
    // readable too even though those destinations do not enter free case routes.
    val freeCountryCodes = remember { freeNames.mapNotNull(AlmanacFlags::countryCode).toSet() }
    val names = remember {
        (com.acme.clara.data.CityMeta.all.keys +
            com.acme.clara.data.Expansion.byName.keys +
            com.acme.clara.data.Expansion2.byName.keys).toSortedSet().toList()
    }
    val maxWave = com.acme.clara.data.Masterminds.unlockedMaxWave(vm.s.rankIndex, paid)
    fun isUnlocked(name: String): Boolean =
        AlmanacFlags.countryCode(name) in freeCountryCodes ||
            (paid && (com.acme.clara.data.Progression.wave[name] ?: Int.MAX_VALUE) <= maxWave)
    // CityMeta.of resolves any known place (localized), so the detail view works for
    // expansion entries too — reachable only when unlocked, since locked rows don't tap.
    val entry = selected?.let { com.acme.clara.data.CityMeta.of(it) }
    val entryUnlocked = selected?.let(::isUnlocked) ?: false

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // Persistent corner navigation: detail cards gain a compact DOS back key while the
            // close affordance stays in the same upper-right position on both list and detail.
            Box(Modifier.fillMaxWidth().height(v.w(22))) {
                if (entry != null) {
                    Box(
                        Modifier.align(Alignment.CenterStart).height(v.w(22)).width(v.w(48))
                            .clickable { selected = null }
                            .labelled(Strings.ui("◀ BACK").removePrefix("◀").trim()),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier.background(Vga.LightGray)
                                .border(BorderStroke(v.w(0.7f), Vga.White))
                                .padding(horizontal = v.w(3), vertical = v.w(1.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(Strings.ui("◀ BACK"), style = v.text(7, color = Vga.Black, bold = true))
                        }
                    }
                }
                Text(
                    Strings.ui("WORLD DATABASE"),
                    style = v.text(10, color = Vga.Yellow, bold = true),
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(
                    Modifier.align(Alignment.CenterEnd).size(v.w(22))
                        .clickable { vm.dismissOverlay() }
                        .labelled("${Strings.ui("CLOSE")} ${Strings.ui("WORLD DATABASE")}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", style = v.text(13, color = Vga.White, bold = true))
                }
            }

            if (entry == null) {
                val unlockedCount = names.count(::isUnlocked)
                Text(
                    Strings.ui("{0} of {1} places unlocked", unlockedCount, names.size),
                    style = v.text(7, color = Vga.LightCyan))
                // L4 legend: a name's color marks where it sits on the spaced-repetition curve.
                Row(horizontalArrangement = Arrangement.spacedBy(v.w(5))) {
                    Text(Strings.ui("seen recently"), style = v.text(6, color = Vga.LightGreen))
                    Text(Strings.ui("review due"), style = v.text(6, color = Vga.Yellow))
                }
                Spacer(Modifier.height(v.w(2)))
                LazyVerticalGrid(columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(names) { name ->
                        val unlocked = isUnlocked(name)
                        val fresh = unlocked && SpacedRepetition.isFresh(name, vm.s.cityLastSeen, vm.s.casesSolved)
                        val due = unlocked && SpacedRepetition.isDue(name, vm.s.cityLastSeen, vm.s.casesSolved)
                        val color = when {
                            !unlocked -> Vga.LightGray
                            fresh -> Vga.LightGreen
                            due -> Vga.Yellow
                            else -> Vga.White
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable { selected = name }
                                .labelled(if (unlocked) Strings.place(name) else Strings.ui("{0}, locked", Strings.place(name)))
                                .padding(horizontal = v.w(1)),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(Strings.place(name), style = v.text(7, color = color, bold = unlocked))
                        }
                    }
                }
            } else {
                // Two-pane entry, like a case-file card: the postcard pinned on the left with
                // the place's name as its caption, the facts + description reading on the right.
                Row(Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(v.w(7))) {
                    Column(Modifier.width(v.w(108)), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Keep the postcard dominant; the country's real flag sits on it like a
                        // small travel stamp. The facts pane retains the written flag description.
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1.5f)
                                .border(BorderStroke(v.w(0.8f), Vga.White)),
                        ) {
                            CityPhoto(entry.name, v, Modifier.fillMaxSize())
                            AlmanacFlags.assetName(entry.name)?.let { flagAsset ->
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(v.w(3))
                                        .width(v.w(31)).height(v.w(24)),
                                ) {
                                    Box(
                                        Modifier.matchParentSize().offset(v.w(1), v.w(1))
                                            .background(Vga.Black.copy(alpha = 0.65f)),
                                    )
                                    Box(
                                        Modifier.matchParentSize().background(Vga.White).padding(v.w(1.3f)),
                                    ) {
                                        PixelImage(
                                            flagAsset, Modifier.fillMaxSize(),
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(v.w(2)))
                        Text(Strings.place(entry.name).uppercase(), style = v.text(8.5f, color = Vga.LightGreen, bold = true),
                            textAlign = TextAlign.Center)
                        Text(Strings.label("region.name", entry.region), style = v.text(6.5f, color = Vga.LightCyan),
                            textAlign = TextAlign.Center)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        // The structured attributes the clue engine draws on, surfaced as
                        // reference facts (all optional — the fallback CityInfo has none).
                        AlmanacFact(v, Strings.ui("Landmark"), entry.landmark)
                        if (entryUnlocked) {
                            entry.flag?.let { AlmanacFact(v, Strings.ui("Flag"), it) }
                            entry.currency?.let { AlmanacFact(v, Strings.ui("Currency"), it.removePrefix("the ")) }
                            entry.greeting?.let {
                                Text(it, style = v.text(6.5f, color = Vga.LightCyan))
                                Spacer(Modifier.height(v.w(3)))
                            }
                            Text(entry.description, style = v.text(7.5f, color = Vga.White))
                        } else {
                            Text("▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒", style = v.text(8, color = Vga.LightGray))
                            Spacer(Modifier.height(v.w(3)))
                            Text(Strings.ui("One fact is on file. The rest unlocks with this campaign wave."),
                                style = v.text(7, color = Vga.LightCyan))
                            if (com.acme.clara.billing.BillingManager.SALES_ENABLED && !paid) {
                                Spacer(Modifier.height(v.w(4)))
                                DosButton(Strings.ui("Unlock World Campaign"), fill = Vga.Yellow,
                                    textColor = Vga.Black, style = v.text(7.5f, bold = true)) {
                                    vm.openOverlay(Overlay.PurchaseOffer("Database locked entry"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* Passport (C4) — a painted world map of the countries the detective has visited.
 * Country border silhouettes (data.CountryShapes, projected through the same Mercator
 * transform as the DEPART map) fill in green as you land in each place; micro-states with
 * no usable polygon are stamped as a dot. Visits are tracked from day one on the free tier
 * (GameState.visitedPlaces). Paid countries are frosted individually until their campaign wave
 * opens; buying reveals Wave 1 immediately while later pages remain visibly locked. */
@Composable
private fun PassportWindow(v: Virtual, vm: ClaraViewModel) {
    val s = vm.s
    val paid = s.expansionUnlocked
    val placeCountry = CountryShapes.placeCountry
    val universe = placeCountry.values.toSet()
    fun placeUnlocked(place: String): Boolean = CountryShapes.isPlaceUnlocked(place, s.rankIndex, paid)
    val lockedCountries = CountryShapes.lockedCountryCodes(s.rankIndex, paid)
    val lockedDotPlaces = placeCountry.keys.filter { !placeUnlocked(it) &&
        CountryShapes.rings(placeCountry[it].orEmpty()).isEmpty() }
    val visitedPlaces = s.visitedPlaces.filter { it in placeCountry && placeUnlocked(it) }
    val visitedCountries = visitedPlaces.mapNotNull { placeCountry[it] }.toSet()
    val completedFamilies = Masterminds.completedFamilies(s.capturedVillains)

    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f).background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(5)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Strings.ui("PASSPORT"), style = v.text(10, color = Vga.Yellow, bold = true))
            Text(Strings.ui("{0} of {1} countries", visitedCountries.size, universe.size),
                style = v.text(7, color = Vga.LightCyan))
            Spacer(Modifier.height(v.w(3)))

            // the painted map — the raster DEPART interior with visited countries filled in
            val mapModifier = Modifier.fillMaxWidth().aspectRatio(WorldMap.WV / WorldMap.HV)
                .background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(1))
                .then(if (!paid && com.acme.clara.billing.BillingManager.SALES_ENABLED)
                    Modifier.clickable { vm.openOverlay(Overlay.PurchaseOffer("Passport locked map")) }
                else Modifier)
            Box(mapModifier) {
                PixelImage("world_map_clean", Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    // Frost only countries whose recognition wave has not been earned. This
                    // remains after purchase for Waves 2-10 instead of revealing the whole map.
                    lockedCountries.forEach { code ->
                        val rings = CountryShapes.rings(code)
                        if (rings.isNotEmpty()) {
                            val path = Path()
                            rings.forEach { ring ->
                                ring.firstOrNull()?.let { p0 -> path.moveTo(p0.x * w, p0.y * h) }
                                for (i in 1 until ring.size) path.lineTo(ring[i].x * w, ring[i].y * h)
                                path.close()
                            }
                            drawPath(path, Vga.LightGray.copy(alpha = 0.55f))
                            drawPath(path, Vga.White.copy(alpha = 0.55f), style = Stroke(width = h * 0.006f))
                        }
                    }
                    val r = h * 0.03f
                    lockedDotPlaces.forEach { place ->
                        WorldMap.of(place)?.let { pos ->
                            drawRect(Vga.LightGray.copy(alpha = 0.75f),
                                Offset(pos.x * w - r * 0.55f, pos.y * h - r * 0.55f),
                                Size(r * 1.1f, r * 1.1f))
                        }
                    }
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
                if (lockedCountries.isNotEmpty()) {
                    Box(Modifier.align(Alignment.TopCenter).padding(top = v.w(3))
                        .background(Vga.Black.copy(alpha = 0.84f)).border(BorderStroke(v.w(0.6f), Vga.LightGray))
                        .padding(horizontal = v.w(4), vertical = v.w(1.5f))) {
                        Text(
                            if (paid) Strings.ui("🔒 Future campaign waves")
                            else Strings.ui("🔒 Unlock World Campaign"),
                            style = v.text(6.5f, color = if (paid) Vga.LightCyan else Vga.Yellow, bold = true),
                        )
                    }
                }
                // Five persistent campaign seals live inside the map instead of consuming another
                // vertical row in the already-tight Passport window. A family earns full colour
                // only once both its Boss and Successor are captured.
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Vga.Black.copy(alpha = 0.82f))
                        .padding(horizontal = v.w(3), vertical = v.w(1)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${completedFamilies.size}/${Masterminds.familyOrder.size}",
                        style = v.text(6f, color = Vga.Yellow, bold = true))
                    Masterminds.familyOrder.forEach { family ->
                        val earned = family in completedFamilies
                        val asset = Masterminds.familyStampAssets.getValue(family)
                        val familyLabel = Strings.label("mastermind.family", family)
                        Box(
                            Modifier.size(v.w(17)).padding(v.w(0.6f))
                                .border(BorderStroke(v.w(0.5f), if (earned) Vga.Yellow else Vga.DarkGray)),
                            contentAlignment = Alignment.Center,
                        ) {
                            PixelImage(
                                asset,
                                Modifier.fillMaxSize().alpha(if (earned) 1f else 0.32f),
                                ContentScale.Fit,
                                contentDescription = if (earned) familyLabel
                                    else Strings.ui("{0}, locked", familyLabel),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(v.w(3)))

            // the stamps: visited countries by name (the collection, also for accessibility)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (visitedCountries.isEmpty()) {
                    Text(Strings.ui("No stamps yet — solve a case to start filling your passport."),
                        style = v.text(7, color = Vga.LightGray))
                } else {
                    visitedCountries
                        .map { Strings.opt("country.$it") ?: CountryShapes.countryName[it] ?: it }
                        .sorted()
                        .forEach { name ->
                            Text("✓ ${name.uppercase()}",
                                style = v.text(7, color = Vga.LightGreen, bold = true),
                                modifier = Modifier.fillMaxWidth().padding(vertical = v.w(0.5f)))
                        }
                }
            }
            Spacer(Modifier.height(v.w(3)))
            if (!paid && com.acme.clara.billing.BillingManager.SALES_ENABLED) {
                DosButton(Strings.ui("Unlock World Campaign"), fill = Vga.LightGreen,
                    textColor = Vga.Black, style = v.text(8, bold = true)) {
                    vm.openOverlay(Overlay.PurchaseOffer("Passport locked map"))
                }
                Spacer(Modifier.height(v.w(2)))
            }
            DosButton(Strings.ui("CLOSE"), fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

/* The World Campaign purchase dialog — the one purchase this app sells, reachable from the
 * persistent Campaign menu entry, the locked Passport, and a locked World Database entry, plus
 * the prompt right after Case 14. Every entry point opens the exact same dialog; [Overlay
 * .PurchaseOffer.source] only tags which one for analytics. */
@Composable
private fun PurchaseOfferWindow(v: Virtual, vm: ClaraViewModel) {
    val activity = LocalContext.current as? android.app.Activity
    var details by remember { mutableStateOf<com.android.billingclient.api.ProductDetails?>(null) }
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        com.acme.clara.billing.BillingManager.queryProductDetails { d -> details = d; checked = true }
    }
    val price = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.9f).background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))
                .padding(v.w(6)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(Strings.ui("OPTIONAL ONE-TIME PURCHASE"), style = v.text(5.5f, color = Vga.Black, bold = true),
                modifier = Modifier.background(Vga.Yellow).padding(horizontal = v.w(3), vertical = v.w(1)))
            Spacer(Modifier.height(v.w(3)))
            Text(Strings.ui("INTERNATIONAL CAMPAIGN"), style = v.text(10, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(2)))
            Text(
                Strings.ui("Dismantle Clara's five crime families, one wave at a time — and finally catch her."),
                style = v.text(7, color = Vga.LightCyan), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(v.w(4)))
            listOf(
                Strings.ui("Finish the story: five crime families, then Clara herself"),
                Strings.ui("201 new places to explore, unlocked wave by wave"),
                Strings.ui("Extra comforts: +8h buffer, a hint, trip planner"),
                Strings.ui("Buy once: no subscription — progress always saved"),
            ).forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = v.w(0.7f))) {
                    Text("◆ ", style = v.text(7, color = Vga.Yellow, bold = true))
                    Text(line, style = v.text(7, color = Vga.White))
                }
            }
            Spacer(Modifier.height(v.w(4)))
            when {
                !checked -> Text(Strings.ui("Checking Google Play..."), style = v.text(7, color = Vga.LightGray))
                details == null -> Text(Strings.ui("Not available yet — check back soon."), style = v.text(7, color = Vga.LightGray))
                else -> DosButton(
                    Strings.ui("Unlock World Campaign — {0}", price ?: ""), fill = Vga.Yellow, textColor = Vga.Black,
                    style = v.text(8, bold = true),
                ) {
                    val a = activity; val d = details
                    if (a != null && d != null) com.acme.clara.billing.BillingManager.launchPurchase(a, d)
                }
            }
            Spacer(Modifier.height(v.w(2)))
            DosButton(Strings.ui("NOT NOW"), fill = Vga.LightGray, textColor = Vga.Black,
                style = v.text(8, bold = true)) { vm.dismissOverlay() }
            Spacer(Modifier.height(v.w(2)))
            Text(
                Strings.ui("Restore purchase"), style = v.text(6.5f, color = Vga.LightCyan),
                modifier = Modifier.clickable { com.acme.clara.billing.BillingManager.queryExistingPurchases() }
                    .tappable(Strings.ui("Restore purchase")).padding(v.w(1)),
            )
        }
    }
}

/** Answer-safe paid comfort tool: compare time cost and arrival before committing to a flight. */
@Composable
private fun CasePlannerWindow(v: Virtual, vm: ClaraViewModel) {
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.65f)).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.82f).background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(6)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Strings.ui("CASE PLANNER"), style = v.text(10, color = Vga.Yellow, bold = true))
            Text(Strings.ui("Compare the current routes. The correct lead is never revealed."),
                style = v.text(6.5f, color = Vga.LightCyan), textAlign = TextAlign.Center)
            Spacer(Modifier.height(v.w(4)))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                vm.s.departOptions.forEach { city ->
                    val hours = vm.flightHoursTo(city)
                    Row(Modifier.fillMaxWidth().padding(vertical = v.w(1.5f)),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Strings.place(city), style = v.text(7.5f, color = Vga.White, bold = true))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Strings.ui("{0}h flight", hours), style = v.text(7, color = Vga.Yellow))
                            Text(vm.arrivalClockLabel(hours), style = v.text(6, color = Vga.LightCyan))
                        }
                    }
                }
                if (vm.s.departOptions.isEmpty()) {
                    Text(Strings.ui("Open the planner during an active investigation."),
                        style = v.text(7, color = Vga.LightGray), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }
            }
            DosButton(Strings.ui("CLOSE"), fill = Vga.Green, textColor = Vga.White,
                style = v.text(9, bold = true)) { vm.dismissOverlay() }
        }
    }
}

/* Confirms the purchase and the immediate Wave 1 value before receipt-only silence sets in. */
@Composable
private fun UnlockCeremonyWindow(v: Virtual, vm: ClaraViewModel) {
    val waveOnePlaces = com.acme.clara.data.Progression.wave.values.count { it == 0 }
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.88f).background(Vga.Black).border(BorderStroke(v.w(1.5f), Vga.Yellow))
                .padding(v.w(7)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(Strings.ui("YOU'RE IN"), style = v.text(6, color = Vga.Black, bold = true),
                modifier = Modifier.background(Vga.Yellow).padding(horizontal = v.w(3), vertical = v.w(1)))
            Spacer(Modifier.height(v.w(3)))
            Text(Strings.ui("International Campaign unlocked"), style = v.text(9.5f, color = Vga.Yellow, bold = true),
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(v.w(4)))
            listOf(
                Strings.ui("Wave 1 is open now — {0} new destinations", waveOnePlaces),
                Strings.ui("Passport stamps and Wave 1 Database files are ready"),
                Strings.ui("Comfort perks are active: optional +8h buffer, one Bureau hint, Case Planner"),
                Strings.ui("Clara's network story begins after Case 14"),
            ).forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = v.w(0.7f))) {
                    Text("◆ ", style = v.text(7, color = Vga.Yellow, bold = true))
                    Text(line, style = v.text(7, color = Vga.White))
                }
            }
            Spacer(Modifier.height(v.w(4)))
            DosButton(Strings.ui("CONTINUE"), fill = Vga.Yellow, textColor = Vga.Black,
                style = v.text(8.5f, bold = true)) { vm.dismissOverlay() }
        }
    }
}
