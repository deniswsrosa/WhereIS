package com.acme.clara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.acme.clara.data.GameData
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.i18n.Strings
import com.acme.clara.save.SaveMeta
import com.acme.clara.ui.At
import com.acme.clara.ui.PixelImage
import com.acme.clara.ui.Virtual
import com.acme.clara.ui.VirtualScreen
import com.acme.clara.ui.labelled
import com.acme.clara.ui.tappable
import com.acme.clara.ui.theme.Vga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The "Choose a detective" picker — shown at launch when two or more careers exist. It's the WDB
 * bureau itself: the office backdrop, a case file per detective (their agency ID photo, name, rank
 * and a case-progress meter). Tapping a file resumes the career; its ✕ retires the agent; the button
 * hires a new one. Composed like the rest of the game — panels over a pixel-art scene, VGA palette.
 */
@Composable
fun ChooseGameScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // savedGames()/resumeById() read + JSON-decode every save file on disk (SaveStore.list()/
    // load()) — off the main thread here, same reasoning as MainActivity's cold-start read.
    // deleteGame() itself stays synchronous (see ClaraViewModel kdoc), so by the time a delete
    // bumps `refresh` the file is already gone and this re-fetch correctly won't see it.
    var games by remember { mutableStateOf(emptyList<SaveMeta>()) }
    LaunchedEffect(refresh) {
        games = withContext(Dispatchers.IO) { vm.savedGames() }
        loading = false
    }

    // The bureau office, dimmed so the dossiers read over it (same scene the sign-on uses).
    PixelImage("intro_world_detective_bureau", Modifier.fillMaxSize(), ContentScale.FillBounds)
    Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.62f)))

    v.At(0, 0, 320, 200) {
        Column(Modifier.fillMaxSize().padding(v.w(10))) {
            // ---- title, in the game's black / white-bordered box idiom ----
            Box(
                Modifier.fillMaxWidth().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.padding(vertical = v.w(3)), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Strings.ui("SELECT YOUR DETECTIVE"), style = v.text(10, Vga.Yellow, bold = true))
                    Text(Strings.ui("Tap a file to resume  ·  ✕ to retire"), style = v.text(6.5f, Vga.LightCyan),
                        modifier = Modifier.padding(top = v.w(1)))
                }
            }
            Spacer(Modifier.height(v.w(6)))

            // ---- the case files (scrolls if there are many) ----
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(v.w(5)),
            ) {
                if (games.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(v.w(48)), contentAlignment = Alignment.Center) {
                        Text(Strings.ui("No saved games yet."), style = v.text(8, Vga.LightCyan))
                    }
                }
                games.forEach { m ->
                    DossierCard(v, m,
                        onResume = {
                            if (!loading) {
                                loading = true
                                scope.launch {
                                    val data = withContext(Dispatchers.IO) { vm.savedGame(m.id) }
                                    if (data != null) vm.resume(data) else { loading = false; refresh++ }
                                }
                            }
                        },
                        onDelete = {
                            if (!loading) {
                                loading = true
                                vm.deleteGame(m.id)
                                refresh++
                            }
                        })
                }
            }

            Spacer(Modifier.height(v.w(6)))
            Box(Modifier.fillMaxWidth().height(v.w(22)), contentAlignment = Alignment.Center) {
                Box(Modifier.width(v.w(170)).height(v.w(22))) {
                    DosButton(v, "+  " + Strings.ui("NEW DETECTIVE")) {
                        if (!loading) vm.newGameFlow()
                    }
                }
            }
        }
    }
}

/** One saved career as an agency case file: ID photo (the detective sprite), name, rank, progress. */
@Composable
private fun DossierCard(v: Virtual, m: SaveMeta, onResume: () -> Unit, onDelete: () -> Unit) {
    val rank = Strings.label("rank", GameData.ranks.getOrElse(m.rankIndex) { "Rookie" })
    Box(
        Modifier.fillMaxWidth().height(v.w(48)).background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White))
            .clickable(onClick = onResume).labelled(Strings.ui("Continue {0}", m.name)).padding(v.w(5)),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // agency ID photo: the detective sprite on a badge field
            Box(
                Modifier.size(v.w(30), v.w(38)).background(Vga.Blue).border(BorderStroke(v.w(1), Vga.Yellow)),
                contentAlignment = Alignment.BottomCenter,
            ) { PixelImage("anim_detective_0", Modifier.fillMaxSize().padding(v.w(2)), ContentScale.Fit) }

            Spacer(Modifier.width(v.w(7)))

            Column(Modifier.weight(1f)) {
                Text(m.name, style = v.text(12, Vga.White, bold = true),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(rank, style = v.text(8, Vga.Yellow), modifier = Modifier.padding(top = v.w(1)))
                Spacer(Modifier.height(v.w(2)))
                CaseMeter(v, m.casesSolved)
            }

            Spacer(Modifier.width(v.w(5)))
            Box(
                Modifier.size(v.w(20)).border(BorderStroke(v.w(1), Vga.Red))
                    .clickable(onClick = onDelete).tappable(Strings.ui("Retire {0}", m.name)),
                contentAlignment = Alignment.Center,
            ) { Text("✕", style = v.text(10, Vga.Red, bold = true)) }
        }
    }
}

/** A compact "N solved" line with a filled/empty meter across the 14-case career. */
@Composable
private fun CaseMeter(v: Virtual, cases: Int) {
    val career = 14
    val filled = cases.coerceIn(0, career)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(Strings.ui("{0} solved", cases), style = v.text(6.5f, Vga.LightCyan))
        Spacer(Modifier.width(v.w(4)))
        Row(horizontalArrangement = Arrangement.spacedBy(v.w(0.8f))) {
            repeat(career) { i ->
                Box(Modifier.size(v.w(3.2f), v.w(4.4f))
                    .background(if (i < filled) Vga.LightGreen else Vga.DarkGray)
                    .border(BorderStroke(v.w(0.4f), Vga.Black)))
            }
        }
    }
}

/** DOS dialog button: yellow fill, black frame + hard drop shadow, bold red label (matches the game). */
@Composable
private fun DosButton(v: Virtual, label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(start = v.w(1.5f), top = v.w(1.5f)).background(Vga.Black))
        Box(
            Modifier.fillMaxSize().padding(end = v.w(1.5f), bottom = v.w(1.5f))
                .background(Vga.Yellow).border(BorderStroke(v.w(1), Vga.Black))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = onClick,
                ).labelled(label),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = v.text(9, Vga.Red, bold = true)) }
    }
}
