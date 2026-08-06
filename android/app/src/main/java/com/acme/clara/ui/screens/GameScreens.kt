package com.acme.clara.ui.screens

import com.acme.clara.audio.GameSound
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.acme.clara.game.Venue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.acme.clara.data.CityMeta
import com.acme.clara.data.GameData
import com.acme.clara.data.Progression
import com.acme.clara.data.WorldMap
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.i18n.Strings
import com.acme.clara.ui.*
import com.acme.clara.ui.theme.Vga

/* ----------------------------- INTRO (boot animation) -----------------------------
 * Faithful to the original attract sequence, each stage on its own screen:
 * 1. three crowns + "Brøderbund Software Presents" alone · 2. black screen, the detective
 * walks across near the bottom · 3. black screen, the police squad marches through ·
 * 4. the World Detective Bureau scene ("Clara's gang has pulled another caper!").
 * Tap anywhere to skip to the title.
 */
@Composable
fun IntroScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    var stage by remember { mutableIntStateOf(0) }   // 0 crowns · 1 detective · 2 cops · 3 WDB
    var walkX by remember { mutableFloatStateOf(-60f) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(3500)                                    // crowns card, nothing else on screen
        stage = 1; walkX = -40f
        while (walkX < 330f) { delay(60); walkX += 4f; frame++ }
        stage = 2; walkX = -60f
        while (walkX < 330f) { delay(60); walkX += 5f; frame++ }
        stage = 3                                      // World Detective Bureau scene
        delay(4000)
        vm.introDone()
    }
    when (stage) {
        // captions were baked into the DOS captures with the original's wording; the
        // bitmaps are wiped clean and the (reworded) lines drawn at runtime instead
        0 -> {
            // studio logo card: DENIX INC on a clean white background
            Box(Modifier.fillMaxSize().background(Vga.White))
            PixelImage("logo_denix", Modifier.fillMaxSize())
        }
        3 -> {
            PixelImage("intro_world_detective_bureau", Modifier.fillMaxSize())
            v.At(0, 11, 320, 11, Alignment.Center) {
                Text(Strings.ui("Clara's gang has struck again!"),
                    style = v.text(7, color = Vga.LightRed, bold = true))
            }
            v.At(0, 184, 320, 12, Alignment.Center) {
                Text(Strings.ui("and cracking the case is up to you..."),
                    style = v.text(7, color = Vga.White))
            }
        }
        else -> {
            Box(Modifier.fillMaxSize().background(Vga.Black))
            val sprite = if (stage == 2) "anim_cops_${frame % 3}" else "anim_detective_${frame % 2}"
            val w = if (stage == 2) 49f else 34f
            v.At(walkX, 151, w, 48, Alignment.BottomCenter) {
                PixelImage(sprite, Modifier.fillMaxSize(), ContentScale.Fit)
            }
        }
    }
    Box(Modifier.fillMaxSize().clickable { vm.introDone() })
}

/* ----------------------------- TITLE ----------------------------- */
@Composable
fun TitleScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    // the whole screen advances, like the original's "any key or button"
    Box(Modifier.fillMaxSize().clickable { vm.start() }) {
        PixelImage("title_screen", Modifier.fillMaxSize())
    }
    v.At(0, 176, 320, 24, Alignment.Center) {
        Text(Strings.ui("PRESS  ANY  KEY  TO  BEGIN"), style = v.text(9, color = Vga.Yellow, bold = true))
    }
}

/* --------------------------- HQ PRINTER (sign-on + briefing) ---------------------------
 * Faithful to the original: the WDB HQ dot-matrix printer prints its messages onto one
 * continuous sheet. On the first case it asks you to identify yourself, then — without ever
 * switching to a different screen — keeps printing the case briefing on the same paper.
 * The `hq_screen` art (printer body, header, sprocket-feed paper) is reused; we cover the
 * baked-in prompt with a clean sheet and drive our own teletype so it can type + scroll.
 */

// Interior of the white paper sheet in the 320x200 canvas (measured from hq_screen.png:
// the sheet's left edge line is x=22, its interior x=23..120, y≈97..146; the printer body
// begins at y≈148). The overlay starts just inside the edge line (keeping it visible) and
// runs to the sheet's bottom so it also covers the baked-in paper-guide marks at x24..29.
private const val PAPER_X = 23f
private const val PAPER_Y = 98f
private const val PAPER_W = 97f
private const val PAPER_H = 49f

private val SIGN_ON_PROMPT get() = listOf(Strings.ui("Detective on duty, please enter your name:"))

@Composable
fun SignOnScreen(vm: ClaraViewModel) = HqPrinterScreen(vm, promptForName = true) { vm.beginInvestigation() }

@Composable
fun BriefingScreen(vm: ClaraViewModel) = HqPrinterScreen(vm, promptForName = false) { vm.beginInvestigation() }

/* Sign-on + briefing stages, following the original beat for beat:
 * identify yourself → (new name) "There is no record ... Are you new here? (Y/N)" with
 * Yes/No buttons → "You have been identified ... Your current rank is Rookie." →
 * FLASH / treasure segment → assignment / deadline segment → city. Between segments the
 * game waits with "Press any key or button to continue." printed under the right panel. */
private const val ST_PROMPT = 0      // typing the identify-yourself prompt
private const val ST_NAME = 1        // awaiting name input
private const val ST_NEW_Q = 2       // typing the no-record + are-you-new question
private const val ST_YESNO = 3       // waiting on the Yes/No buttons
private const val ST_IDENT = 4       // typing the identified + rank lines
private const val ST_GATE1 = 5       // press any key -> flash segment
private const val ST_FLASH = 6       // typing FLASH + treasure
private const val ST_GATE2 = 7       // press any key -> assignment segment
private const val ST_ASSIGN = 8      // typing assignment + deadline
private const val ST_BEGIN = 9       // press any key -> investigation

@Composable
private fun HqPrinterScreen(vm: ClaraViewModel, promptForName: Boolean, onBegin: () -> Unit) =
    // While the name is being typed, keep the paper's bottom edge above the keyboard by panning
    // the whole (full-size) scene up — the scene never shrinks.
    VirtualScreen(keepVirtualYAboveIme = PAPER_Y + PAPER_H) { v ->
    val printed = remember { mutableStateListOf<String>() }
    var typing by remember { mutableStateOf("") }
    var stage by remember { mutableIntStateOf(if (promptForName) ST_PROMPT else ST_FLASH) }
    var input by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    suspend fun typeLines(lines: List<String>) {
        var n = 0
        // The paper sheet is short, so wrap every phrase to its width and roll ONE line at a time —
        // otherwise a long phrase types as a single tall block and its first rows scroll off the top
        // before it finishes. Blank strings are kept as-is (they're intentional spacer lines).
        for (raw in lines) for (ln in if (raw.isBlank()) listOf(raw) else paperWrap(raw, 22)) {
            typing = ""
            for (ch in ln) {
                typing += ch
                // dot-matrix clatter: a click on every other character (skipping spaces)
                if (!ch.isWhitespace() && n++ % 2 == 0) GameSound.typeClick(ctx)
                delay(16)
            }
            printed.add(ln); typing = ""
            delay(110)
        }
    }

    LaunchedEffect(stage) {
        when (stage) {
            ST_PROMPT -> { typeLines(SIGN_ON_PROMPT); stage = ST_NAME }
            ST_NEW_Q -> {
                typeLines(listOf("", Strings.ui("Interpol has no file under that name."),
                    "", Strings.ui("First day on the job? (Y/N)")))
                stage = ST_YESNO
            }
            ST_IDENT -> {
                typeLines(listOf("", Strings.ui("Identity confirmed, {0}.", vm.s.detectiveName),
                    "", Strings.ui("You currently hold the rank of {0}.",
                        Strings.label("rank", GameData.ranks[vm.s.rankIndex]))))
                stage = ST_GATE1
            }
            ST_FLASH -> {
                val s = vm.s
                typeLines(listOf(GameData.FLASH, "",
                    GameData.TREASURE_STOLEN.replace("%s", Strings.place(s.currentCity)), "",
                    GameData.TREASURE_ID.replace("%s", com.acme.clara.game.Treasures.localized(s.treasure))))
                stage = ST_GATE2
            }
            ST_ASSIGN -> {
                val s = vm.s
                val assignment = (if (s.culprit?.sex == "Female") GameData.ASSIGNMENT_F else GameData.ASSIGNMENT_M)
                    .replaceFirst("%s", Strings.place(s.currentCity))
                typeLines(listOf("", Strings.ui("Your assignment:"), assignment,
                    "", GameData.DEADLINE))
                stage = ST_BEGIN
            }
        }
    }
    // Snap (not animate) to the bottom: a new LaunchedEffect fires on every keystroke of the
    // typewriter effect (every ~16ms), far faster than an animated scroll can settle — so the
    // animated version always lagged behind, leaving the line actually being typed below the
    // visible paper until it finished and the scroll caught up.
    LaunchedEffect(printed.size, typing, input, stage) { scroll.scrollTo(scroll.maxValue) }
    // Auto-focus the name field and raise the soft keyboard so the player can just start
    // typing (requestFocus alone doesn't reliably show the IME; the small delay lets the
    // field finish composing first).
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(stage) {
        if (stage == ST_NAME) { delay(60); focus.requestFocus(); keyboard?.show() }
    }

    val paperFont = v.text(7, color = Vga.Black)

    PixelImage("hq_screen", Modifier.fillMaxSize())
    // The header box's "HEADQUARTERS / Monday, 9 a.m." used to be baked into hq_screen.png;
    // now drawn at runtime (translatable, matching the same box style CityClockBox uses
    // elsewhere) — a fresh case's clock is always Monday 9 a.m., so clockLabel() just works.
    v.At(4, 13, 141, 30, Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(2)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Strings.ui("HEADQUARTERS"), style = v.text(9, color = Vga.White, bold = true))
                Text(vm.clockLabel(), style = v.text(8, color = Vga.White))
            }
        }
    }
    // gate stages: whole screen tappable to advance (like "press any key or button")
    when (stage) {
        ST_GATE1 -> Box(Modifier.fillMaxSize().clickable { stage = ST_FLASH })
        ST_GATE2 -> Box(Modifier.fillMaxSize().clickable { stage = ST_ASSIGN })
        ST_BEGIN -> Box(Modifier.fillMaxSize().clickable { onBegin() })
    }
    // Ratcheted: the sheet grows from a small starting size (paper feeding out of the printer)
    // and freezes for good once it reaches the cap — same growth beat as the Result screen's
    // printer. Real scrolling (below) handles anything past that; this height is purely visual.
    var peakRows by remember { mutableIntStateOf(0) }
    val liveRows = printed.size + (if (typing.isNotEmpty()) 1 else 0) + (if (stage == ST_NAME) 1 else 0)
    peakRows = maxOf(peakRows, liveRows).coerceAtMost(5)
    val paperGrownH = (6f + peakRows * 8.05f).coerceAtLeast(16f).coerceAtMost(PAPER_H)
    // Clean sheet overlay covering the baked prompt, hosting the live printout. Bottom edge stays
    // fixed (flush with the printer's front lip); the top edge is what moves as the sheet grows.
    v.At(PAPER_X, (PAPER_Y + PAPER_H) - paperGrownH, PAPER_W, paperGrownH) {
        Column(
            // Wider start padding so the printout clears the sprocket strip on the left.
            Modifier.fillMaxSize().background(Vga.White)
                .padding(start = v.w(4), end = v.w(2), top = v.w(1), bottom = v.w(1))
                .verticalScroll(scroll)
        ) {
            printed.forEach { Text(it, style = paperFont) }
            if (typing.isNotEmpty()) Text(typing, style = paperFont)
            if (stage == ST_NAME) BasicTextField(
                value = input,
                onValueChange = { input = it.take(18).filter { c -> c != '\n' } },
                singleLine = true,
                textStyle = paperFont,
                cursorBrush = SolidColor(Vga.Black),
                // flagNoExtractUi: keep the keystrokes on the printer paper instead of
                // Android's fullscreen landscape text editor.
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    platformImeOptions = PlatformImeOptions("flagNoExtractUi"),
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val nm = input.trim().ifBlank { "Gumshoe" }
                    printed.add(nm)
                    input = ""
                    vm.signOnStart(nm)   // generate the case, stay on the printer
                    stage = ST_NEW_Q
                }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            // Trailing gap so the freshly printed line always clears the printer's front lip.
            Spacer(Modifier.height(v.w(3)))
        }
    }
    // "Press any key or button to continue." under the right panel, like the original
    if (stage == ST_GATE1 || stage == ST_GATE2 || stage == ST_BEGIN) {
        v.At(150, 172, 166, 22, Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Strings.ui("Press any key or"), style = v.text(9.5f, color = Vga.Black, bold = true))
                Text(Strings.ui("button to continue."), style = v.text(9.5f, color = Vga.Black, bold = true))
            }
        }
    }
    // Yes / No buttons (yellow, red text) for "Are you new here?" — DOS geometry:
    // Yes At(152,176,76,11), No At(234,176,76,11) (dos_signon_yesno ref, ÷2)
    if (stage == ST_YESNO) {
        v.At(152, 176, 76, 12) {
            // the paper echoes the pressed answer's initial, like the DOS "Y"/"N" keypress
            YellowButton(v, Strings.ui("Yes")) { printed.add(Strings.ui("Yes").take(1).uppercase()); stage = ST_IDENT }
        }
        v.At(234, 176, 76, 12) {
            // "No" re-asks for the name, like the original
            YellowButton(v, Strings.ui("No")) { printed.add(Strings.ui("No").take(1).uppercase()); printed.add(""); stage = ST_PROMPT }
        }
    }
    // Menu bar, drawn last so it stays tappable above the gate stages' fullscreen
    // click-catchers (the art leaves the top strip blank for it, like every other screen).
    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }
    OverlayHost(v, vm)
}

/** DOS-style dialog button: yellow fill, 1px black border with a small drop shadow,
 *  bold red centred label. */
@Composable
private fun YellowButton(v: Virtual, label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().offset(v.w(1), v.w(1)).background(Vga.Black))
        Box(Modifier.matchParentSize().background(Vga.Yellow).border(BorderStroke(v.w(1), Vga.Black))
            .clickable(onClick = onClick).labelled(label), contentAlignment = Alignment.Center) {
            Text(label, style = v.text(9, color = Vga.Red, bold = true))
        }
    }
}

/** The country blurb at the top, with the approved "say-hello" line (from the welcome cards)
 *  pinned to the bottom of the panel in a smaller, greyed-out font so it reads as secondary
 *  "did you know" text (e.g. In Arabic, hello is "Salaam" (sah-LAAM).). */
@Composable
private fun CountryText(v: Virtual, info: com.acme.clara.data.CityInfo, paid: Boolean = false) {
    // The secondary grey line usually teaches the local greeting, but ~30% of arrivals crack a
    // joke instead. Rolled once per city so it stays put across recompositions.
    val bottom = remember(info.name) {
        val funny = if (kotlin.random.Random.nextInt(100) < 30) com.acme.clara.game.Humor.arrivalLine(paid) else null
        funny ?: info.greeting
    }
    Column(Modifier.fillMaxSize()) {
        Text(info.description, style = v.text(8.5f, color = Vga.White))
        if (bottom != null) {
            Spacer(Modifier.weight(1f))
            Text(bottom, style = v.text(7f, color = Vga.LightGray))
        }
    }
}

/* ----------------------------- CITY ----------------------------- */
@Composable
fun CityPhoto(city: String, v: Virtual, modifier: Modifier) {
    val info = CityMeta.of(city)
    // Resolve the briefing postcard: the explicit drawable first, then derive city_<slug> /
    // country_<slug> from the name (many expansion cities ship a sprite but leave drawable null),
    // and only if none exist fall back to the procedural VGA card rather than a blank box.
    val slug = snake(city)
    val resolved = listOfNotNull(info.drawable, "city_$slug", "country_$slug").firstOrNull { spriteExists(it) }
    if (resolved != null) PixelImage(resolved, modifier)
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
            Text(Strings.place(city), style = v.text(9, color = Vga.White, bold = true), textAlign = TextAlign.Center)
        }
    }
}

/** The black white-bordered city name / date box at the top-left, shared by every in-game
 *  screen. When the overnight clamp fired the title reads "SLEEPING…" for a moment
 *  (dos_sleeping_overnight.png), then reverts to the city name. */
@Composable
fun CityClockBox(v: Virtual, vm: ClaraViewModel, tickHours: Int = 0) {
    val s = vm.s
    val sleeping = s.sleeping
    LaunchedEffect(sleeping) {
        if (sleeping) { delay(1800); vm.sleepingShown() }
    }
    // P1 trail heat: "CITY n/m" + a meter that warms cool→hot as you close on the hideout.
    // The distance already exists (it drives the sighting stings); this just surfaces it.
    val total = s.route.size
    val inCase = total > 1 && s.currentCity.isNotBlank() && !sleeping
    val cityNo = (s.progress + 1).coerceIn(1, total.coerceAtLeast(1))
    val heat = if (s.onTrack && total > 1) (s.progress.toFloat() / (total - 1)).coerceIn(0f, 1f) else 0f
    v.At(4, 13, 141, 30, Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(2)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (sleeping) Strings.ui("SLEEPING…") else Strings.place(s.currentCity),
                        style = v.text(9, color = Vga.White, bold = true))
                    if (inCase) {
                        Spacer(Modifier.width(v.w(3)))
                        Text(Strings.ui("CITY {0}/{1}", cityNo, total), style = v.text(6f, color = Vga.Yellow, bold = true))
                    }
                }
                Text(vm.clockLabel(tickHours), style = v.text(8, color = Vga.White))
                // deadline hint (remake aid): dim normally, red in the last day
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val left = vm.hoursLeft() - tickHours
                    Text(vm.deadlineLabel(tickHours),
                        style = v.text(6.5f, color = if (left in 0..24) Vga.Red else Vga.LightGray))
                    if (inCase && s.onTrack) {
                        Spacer(Modifier.width(v.w(3)))
                        TrailMeter(v, heat)
                    }
                }
            }
        }
    }
}

/** P1 goal-gradient meter: four blocks warming blue→red as the trail heats up. */
@Composable
private fun TrailMeter(v: Virtual, heat: Float) {
    val blocks = 4
    val filled = (heat * blocks).roundToInt().coerceIn(0, blocks)
    val hot = when {
        heat >= 0.8f -> Vga.Red
        heat >= 0.5f -> Vga.Yellow
        heat > 0f -> Vga.LightCyan
        else -> Vga.LightGray
    }
    Row(horizontalArrangement = Arrangement.spacedBy(v.w(0.6f))) {
        repeat(blocks) { i ->
            Box(Modifier.size(v.w(3f), v.w(4f))
                .background(if (i < filled) hot else Vga.DarkGray))
        }
    }
}

@Composable
fun CityScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    val s = vm.s
    val info = CityMeta.of(s.currentCity)
    var showVenues by remember(s.currentCity, s.progress) { mutableStateOf(false) }
    // SEE dropdown open: the city box is replaced by the connections list, SEE reads HIDE
    var seeOpen by remember(s.currentCity) { mutableStateOf(false) }
    // walking-to-venue animation: index of the venue being walked to, -1 = none
    var walkingTo by remember(s.currentCity) { mutableIntStateOf(-1) }
    var walkStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(walkingTo) {
        if (walkingTo >= 0) {
            walkStep = 0
            while (walkStep < 8) { delay(140); walkStep++ }   // footsteps march toward the door
            val t = walkingTo; walkingTo = -1; showVenues = false
            vm.openVenue(t)
        }
    }

    // menu bar
    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }

    if (!seeOpen) CityClockBox(v, vm)
    // city photo
    v.At(4, 45, 141, 148) {
        Box(Modifier.fillMaxSize().border(BorderStroke(v.w(1), Vga.White))) {
            CityPhoto(s.currentCity, v, Modifier.fillMaxSize())
        }
    }
    // while SEE is active the connections dropdown replaces the city box, drawn over the
    // top of the photo (like the original)
    if (seeOpen) SeeDropdown(v, s.currentCity, s.departOptions) { seeOpen = false }
    // right panel: the city description; while investigating, first the sighting
    // interstitial (if any), then the witness you're talking to
    if (s.openClue != null) {
        if (s.sightingLevel > 0) {
            SightingPanel(v, s.sightingLevel) { vm.sightingDone() }
        } else {
            WitnessPanel(v, s.openClue!!) { vm.closeClue() }
        }
    } else {
        v.At(149, 13, 167, 145) {
            Box(Modifier.fillMaxSize().background(Vga.Black)
                .border(BorderStroke(v.w(1), Vga.White)).padding(v.w(4))) {
                if (s.onTrack) CountryText(v, info, vm.s.expansionUnlocked)
                else Text(Strings.ui("You look around. Nothing here seems out of the ordinary..."),
                    style = v.text(8.5f, color = Vga.White))
            }
        }
    }
    // Tapping a tool while a witness is on screen dismisses that witness first, so the player can
    // go straight from a testimony to the venue list (or SEE) without an extra tap to close it.
    GameToolbar(v, vm, seeLabel = if (seeOpen) Strings.ui("HIDE") else null,
        onSee = { vm.selectTool(0); vm.closeClue(); seeOpen = !seeOpen },
        onInvestigate = { vm.selectTool(2); vm.closeClue(); showVenues = true })

    // Investigate: pick one of 3 locations, shown as buildings + names (matches the original picker)
    if (showVenues && s.openClue == null) {
        InvestigatePicker(v, s.venues, s.visited, walkingTo, walkStep,
            onPick = { i -> if (walkingTo < 0) walkingTo = i },
            onCancel = { if (walkingTo < 0) showVenues = false })
    }
    OverlayHost(v, vm)
    com.acme.clara.ui.Tour(v, vm, suppressed = showVenues || walkingTo >= 0 || seeOpen)
}

/** The SEE tool's dropdown (dos_see_dropdown_open_hide_icon.png): it replaces the city/date
 *  box — black, white border, current city on top, then an inner double-bordered list of the
 *  connection cities with the first on the white selection bar. Any tap closes it. */
@Composable
private fun SeeDropdown(v: Virtual, city: String, connections: List<String>, onClose: () -> Unit) {
    v.At(4, 13, 141, 18f + connections.size * 10f + 6f) {
        Column(Modifier.fillMaxSize().background(Vga.Black)
            .border(BorderStroke(v.w(1), Vga.White)).clickable(onClick = onClose)) {
            Box(Modifier.fillMaxWidth().height(v.w(14)), contentAlignment = Alignment.Center) {
                Text(city, style = v.text(9, color = Vga.White, bold = true))
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = v.w(3))
                .border(BorderStroke(v.w(2), Vga.White)).padding(v.w(1))) {
                Column(Modifier.fillMaxWidth()) {
                    connections.forEachIndexed { i, c ->
                        Box(Modifier.fillMaxWidth().height(v.w(10))
                            .then(if (i == 0) Modifier.background(Vga.White) else Modifier),
                            contentAlignment = Alignment.Center) {
                            Text(c, style = v.text(8.5f, color = if (i == 0) Vga.Black else Vga.White, bold = true))
                        }
                    }
                }
            }
        }
    }
}

/** The sighting interstitial in the black right panel, escalating as you close in
 *  (§6 ladder): 1 masked face rises from the bottom edge · 2 striped-shirt thug pops up and
 *  shakes · 3 burglar with the loot sack peeks in from the right, then runs across ·
 *  4 the hideout dagger flies across. Tap to skip. */
@Composable
private fun SightingPanel(v: Virtual, level: Int, onDone: () -> Unit) {
    // generic animation parameter 0..1 driven per level
    var t by remember(level) { mutableFloatStateOf(0f) }
    var phase by remember(level) { mutableIntStateOf(0) }   // level 3: 0 peek · 1 run
    var frame by remember(level) { mutableIntStateOf(0) }
    LaunchedEffect(level) {
        when (level) {
            1 -> {  // face rises slowly, pauses, sinks back
                while (t < 1f) { delay(45); t += 0.04f }
                delay(700)
                while (t > 0f) { delay(45); t -= 0.04f }
            }
            2 -> {  // thug pops up, shakes, drops back
                while (t < 1f) { delay(25); t += 0.08f }
                repeat(24) { delay(60); frame++ }
                while (t > 0f) { delay(25); t -= 0.08f }
            }
            3 -> {  // burglar peeks from the right edge, then sprints right -> left
                while (t < 1f) { delay(50); t += 0.1f }
                delay(600)
                phase = 1; t = 0f
                while (t < 1f) { delay(40); t += 0.035f; frame++ }
            }
            4 -> {  // the dagger flies in from the right and sticks mid-panel
                while (t < 1f) { delay(25); t += 0.09f }
                delay(900)
            }
        }
        onDone()
    }
    v.At(149, 13, 167, 145) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))
            .clipToBounds().clickable(onClick = onDone)) {
            when (level) {
                1 -> PixelImage("sight_face",
                    Modifier.align(Alignment.BottomCenter).offset(0.dp, v.w(42 * (1f - t)))
                        .size(v.w(52), v.w(42)), ContentScale.Fit)
                // DOS refs (work/burglar_snapshots/practice_thug): the thug rises in the
                // panel's bottom-LEFT corner, not centred
                2 -> PixelImage("sight_thug",
                    Modifier.align(Alignment.BottomStart)
                        .offset(v.w(8 + if (t >= 1f) (if (frame % 2 == 0) -2f else 2f) else 0f), v.w(50 * (1f - t)))
                        .size(v.w(54), v.w(50)), ContentScale.Fit)
                3 -> {
                    if (phase == 0) PixelImage("sight_burglar_peek",
                        Modifier.align(Alignment.BottomEnd).offset(v.w(35 * (1f - t)), v.w(-4))
                            .size(v.w(35), v.w(51)), ContentScale.Fit)
                    // the original's run is a 2-pose leg cycle (~120 ms per pose), verified
                    // frame-by-frame from DOS captures (work/burglar_snapshots/appearance_*)
                    else PixelImage(
                        if ((frame / 3) % 2 == 0) "sight_burglar_run" else "sight_burglar_run_b",
                        Modifier.align(Alignment.BottomStart)
                            .offset(v.w(165 - 230 * t), v.w(-4))
                            .size(v.w(61), v.w(51)), ContentScale.Fit)
                }
                4 -> PixelImage("sight_dagger",
                    Modifier.align(Alignment.CenterStart).offset(v.w(150 * (1f - t) - 8f), 0.dp)
                        .size(v.w(58), v.w(16)), ContentScale.Fit)
            }
        }
    }
}

/** The authentic 4-icon toolbar strip (SEE · DEPART · INVESTIGATE · CRIME). The green
 *  selection border tracks the last activated tool (vm.s.selectedTool), like the original.
 *  While the SEE dropdown is open, `seeLabel` covers the SEE caption with "HIDE". */
@Composable
fun GameToolbar(v: Virtual, vm: ClaraViewModel, seeLabel: String? = null,
                onSee: (() -> Unit)? = null, onInvestigate: (() -> Unit)? = null) {
    val selected = vm.s.selectedTool
    // same x-extent as the right panel above it — in the original both strips share one width
    v.At(149, 163, 167, 32) {
        Box(Modifier.fillMaxSize()) {
            PixelImage("toolbar_bar", Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize()) {
                // During a guided step the tour disables the tools it isn't pointing at.
                fun ok(i: Int) = com.acme.clara.ui.tourAllowsTool(vm.s, i)
                ToolZone(Modifier.weight(1f), selected == 0, ok(0), v) { onSee?.invoke() }
                ToolZone(Modifier.weight(1f), selected == 1, ok(1), v) { vm.gotoTravel() }
                ToolZone(Modifier.weight(1f), selected == 2, ok(2), v) { onInvestigate?.invoke() }
                ToolZone(Modifier.weight(1f), selected == 3, ok(3), v) { vm.gotoCrime() }
            }
            // "HIDE" caption over the SEE icon while its dropdown is open (DOS behaviour)
            if (seeLabel != null) {
                v.At(10, 3, 26, 7, Alignment.Center) {
                    Box(Modifier.fillMaxSize().background(Vga.White), contentAlignment = Alignment.Center) {
                        Text(seeLabel, style = v.text(6, color = Vga.Black, bold = true))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolZone(modifier: Modifier, selected: Boolean, enabled: Boolean, v: Virtual, onClick: () -> Unit) {
    Box(
        modifier.fillMaxHeight()
            .then(if (selected) Modifier.border(BorderStroke(v.w(2), Vga.Green)) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // A disabled tool (locked by the tour) is dimmed so it reads as unavailable.
        if (!enabled) Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.5f)))
    }
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
/** "Sport Club" -> "sport_club" : maps a venue/occupation name to its drawable resource suffix. */
private val SLUG_REGEX = Regex("[^a-z0-9]+")
private fun snake(s: String) = s.lowercase().replace(SLUG_REGEX, "_").trim('_')

private data class Look(val hair: Color, val skin: Color, val shirt: Color, val style: Int)
private fun witnessLook(occupation: String): Look {
    val h = occupation.hashCode()
    val hair = listOf(Vga.Yellow, Vga.Brown, Vga.Black, Vga.LightGray, Vga.LightRed, Color(0xFF6B4A2A))[(h ushr 1).mod(6)]
    val skin = listOf(Color(0xFFF0C8A0), Color(0xFFE0A878), Color(0xFFC89058), Color(0xFF9C6B3F))[(h ushr 4).mod(4)]
    val shirt = listOf(Vga.LightRed, Vga.Cyan, Vga.Green, Vga.LightBlue, Vga.Magenta, Vga.Brown, Vga.LightGreen)[(h ushr 7).mod(7)]
    return Look(hair, skin, shirt, (h ushr 11).mod(4))   // style: 0 short · 1 full · 2 bald · 3 cap
}

/** Right-panel witness, matching the original (bud_palace_wit.png): the witness sprite sits
 *  in the panel's BOTTOM-LEFT corner with its occupation label in white caps directly under
 *  it at the panel's bottom edge, and the white rounded speech bubble sits to its RIGHT,
 *  vertically centred in the panel, tail pointing left toward the sprite. */
@Composable
private fun WitnessPanel(v: Virtual, clue: Venue, onDone: () -> Unit) {
    val look = witnessLook(clue.occupation)
    var shown by remember(clue.text) { mutableIntStateOf(0) }
    LaunchedEffect(clue.text) { shown = 0; while (shown < clue.text.length) { delay(20); shown++ } }
    val bob by rememberInfiniteTransition(label = "wb").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "wb")

    v.At(149, 13, 167, 145) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))
            .clickable { if (shown < clue.text.length) shown = clue.text.length else onDone() }) {
            // sprite + caps label pinned to the bottom-left like the original; everything
            // left-aligned so the label starts where the sprite's left edge starts
            Column(Modifier.align(Alignment.BottomStart).padding(start = v.w(6), bottom = v.w(3)),
                horizontalAlignment = Alignment.Start) {
                val portrait = "witness_" + snake(clue.occupation)
                // DOS witness bust ≈ 30-40 virtual wide × ~46 tall (measured from
                // dos_witness_waiter_layout). Fit the portrait inside that box so the
                // (taller-than-wide) art doesn't render oversized and eat the panel.
                if (spriteExists(portrait)) {
                    PixelImage(portrait, Modifier.size(v.w(40), v.w(48)), ContentScale.Fit,
                        alignment = Alignment.BottomStart)
                } else {
                    Canvas(Modifier.size(v.w(32), v.w(48))) {
                        drawBust(size.width, size.height, look, (bob - 0.5f) * size.height * 0.02f)
                    }
                }
                Text(Strings.label("occ", clue.occupation).uppercase(), style = v.text(7, color = Vga.White, bold = true),
                    modifier = Modifier.padding(top = v.w(2)))
            }
            // speech bubble to the sprite's right, vertically centred, tail pointing left
            Box(Modifier.align(Alignment.CenterStart).padding(start = v.w(55), end = v.w(4))) {
                Box {
                    Box(Modifier.background(Vga.White, RoundedCornerShape(v.w(6)))
                        .padding(horizontal = v.w(5), vertical = v.w(4))) {
                        Text(clue.text.take(shown), style = v.text(7.5f, color = Vga.Black))
                    }
                    // tail: triangle sticking out of the bubble's left edge toward the sprite
                    Canvas(Modifier.align(Alignment.CenterStart).offset(v.w(-7), v.w(4)).size(v.w(9), v.w(8))) {
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height * 0.9f); close()
                        }
                        drawPath(p, Vga.White)
                    }
                }
            }
        }
    }
}

/** Head-and-shoulders caricature facing right, in the DOS witness-portrait style: a large round
 *  head with an exaggerated nose over a collared shirt. Hair varies by look.style. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBust(w: Float, h: Float, look: Look, bob: Float) {
    val ink = Vga.Black
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f)
    // ---- shoulders + collar ----
    val shTop = h * 0.72f + bob
    drawRoundRect(look.shirt, topLeft = Offset(w * 0.02f, shTop), size = Size(w * 0.96f, h - shTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f, w * 0.18f))
    // collar V
    val collar = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.32f, shTop); lineTo(w * 0.50f, shTop + h * 0.10f); lineTo(w * 0.68f, shTop)
    }
    drawPath(collar, Vga.White, style = stroke)
    // ---- neck ----
    val headCx = w * 0.50f; val headCy = h * 0.36f + bob
    val hw = w * 0.34f; val hh = h * 0.30f
    drawRect(look.skin, topLeft = Offset(headCx - w * 0.10f, headCy + hh * 0.55f), size = Size(w * 0.20f, h * 0.16f))
    // ---- head (big round, facing slightly right) ----
    drawOval(look.skin, topLeft = Offset(headCx - hw, headCy - hh), size = Size(hw * 2, hh * 2))
    drawOval(ink, topLeft = Offset(headCx - hw, headCy - hh), size = Size(hw * 2, hh * 2), style = stroke)
    // ear (left)
    drawOval(look.skin, topLeft = Offset(headCx - hw * 1.06f, headCy - hh * 0.05f), size = Size(w * 0.10f, h * 0.09f))
    // exaggerated nose (right profile)
    val nose = androidx.compose.ui.graphics.Path().apply {
        moveTo(headCx + hw * 0.70f, headCy - hh * 0.10f)
        lineTo(headCx + hw * 1.12f, headCy + hh * 0.12f)
        lineTo(headCx + hw * 0.70f, headCy + hh * 0.24f)
    }
    drawPath(nose, look.skin); drawPath(nose, ink, style = stroke)
    // eyebrow + eye
    drawRect(ink, topLeft = Offset(headCx + hw * 0.12f, headCy - hh * 0.44f), size = Size(w * 0.14f, h * 0.018f))
    drawOval(ink, topLeft = Offset(headCx + hw * 0.20f, headCy - hh * 0.30f), size = Size(w * 0.055f, h * 0.06f))
    // smiling mouth
    drawArc(ink, 20f, 50f, false, topLeft = Offset(headCx + hw * 0.02f, headCy - hh * 0.02f),
        size = Size(hw * 1.0f, hh * 0.9f), style = stroke)
    // ---- hair by style ----
    when (look.style) {
        2 -> {}  // bald: just a fringe over the ear
        3 -> drawRect(look.hair, topLeft = Offset(headCx - hw * 1.02f, headCy - hh * 1.12f), size = Size(hw * 2.0f, hh * 0.55f)) // cap
        else -> {
            // hair sweeping over the crown and down the back-left
            drawArc(look.hair, 160f, 230f, true,
                topLeft = Offset(headCx - hw * 1.02f, headCy - hh * 1.12f), size = Size(hw * 2.0f, hh * 1.7f))
            if (look.style == 1)  // fuller: sideburn down the left
                drawRect(look.hair, topLeft = Offset(headCx - hw * 1.0f, headCy - hh * 0.2f), size = Size(w * 0.08f, hh * 1.1f))
        }
    }
}

/** Investigation picker, faithful to the original dialog: a black white-bordered box at
 *  (70,71)-(250,176) with the three buildings on a shared baseline and the location names
 *  listed under them (selected name = white bar). Tapping a building/name starts the
 *  footsteps walk animation, then the visit. Tap outside the dialog to cancel. */
@Composable
private fun InvestigatePicker(v: Virtual, venues: List<Venue>, visited: Set<Int>,
                              walkingTo: Int, walkStep: Int,
                              onPick: (Int) -> Unit, onCancel: () -> Unit) {
    var selected by remember { mutableIntStateOf(-1) }
    // full-screen scrim: tap outside cancels (the original cancels with Esc)
    Box(Modifier.fillMaxSize().clickable { onCancel() })
    v.At(70, 71, 180, 105) {
        Column(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(2), Vga.White))
            .clickable(enabled = false) {}.padding(v.w(3))) {
            // 3 buildings, outer two on a shared baseline and the middle one riding ~8px
            // higher — measured from the original picker (dos_04: bottoms 114/106/114)
            Row(Modifier.fillMaxWidth().height(v.w(52)), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom) {
                venues.forEachIndexed { i, venue ->
                    val asset = "venue_" + snake(venue.place)
                    val lift = if (i == 1) 8f else 0f
                    Box(Modifier.width(v.w(52)).fillMaxHeight().clickable { selected = i; onPick(i) }
                        .padding(bottom = v.w(lift)),
                        contentAlignment = Alignment.BottomCenter) {
                        if (spriteExists(asset)) {
                            val aspect = drawableAspect(asset, 0.6f)
                            val bw = 48f
                            val bh = (bw * aspect).coerceAtMost(50f)
                            PixelImage(asset, Modifier.size(v.w(bw), v.w(bh)), ContentScale.Fit)
                        } else {
                            Canvas(Modifier.size(v.w(48), v.w(40))) { drawCivicBuilding(size.width, size.height, i, i in visited) }
                        }
                    }
                }
            }
            // footsteps: white dots marching from the centre toward the chosen building
            Box(Modifier.fillMaxWidth().height(v.w(10))) {
                if (walkingTo >= 0) {
                    Canvas(Modifier.fillMaxSize()) {
                        val slotW = size.width / 3f
                        val targetX = slotW * walkingTo + slotW / 2f
                        val startX = size.width / 2f
                        for (d in 0 until walkStep.coerceAtMost(6)) {
                            val t = d / 6f
                            val x = startX + (targetX - startX) * t
                            val y = size.height * (0.8f - 0.5f * t)
                            drawRect(Vga.White, topLeft = Offset(x + (if (d % 2 == 0) -size.width*0.008f else size.width*0.008f), y),
                                size = Size(size.width * 0.012f, size.height * 0.16f))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // location names: selected = white bar with black text (like "Museum" in the original)
            venues.forEachIndexed { i, venue ->
                val done = i in visited
                val isSel = i == selected || (selected < 0 && i == 0)
                Box(Modifier.fillMaxWidth().padding(horizontal = v.w(24))
                    .then(if (isSel) Modifier.background(Vga.White) else Modifier)
                    .clickable { selected = i; onPick(i) }.padding(vertical = v.w(0.5f)),
                    contentAlignment = Alignment.Center) {
                    Text(Strings.label("venue", venue.place),
                        style = v.text(8.5f, color = if (isSel) Vga.Black else if (done) Vga.LightGray else Vga.White, bold = true),
                        textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(v.w(2)))
        }
    }
}

/** Three venue buildings modelled closely on the original's investigate illustrations (the San
 *  Marino set: a low purple Sport Club, a salmon columned Library and a green-towered Palace).
 *  The game's full per-venue building set lives in the undecoded CARMEN.DAT; these are the closest
 *  match reproducible from the reference art (reference_screens/dos_04). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCivicBuilding(w: Float, h: Float, idx: Int, visited: Boolean) {
    fun g(c: Color) = if (visited) Vga.DarkGray else c
    fun rect(x: Float, y: Float, ww: Float, hh: Float, c: Color) =
        drawRect(c, topLeft = Offset(w * x, h * y), size = Size(w * ww, h * hh))
    val baseY = 0.90f
    when (idx % 3) {
        0 -> {  // Sport Club: low, wide, purple, with a railing and a stepped dome
            val purple = g(Color(0xFF9A6FB0)); val dark = g(Color(0xFF5C3F73))
            // thin white antenna poles
            rect(0.06f, 0.30f, 0.015f, 0.30f, g(Vga.White)); rect(0.925f, 0.30f, 0.015f, 0.30f, g(Vga.White))
            // wide base platform
            rect(0.02f, 0.60f, 0.96f, 0.30f, purple)
            // railing bars with lit gaps
            for (i in 0 until 11) rect(0.06f + i * 0.082f, 0.56f, 0.02f, 0.14f, dark)
            if (!visited) for (i in 0 until 5) rect(0.14f + i * 0.16f, 0.60f, 0.05f, 0.08f, Vga.Yellow)
            // stepped centre block + dome cap
            rect(0.30f, 0.44f, 0.40f, 0.18f, purple)
            rect(0.36f, 0.36f, 0.28f, 0.10f, dark)
            drawArc(purple, 180f, 180f, true, topLeft = Offset(w * 0.40f, h * 0.24f), size = Size(w * 0.20f, h * 0.24f))
            if (!visited) for (i in 0 until 6) rect(0.37f + i * 0.043f, 0.45f, 0.02f, 0.03f, Vga.Yellow)
        }
        1 -> {  // Library: salmon classical temple, tiled gable, dark columns, white steps
            val salmon = g(Color(0xFFC88878)); val col = g(Color(0xFF383038))
            // gable roof (trapezoid) with a tiled band
            val roof = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.10f, h * 0.34f); lineTo(w * 0.5f, h * 0.14f); lineTo(w * 0.90f, h * 0.34f); close()
            }
            drawPath(roof, salmon)
            rect(0.10f, 0.30f, 0.80f, 0.05f, g(Color(0xFF7A5A9A)))    // tiled frieze
            // entablature
            rect(0.10f, 0.35f, 0.80f, 0.05f, salmon)
            // columns
            for (c in 0 until 5) rect(0.14f + c * 0.16f, 0.40f, 0.05f, baseY - 0.40f, col)
            rect(0.10f, baseY - 0.02f, 0.80f, 0.06f, salmon)          // stylobate
            // central pediment + entrance
            val ped = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.38f, h * 0.46f); lineTo(w * 0.5f, h * 0.36f); lineTo(w * 0.62f, h * 0.46f); close()
            }
            drawPath(ped, salmon)
            if (!visited) { rect(0.22f, 0.48f, 0.07f, 0.10f, Vga.Yellow); rect(0.71f, 0.48f, 0.07f, 0.10f, Vga.Yellow) }
            rect(0.44f, 0.60f, 0.12f, 0.30f, salmon)
            // white steps
            rect(0.36f, 0.90f, 0.28f, 0.04f, g(Vga.White)); rect(0.40f, 0.86f, 0.20f, 0.04f, g(Vga.White))
        }
        else -> {  // Palace: two green towers, crenellated salmon centre, blue entrance
            val green = g(Color(0xFF1E7A46)); val salmon = g(Color(0xFFD09890)); val blue = g(Color(0xFF3A46B0))
            // side towers
            for (tx in listOf(0.04f, 0.74f)) {
                rect(tx, 0.30f, 0.22f, baseY - 0.30f, green)
                if (!visited) for (r in 0 until 3) for (cc in 0 until 2)
                    rect(tx + 0.04f + cc * 0.10f, 0.36f + r * 0.16f, 0.06f, 0.10f, Color(0xFF0E4028))
            }
            // centre wall + crenellations
            rect(0.24f, 0.34f, 0.52f, baseY - 0.34f, salmon)
            for (i in 0 until 5) rect(0.25f + i * 0.11f, 0.28f, 0.06f, 0.07f, salmon)
            // blue gabled entrance
            val gab = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.40f, h * 0.44f); lineTo(w * 0.5f, h * 0.34f); lineTo(w * 0.60f, h * 0.44f); close()
            }
            drawPath(gab, blue)
            rect(0.44f, 0.56f, 0.12f, baseY - 0.56f, g(Color(0xFF201828)))   // dark doorway
            if (!visited) { rect(0.29f, 0.50f, 0.07f, 0.22f, Vga.Yellow); rect(0.64f, 0.50f, 0.07f, 0.22f, Vga.Yellow) }
        }
    }
}

/* ----------------------------- TRAVEL ----------------------------- */
// City positions live in data.WorldMap (shared with the ViewModel's flight-time model).

@Composable
fun TravelScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    val s = vm.s
    val options = s.departOptions
    val flying = s.flying
    // flight animation: fraction of the current leg drawn (0..1)
    var legT by remember { mutableFloatStateOf(0f) }
    // DOS animates the destination list growing out of the city box when DEPART opens
    var grow by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(flying) {
        if (flying != null) {
            legT = 0f
            while (legT < 1f) { delay(40); legT += 0.012f }
            delay(350)
            vm.arrive()
        }
    }
    LaunchedEffect(Unit) { while (grow < 1f) { delay(16); grow += 0.12f }; grow = 1f }

    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }

    // city name box (top-left) — during a flight the clock ticks hour by hour
    CityClockBox(v, vm, tickHours = if (flying != null) (legT * s.flightHours).toInt() else 0)
    // city photo above the map — DOS keeps the departure city's photo visible during the
    // flight too (dos_flight_airport_photo_behind_map.png), no sky gradient
    v.At(4, 45, 141, 148) {
        Box(Modifier.fillMaxSize().border(BorderStroke(v.w(1), Vga.White))) {
            CityPhoto(s.currentCity, v, Modifier.fillMaxSize())
        }
    }
    // description panel (top-right, partly covered by the map below)
    v.At(149, 13, 167, 145) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White)).padding(v.w(4))) {
            Text(CityMeta.of(s.currentCity).description, style = v.text(8.5f, color = Vga.White))
        }
    }
    // world map (bottom) — DOS draws a clean map every time: only the current city (white)
    // and the offered destinations (yellow); during the flight only the current leg grows.
    v.At(8, 83, 304, 111) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(2), Vga.White)))
    }
    v.At(10, 85, WorldMap.WV, WorldMap.HV) {
        Box(Modifier.fillMaxSize()) {
            PixelImage("world_map_clean", Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                fun px(city: String) = WorldMap.of(city)?.let { Offset(it.x * size.width, it.y * size.height) }
                val dashes = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(size.width * 0.008f, size.width * 0.006f), 0f)
                val stroke = size.height * 0.016f
                // the leg being flown grows dash by dash
                if (flying != null) {
                    val a = px(s.currentCity); val b = px(flying)
                    if (a != null && b != null) {
                        val end = Offset(a.x + (b.x - a.x) * legT, a.y + (b.y - a.y) * legT)
                        drawLine(Vga.Red, a, end, strokeWidth = stroke, pathEffect = dashes)
                    }
                }
                val dot = size.height * 0.028f
                fun marker(city: String, fill: Color) = px(city)?.let {
                    drawRect(Vga.Black, topLeft = Offset(it.x - dot, it.y - dot), size = Size(dot * 2, dot * 2))
                    drawRect(fill, topLeft = Offset(it.x - dot * 0.6f, it.y - dot * 0.6f), size = Size(dot * 1.2f, dot * 1.2f))
                }
                options.forEach { marker(it, Vga.Yellow) }
                marker(s.currentCity, Vga.White)
            }
            // labels: white for the current city, yellow for the destinations (like the original)
            (listOf(s.currentCity) + options).distinct().forEach { city ->
                WorldMap.of(city)?.let { p ->
                    val leftSide = p.x > 0.78f
                    val color = if (city == s.currentCity) Vga.White else Vga.Yellow
                    MapLabel(v, Strings.place(city).uppercase(), p, leftSide, color)
                }
            }
        }
    }
    // destination drop-down growing out of the city box (hidden while flying) — same style
    // as the SEE list: header, inner double border, first destination pre-selected
    if (flying == null) {
        // tap outside the list cancels (the original cancels with Esc)
        Box(Modifier.fillMaxSize().clickable { vm.gotoCity() })
        // Nothing pre-selected: the first tap on a city only highlights it (and shows the
        // flight time); a second tap on the same city commits the flight — so a mis-tap
        // never burns hours by accident.
        var selected by remember(s.currentCity) { mutableIntStateOf(-1) }
        val fullH = 18f + options.size * 10f + 6f + 9f
        v.At(4, 13, 141, 24f + (fullH - 24f) * grow) {
            Column(Modifier.fillMaxSize().background(Vga.Black)
                .border(BorderStroke(v.w(1), Vga.White)).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
                Box(Modifier.fillMaxWidth().height(v.w(14)), contentAlignment = Alignment.Center) {
                    Text(Strings.place(s.currentCity), style = v.text(9, color = Vga.White, bold = true))
                }
                if (grow >= 1f) Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = v.w(3))
                    .border(BorderStroke(v.w(2), Vga.White)).padding(v.w(1))) {
                    Column(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, city ->
                            val isSel = i == selected
                            Box(Modifier.fillMaxWidth().height(v.w(10))
                                .then(if (isSel) Modifier.background(Vga.White) else Modifier)
                                .clickable { if (selected == i) vm.travelTo(city) else selected = i },
                                contentAlignment = Alignment.Center) {
                                Text(Strings.place(city), style = v.text(8.5f,
                                    color = if (isSel) Vga.Black else Vga.White, bold = true))
                                // flight time shown up front so the player weighs the cost
                                Text("~${vm.flightHoursTo(city)}h",
                                    style = v.text(6f, color = if (isSel) Vga.Blue else Vga.LightGray),
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = v.w(2)))
                            }
                        }
                    }
                }
                // hint appears once a city is highlighted
                if (grow >= 1f) Box(Modifier.fillMaxWidth().height(v.w(8)), contentAlignment = Alignment.Center) {
                    if (selected >= 0) Text(Strings.ui("tap again to fly"),
                        style = v.text(6f, color = Vga.Yellow, bold = true))
                }
            }
        }
    }
    OverlayHost(v, vm)
    // Tour: on the map, spotlight the destination list and repeat the witness's hint.
    com.acme.clara.ui.Tour(v, vm, suppressed = flying != null)
}

/** A city name printed on the map next to its dot (offset left or right to stay on-screen). */
@Composable
private fun BoxScope.MapLabel(v: Virtual, name: String, p: Offset, leftSide: Boolean, color: Color) {
    val approxCharW = 4.6f
    val dx = if (leftSide) -(name.length * approxCharW + 4f) else 4f
    Box(Modifier.align(Alignment.TopStart)
        .offset(v.w(WorldMap.WV * p.x + dx), v.w(WorldMap.HV * p.y - 3.5f))) {
        // black outline for legibility over land/ocean, then the coloured text
        Text(name, style = v.text(6.5f, color = Vga.Black, bold = true),
            modifier = Modifier.offset(v.w(0.4f), v.w(0.4f)))
        Text(name, style = v.text(6.5f, color = color, bold = true))
    }
}

/* ----------------------------- CRIME COMPUTER -----------------------------
 * Faithful to the original screen: the city name box stays top-left, the LEFT panel holds
 * the HQ dot-matrix printer (results print onto its paper), and the RIGHT panel is the beige
 * CRT computer listing SEX/HOBBY/HAIR/FEATURE/VEHICLE and COMPUTE. Tapping a row selects it
 * (white bar, blue text — the DOS cursor) and cycles its value; COMPUTE prints the matching
 * suspects on the printer, and a single match auto-issues the arrest warrant.
 * Art: crime_printer.png / crime_computer.png cropped from an original capture.
 */
// canonical row labels; localized at render via Strings.ui (kept short — the CRT row is ~10 chars)
private val CRIME_ROWS = listOf("SEX", "HOBBY", "HAIR", "FEATURE", "VEHICLE")

/** Wrap text to the printer paper width (~17 chars). */
private fun paperWrap(text: String, width: Int = 17): List<String> {
    val out = mutableListOf<String>(); var line = ""
    for (word in text.split(" ")) {
        line = when {
            line.isEmpty() -> word
            line.length + 1 + word.length <= width -> "$line $word"
            else -> { out.add(line); word }
        }
    }
    if (line.isNotEmpty()) out.add(line)
    return out
}

@Composable
fun CrimeScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    val s = vm.s
    // Start with no row selected so every row behaves the same: the first tap selects
    // (white cursor bar), the next cycles its value. (With row 0 pre-selected, a first
    // tap on SEX cycled immediately — inconsistent with the other rows.)
    var selRow by remember { mutableIntStateOf(-1) }
    val paper = remember { mutableStateListOf(Strings.ui("READY.")) }
    var typing by remember { mutableStateOf("") }
    var printing by remember { mutableStateOf(false) }
    // S2: a visual "WARRANT ISSUED" stamp fires when the description narrows to one suspect
    // (the WARRANT stinger + haptic already play in vm.compute()).
    var showStamp by remember { mutableStateOf(false) }
    val reduce = reducedMotion()
    val scope = rememberCoroutineScope()

    // dot-matrix teletype: every line prints letter by letter, like the original
    suspend fun printLine(line: String) {
        typing = ""
        for (ch in line) { typing += ch; delay(14) }
        paper.add(line); typing = ""
        delay(90)
    }

    fun valueOf(row: Int): String? = when (row) {
        0 -> s.compSex; 1 -> s.compHobby; 2 -> s.compHair; 3 -> s.compFeature; else -> s.compVehicle
    }
    fun optionsOf(row: Int): List<String> = when (row) {
        0 -> GameData.sexes; 1 -> GameData.hobbies; 2 -> GameData.hairColors
        3 -> GameData.features; else -> GameData.vehicles
    }
    fun keyOf(row: Int): String = when (row) {
        0 -> "sex"; 1 -> "hobby"; 2 -> "hair"; 3 -> "feature"; else -> "vehicle"
    }
    fun cycle(row: Int) {
        val cyc = listOf<String?>(null) + optionsOf(row)
        val idx = cyc.indexOf(valueOf(row))
        vm.setComp(keyOf(row), cyc[(idx + 1) % cyc.size])
    }
    fun runCompute() {
        if (printing) return
        printing = true
        scope.launch {
            printLine(Strings.ui("Wait..."))
            delay(900)
            vm.compute()
            val results = vm.matches()
            paper.add("")
            when {
                !vm.anyFilterSet() -> paperWrap(Strings.ui("Please enter the suspect's description first.")).forEach { printLine(it) }
                results.isEmpty() -> paperWrap(GameData.ELIMINATES_ALL).forEach { printLine(it) }
                else -> {
                    // the original prints "Suspect:" when the description narrows to one
                    printLine(if (results.size == 1) Strings.ui("Suspect:") else Strings.ui("Suspects:"))
                    results.forEach { printLine(it.name) }
                    if (results.size == 1) {
                        paper.add("")
                        paperWrap(GameData.WARRANT_ISSUED.replace("%s", results.first().name))
                            .forEach { printLine(it) }
                        showStamp = true
                    }
                }
            }
            // DOS prints READY. only at the start of a computer session, and leaves no row
            // selected after a compute
            selRow = -1
            printing = false
        }
    }

    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }
    // city name / date box (unchanged from the city screen; shows SLEEPING… when the
    // 3-hour compute crosses 10 p.m. — the CRT stays up, like the original)
    CityClockBox(v, vm)
    // LEFT: the printer panel — paper grows upward from the platen as results print
    v.At(2, 44, 146, 154) {
        Box(Modifier.fillMaxSize()) {
            PixelImage("crime_printer", Modifier.fillMaxSize())
            // paper sheet: bottom fixed at panel y=102, top grows with content (min = original sheet)
            val lineH = 7.4f
            val shown = paper.takeLast(12) + (if (typing.isNotEmpty()) listOf(typing) else emptyList())
            val sheetH = (10f + shown.size * lineH).coerceAtLeast(29f).coerceAtMost(96f)
            v.At(14, 102f - sheetH, 113, sheetH) {
                Box(Modifier.fillMaxSize().background(Vga.White).border(BorderStroke(v.w(0.7f), Vga.Black))) {
                    // sprocket hole columns: small holes every ~6 virtual px down both edges
                    Canvas(Modifier.fillMaxSize()) {
                        val unit = size.width / 113f          // 1 virtual px
                        val hole = unit * 1.7f
                        var y = size.height - unit * 4f
                        while (y > unit * 2f) {
                            drawRect(Vga.Black, topLeft = Offset(unit * 2.6f, y), size = Size(hole, hole))
                            drawRect(Vga.Black, topLeft = Offset(size.width - unit * 4.3f, y), size = Size(hole, hole))
                            y -= unit * 6.2f
                        }
                    }
                    Column(Modifier.fillMaxSize().padding(start = v.w(8), end = v.w(7), top = v.w(2)),
                        verticalArrangement = Arrangement.Bottom) {
                        shown.forEach { Text(it, style = v.text(6.8f, color = Vga.Black), maxLines = 1) }
                        Spacer(Modifier.height(v.w(3)))
                    }
                }
            }
        }
    }
    // RIGHT: the CRT computer with the attribute rows
    v.At(150, 16, 170, 156) {
        Box(Modifier.fillMaxSize()) {
            PixelImage("crime_computer", Modifier.fillMaxSize())
            // §17: the front-bezel LEDs light up and blink while the computer works
            // (dos_computer_wait_led.png vs dos_computer_initial.png; strip at (60,97))
            if (printing) {
                var ledOn by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { while (true) { delay(350); ledOn = !ledOn } }
                if (ledOn) v.At(60, 97, 44, 6) {
                    PixelImage("crime_computer_leds", Modifier.fillMaxSize())
                }
            }
            // rows live inside the CRT: interior x=13..152, first row y=9, pitch 10 (image-relative)
            CRIME_ROWS.forEachIndexed { i, label ->
                val sel = selRow == i
                v.At(13, 9f + i * 10f, 139, 9) {
                    Box(Modifier.fillMaxSize()
                        .then(if (sel) Modifier.background(Vga.White) else Modifier)
                        .clickable { if (selRow == i) cycle(i) else selRow = i }) {
                        Text(Strings.ui(label) + ":", style = v.text(8, color = if (sel) Vga.Blue else Vga.Yellow, bold = true),
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = v.w(11)))
                        valueOf(i)?.let {
                            Text(Strings.label("tval", it), style = v.text(8, color = if (sel) Vga.Blue else Vga.Yellow),
                                modifier = Modifier.align(Alignment.CenterStart).padding(start = v.w(65)))
                        }
                    }
                }
            }
            // COMPUTE row
            v.At(13, 69, 139, 9) {
                Box(Modifier.fillMaxSize()
                    .then(if (selRow == 5) Modifier.background(Vga.White) else Modifier)
                    .clickable { selRow = 5; runCompute() }) {
                    Text(Strings.ui("COMPUTE"), style = v.text(8, color = if (selRow == 5) Vga.Blue else Vga.Yellow, bold = true),
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = v.w(11)))
                }
            }
        }
    }
    GameToolbar(v, vm, onSee = { vm.selectTool(0); vm.gotoCity() },
        onInvestigate = { vm.selectTool(2); vm.gotoCity() })
    if (showStamp) WarrantStamp(v, reduce) { showStamp = false }
    OverlayHost(v, vm)
    // Suppress while the printer is still clattering out the suspect list, so the player reads the
    // result before the next tip (chase the suspect) appears.
    com.acme.clara.ui.Tour(v, vm, suppressed = printing)
}

/** S2: the "WARRANT ISSUED · APPROVED" stamp — a ritual beat that slams in when the roster
 *  narrows to one, then clears itself. Pairs the eye with the WARRANT stinger already playing. */
@Composable
internal fun WarrantStamp(v: Virtual, reduce: Boolean, onDone: () -> Unit) {
    var play by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (play || reduce) 1f else 3f,
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = "stamp")
    val rot by animateFloatAsState(if (play || reduce) -8f else 6f, tween(300), label = "rot")
    LaunchedEffect(Unit) { play = true; delay(1400); onDone() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rot }
            .border(BorderStroke(v.w(2), Vga.Red)).background(Vga.Black.copy(alpha = 0.75f))
            .padding(horizontal = v.w(10), vertical = v.w(5)),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Strings.ui("WARRANT ISSUED"), style = v.text(13, color = Vga.Red, bold = true))
                Text("●  " + Strings.ui("APPROVED") + "  ●", style = v.text(9, color = Vga.Red, bold = true))
            }
        }
    }
}

/* ----------------------------- CHASE (hideout confrontation) -----------------------------
 * Replicates the arrest sequence captured from the original: the suspect (trenchcoat +
 * fedora) sprints across the black right panel, "There goes the suspect!" flashes up, the
 * police squad storms after them, and — when your warrant is right — the suspect marches
 * back across, hands up, escorted at gunpoint. Then the Interpol report (ResultScreen).
 * Sprites anim_suspect_run_0..2 / anim_cops_0..2 / anim_escort_0..1 are cropped from the
 * original's animation frames. Tap anywhere to skip.
 */
@Composable
fun ChaseScreen(vm: ClaraViewModel) = VirtualScreen { v ->
    val s = vm.s
    // stage: 0 suspect runs right · 1 "There goes the suspect!" · 2 cops chase right ·
    // 3 escort marches back left (win only) · then done
    var stage by remember { mutableIntStateOf(0) }
    var x by remember { mutableFloatStateOf(-50f) }     // sprite x within the panel (virtual px)
    var frame by remember { mutableIntStateOf(0) }
    // A tap fast-forwards only the CURRENT beat, not the whole sequence — one stray tap used to
    // call chaseDone() outright and skip straight past every remaining stage, which read as the
    // animation glitching/cutting out rather than a deliberate skip.
    var skip by remember { mutableStateOf(false) }

    suspend fun runTo(target: Float, step: Float, stepDelay: Long) {
        while ((if (step > 0) x < target else x > target) && !skip) { delay(stepDelay); x += step; frame++ }
        x = target
    }
    suspend fun pause(ms: Long) {
        var left = ms
        while (left > 0 && !skip) { delay(30); left -= 30 }
    }

    LaunchedEffect(Unit) {
        // 1) the suspect sprints across, left -> right
        x = -50f
        runTo(170f, 4.5f, 55)
        // 2) "There goes the suspect!"
        skip = false; stage = 1
        pause(1500)
        if (s.won) {
            // 3) the cops storm after them
            skip = false; stage = 2; x = -55f
            runTo(170f, 5f, 55)
            skip = false
            pause(700)
            // 4) hands up: the suspect is walked back at gunpoint, right -> left
            skip = false; stage = 3; x = 165f
            runTo(-55f, -3.5f, 60)
        }
        vm.chaseDone()
    }

    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }
    // city name / date box
    CityClockBox(v, vm)
    // city photo stays on the left
    v.At(4, 45, 141, 148) {
        Box(Modifier.fillMaxSize().border(BorderStroke(v.w(1), Vga.White))) {
            CityPhoto(s.currentCity, v, Modifier.fillMaxSize())
        }
    }
    // right panel: the chase plays out on black (clipped so sprites enter/exit at the edges)
    v.At(149, 13, 167, 145) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))
            .clipToBounds()) {
            when (stage) {
                0 -> {
                    val sprite = "anim_suspect_run_${frame % 3}"
                    PixelImage(sprite,
                        Modifier.align(Alignment.BottomStart)
                            .padding(bottom = v.w(10))
                            .offset(v.w(x), 0.dp)
                            .size(v.w(48), v.w(48)), ContentScale.Fit)
                }
                1 -> Text(Strings.ui("There goes\nthe suspect!"),
                    style = v.text(9, color = Vga.White, bold = true),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = v.w(18)))
                2 -> PixelImage("anim_cops_${frame % 3}",
                    Modifier.align(Alignment.BottomStart)
                        .padding(bottom = v.w(10))
                        .offset(v.w(x), 0.dp)
                        .size(v.w(50), v.w(48)), ContentScale.Fit)
                3 -> PixelImage("anim_escort_${frame % 2}",
                    Modifier.align(Alignment.BottomStart)
                        .padding(bottom = v.w(10))
                        .offset(v.w(x), 0.dp)
                        .size(v.w(48), v.w(48)), ContentScale.Fit)
            }
        }
    }
    // DOS removes the toolbar completely during the chase — the area below the right panel
    // stays plain black (dos_chase_suspect_no_toolbar.png)
    // tap to fast-forward the current beat (see `skip` above)
    Box(Modifier.fillMaxSize().clickable { skip = true })
}

/* ----------------------------- RESULT -----------------------------
 * Faithful to the original ending: the left printer types the Interpol messages while the
 * right panel shows the culprit behind bars in the brick JAIL (win) or stays black (loss).
 * "Press any key or button to continue." advances to the next case.
 */
/** P4: the line the promotion screen prints to name what the new rank actually changes.
 *  Only route length is coded per rank (5 + rankIndex, capped at 9), so name that — never a
 *  region unlock or a tighter clock the game doesn't apply. */
internal fun promotionPerkLine(rankIndex: Int): String {
    val cities = (5 + rankIndex).coerceAtMost(9)
    return Strings.ui("As {0}, the trail now runs {1} cities — a longer chase, more clues to read.",
        Strings.label("rank", GameData.ranks[rankIndex]), cities)
}

/** Lowercased with combining diacritics stripped, for accent-insensitive text comparison
 *  (a translated quiz answer like "Nilo" or "Bagdá" shouldn't fail to match over one accent). */
private val DIACRITIC_REGEX = Regex("\\p{Mn}+")
private fun foldDiacritics(s: String): String =
    java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(DIACRITIC_REGEX, "")

@Composable
fun ResultScreen(vm: ClaraViewModel) = VirtualScreen(keepVirtualYAboveIme = 150f) { v ->
    val s = vm.s
    val shareCtx = androidx.compose.ui.platform.LocalContext.current
    val reduce = reducedMotion()
    val printed = remember { mutableStateListOf<String>() }
    var typing by remember { mutableStateOf("") }
    // 0 typing report · 1 typing quiz · 2 quiz input · 3 typing verdict/ready · 4 Yes/No
    var stage by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val quiz = remember { GameData.promotionQuiz.random() }
    val focus = remember { FocusRequester() }
    val done = stage == 4
    // The quiz answer field only appears once the player has explicitly tapped past this — it
    // used to auto-focus (and pop the keyboard) as soon as the question finished typing, which
    // was jarring, and when that was removed instead it wasn't obvious the tiny field on the
    // paper was even tappable. An explicit "tap to continue" gate, matching every other one on
    // this screen, makes the reveal deliberate either way.
    var answerRevealed by remember { mutableStateOf(false) }
    // S1: the captured mugshot slams in and is "filed" in the gallery before the report starts
    // printing — it used to play at the same time as the printer, so its own tap-to-continue and
    // the printer's competed for the same tap and neither was clearly the thing being responded to.
    var slamActive by remember { mutableStateOf(s.won && !s.careerOver && !reduce && s.culprit?.name != null) }
    // The paper only shows its last ~12 lines, so a long report used to just scroll continuously
    // past the reader. Pause once a page's worth has printed and wait for a tap before continuing.
    var waitingForTap by remember { mutableStateOf(false) }
    suspend fun gate() {
        if (reduce) return
        waitingForTap = true
        while (waitingForTap) delay(30)
    }

    suspend fun typeAll(lines: List<String>, width: Int = 20) {
        for (line in lines) {
            for (piece in paperWrap(line, width)) {
                if (reduce) {                       // reduced-motion: print instantly, no typewriter
                    printed.add(piece)
                } else {
                    typing = ""
                    for (ch in piece) { typing += ch; delay(14) }
                    printed.add(piece); typing = ""
                    delay(90)
                }
                if (printed.size % 10 == 0) gate()
            }
            printed.add("")
        }
    }
    LaunchedEffect(Unit) {
        while (slamActive) delay(30)   // let the mugshot reveal finish (and be dismissed) first
        typeAll(s.resultLines)
        when {
            // Carmen jailed on the final case: no next case — the detective is retired
            // from the roster; any tap returns to the title
            s.careerOver -> stage = 5
            s.pendingPromotion -> {
                typeAll(listOf(
                    Strings.ui("Use the World Almanac and Book of Facts to help you find the missing word in the following sentence:"),
                    Strings.quizQuestion(quiz),
                ))
                stage = 2
            }
            else -> {
                typeAll(listOf(Strings.ui("Ready for your next case, {0}?", s.detectiveName)))
                stage = 4
            }
        }
    }
    LaunchedEffect(stage) {
        if (stage == 3) {
            // Accent-insensitive: a translated answer (e.g. Portuguese "Nilo") shouldn't fail
            // just because a mobile keyboard made the diacritic (if any) awkward to type.
            val correct = foldDiacritics(input.trim()) == foldDiacritics(Strings.quizAnswer(quiz))
            if (correct) {
                vm.resolvePromotion(true)
                // P4: name what the rank actually changes — route length is the real, coded
                // reward (it scales per rank); don't promise regions or a tighter clock, which
                // the game doesn't change.
                val newRank = vm.s.rankIndex
                typeAll(listOfNotNull(
                    Strings.ui("Correct! Well done, {0}.", s.detectiveName),
                    Strings.ui("Your new rank is: {0}.", Strings.label("rank", GameData.ranks[newRank])),
                    promotionPerkLine(newRank),
                    vm.casesToNextPromotion().takeIf { it > 0 }
                        ?.let { Strings.ui("{0} more cases until your next promotion.", it) },
                ))
                typeAll(listOf(Strings.ui("Ready for your next case, {0}?", s.detectiveName)))
                stage = 4
            } else {
                // DOS re-asks the same question until it's answered correctly
                // (dos_quiz_incorrect_try_again.png) — the promotion stays attainable
                typeAll(listOf(Strings.ui("That is incorrect."), Strings.ui("Please try again."), Strings.quizQuestion(quiz)))
                stage = 2
            }
        }
        if (stage == 2) {
            // clear any taps that leaked in while earlier stages auto-advanced (§20)
            input = ""
            answerRevealed = false
        }
    }
    // Once the player deliberately taps past the "tap to continue" gate, reveal the answer field
    // and bring the keyboard up — a short delay lets the field finish entering composition first.
    LaunchedEffect(answerRevealed) {
        if (answerRevealed) { delay(80); runCatching { focus.requestFocus() } }
    }
    val paperScroll = rememberScrollState()
    // Snap (not animate) to the bottom: a new LaunchedEffect fires on every keystroke of the
    // typewriter effect (every ~16ms), far faster than an animated scroll can settle — so the
    // animated version always lagged behind, leaving the line actually being typed below the
    // visible paper until it finished and the scroll caught up.
    LaunchedEffect(printed.size, typing, input, stage) { paperScroll.scrollTo(paperScroll.maxValue) }

    v.At(0, 0, 320, 11) { GameMenuBar(v, vm) }
    // city name box
    CityClockBox(v, vm)
    // left: the printer with the Interpol report typing on
    v.At(2, 44, 146, 154) {
        Box(Modifier.fillMaxSize()) {
            PixelImage("crime_printer", Modifier.fillMaxSize())
            val lineH = 7.82f   // matches v.text(6.8f)'s actual lineHeight (6.8 * 1.15)
            val showInput = stage == 2 && answerRevealed
            // Ratcheted: the sheet's height only ever grows (paper feeding out of the printer),
            // and freezes for good once it reaches the cap — it used to be recomputed from
            // whatever was on the currently-visible tail (typing line appearing/disappearing,
            // the answer field appearing), so it kept shrinking and regrowing by a line's worth
            // right at the cap, which read as a flicker. Actual overflow is now handled by real
            // scrolling (below) instead of an estimated line-height budget, so this height is
            // purely the visual "how tall is the paper right now" — it doesn't need to be exact.
            var peakRows by remember { mutableIntStateOf(0) }
            val liveRows = printed.size + (if (typing.isNotEmpty()) 1 else 0) + (if (showInput) 1 else 0)
            peakRows = maxOf(peakRows, liveRows).coerceAtMost(10)
            val sheetH = (10f + peakRows * lineH).coerceAtLeast(29f).coerceAtMost(96f)
            v.At(14, 102f - sheetH, 113, sheetH) {
                Box(Modifier.fillMaxSize().background(Vga.White).border(BorderStroke(v.w(0.7f), Vga.Black))) {
                    Canvas(Modifier.fillMaxSize()) {
                        val unit = size.width / 113f
                        val hole = unit * 1.7f
                        var y = size.height - unit * 4f
                        while (y > unit * 2f) {
                            drawRect(Vga.Black, topLeft = Offset(unit * 2.6f, y), size = Size(hole, hole))
                            drawRect(Vga.Black, topLeft = Offset(size.width - unit * 4.3f, y), size = Size(hole, hole))
                            y -= unit * 6.2f
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().padding(start = v.w(8), end = v.w(7), top = v.w(2))
                            .verticalScroll(paperScroll),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        printed.forEach { Text(it, style = v.text(6.8f, color = Vga.Black), maxLines = 1) }
                        if (typing.isNotEmpty()) Text(typing, style = v.text(6.8f, color = Vga.Black), maxLines = 1)
                        // promotion-quiz answer typed directly onto the paper, revealed by the
                        // gray-strip "tap to continue" gate below rather than appearing on its own
                        if (showInput) BasicTextField(
                            value = input,
                            onValueChange = { input = it.take(18).filter { c -> c != '\n' } },
                            singleLine = true,
                            textStyle = v.text(6.8f, color = Vga.Black),
                            cursorBrush = SolidColor(Vga.Black),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                platformImeOptions = PlatformImeOptions("flagNoExtractUi"),
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (input.isNotBlank()) { printed.add(input); stage = 3 }
                            }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                        Spacer(Modifier.height(v.w(3)))
                    }
                }
            }
        }
    }
    // right: the JAIL (win) or black panel (loss)
    v.At(150, 26, 168, 146) {
        Box(Modifier.fillMaxSize().background(Vga.Black).border(BorderStroke(v.w(1), Vga.White))) {
            if (s.won) {
                PixelImage("jail_cell", Modifier.fillMaxSize())
                // the suspect's eyes blink behind the bars on a slow loop (dos_jail_eyes_a/b)
                var blink by remember { mutableStateOf(false) }
                if (!reduce) LaunchedEffect(Unit) { while (true) { delay(1000); blink = !blink } }
                if (blink) v.At(63, 71, 41, 29) {
                    PixelImage("jail_eyes_alt", Modifier.fillMaxSize())
                }
            }
        }
    }
    // Compact SHARE action in a top corner: it used to be a full-width button sitting right above
    // the "next case?" Yes/No pair, which read as part of that decision — up here, clearly its own
    // independent, always-available action instead.
    if (s.won && !s.careerOver) {
        v.At(266, 14, 50, 10) {
            YellowButton(v, Strings.ui("SHARE")) { com.acme.clara.ui.shareResult(shareCtx, vm) }
        }
    }
    // A single gray status strip for every "the game is waiting on you" moment on this screen —
    // matching the sign-on printer's own gray-background / black-text "press any key" treatment.
    // Previously each moment used white-on-black text floating in a different spot (on the printer,
    // or wherever), which read as unrelated ad-hoc prompts rather than one consistent "here's what
    // happens next" zone.
    v.At(150, 172, 168, 28) {
        Box(
            Modifier.fillMaxSize().background(Vga.White).border(BorderStroke(v.w(1), Vga.Black)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                waitingForTap || (stage == 2 && !answerRevealed) || stage == 5 ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Strings.ui("Press any key or"), style = v.text(7.5f, color = Vga.Black, bold = true))
                        Text(Strings.ui("button to continue."), style = v.text(7.5f, color = Vga.Black, bold = true))
                    }
                done -> Row(horizontalArrangement = Arrangement.spacedBy(v.w(8))) {
                    Box(Modifier.size(v.w(76), v.w(12))) { YellowButton(v, Strings.ui("Yes")) { vm.toBriefingForNext() } }
                    Box(Modifier.size(v.w(76), v.w(12))) { YellowButton(v, Strings.ui("No")) { vm.menuQuitToTitle() } }
                }
            }
        }
    }
    // Tap anywhere to advance past whichever gate is currently active in the strip above.
    if (waitingForTap) Box(Modifier.fillMaxSize().clickable { waitingForTap = false })
    if (stage == 2 && !answerRevealed) Box(Modifier.fillMaxSize().clickable { answerRevealed = true })
    if (stage == 5) Box(Modifier.fillMaxSize().clickable { vm.menuQuitToTitle() })
    // S1: the captured mugshot slams in and is "filed" in the gallery — plays before the report
    // starts printing (see `slamActive` above).
    if (slamActive) ArrestSlam(v, s.culprit?.name!!) { slamActive = false }
    OverlayHost(v, vm)
}

/** S1 multisensory reward: on a win the suspect's mugshot slams in, before the report starts
 *  printing. Plays once, dismissed by a tap (skipped on reduced motion) — it used to auto-dismiss
 *  after 1.35s, which was often gone before the player had really seen it, AND used to run at the
 *  same time as the printer, so this tap and the printer's own competed for attention. A full-screen
 *  scrim now sits behind the card too: without one, "FILED IN MOST WANTED" landed directly over
 *  whatever was already on screen (white paper in one spot, black panel in another) and read fine
 *  in some places and unreadably in others. */
@Composable
internal fun ArrestSlam(v: Virtual, name: String, onDone: () -> Unit) {
    var play by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (play) 1f else 2.6f,
        animationSpec = tween(360, easing = FastOutSlowInEasing), label = "slam")
    val alpha by animateFloatAsState(if (play) 1f else 0f, tween(160), label = "fade")
    LaunchedEffect(Unit) { play = true }
    val slug = "suspect_" + snake(name)
    Box(
        Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.8f * alpha))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
            Box(Modifier.size(v.w(56)).graphicsLayer { scaleX = scale; scaleY = scale }
                .border(BorderStroke(v.w(1), Vga.Yellow)).background(Vga.DarkGray),
                contentAlignment = Alignment.Center) {
                if (spriteExists(slug)) PixelImage(slug, Modifier.fillMaxSize())
                else Text("◆", style = v.text(20, color = Vga.LightGray, bold = true))
            }
            Spacer(Modifier.height(v.w(4)))
            Text(Strings.ui("FILED IN MOST WANTED"), style = v.text(9, color = Vga.Yellow, bold = true))
            Spacer(Modifier.height(v.w(6)))
            Text(Strings.ui("Press any key or"), style = v.text(7.5f, color = Vga.White, bold = true))
            Text(Strings.ui("button to continue."), style = v.text(7.5f, color = Vga.White, bold = true))
        }
    }
}
