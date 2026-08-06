package com.acme.clara.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.game.GameState
import com.acme.clara.game.Phase
import com.acme.clara.i18n.Strings
import com.acme.clara.ui.theme.Vga

/**
 * The Rookie tour — a set of contextual, teach-once lessons, not a linear script. Each turn it asks
 * [lessonFor] which single lesson (if any) is relevant to the *current* game state, spotlights the
 * one control that lesson is about, and shows a tip that points at it. Lessons are armed by what the
 * player has actually done (heard a trail hint, heard a suspect trait, got a warrant…), so a tip
 * never fires before its concept exists, and each clears the moment the player performs its action.
 *
 * Rendered inside each gameplay screen's VirtualScreen so it shares the 320x200 coordinate space;
 * [suppressed] hides it during a screen's own transient panels (venue picker, walking, SEE list).
 */
@Composable
fun Tour(v: Virtual, vm: ClaraViewModel, suppressed: Boolean = false) {
    val s = vm.s
    if (!s.tutorialActive || suppressed) return
    // A witness testimony or a menu window owns the screen — never spotlight over it.
    if (s.openClue != null || s.overlay != null) return
    val shown = lessonFor(s) ?: return

    val reduce = reducedMotion()
    val pulse = if (reduce) 1f else {
        val inf = rememberInfiniteTransition(label = "tourPulse")
        inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(720), RepeatMode.Reverse), label = "a").value
    }

    val t = shown.target
    val m = 3f

    // ---- dim everything except the target (four bands, so the control stays live) ----
    Dim(v, 0f, 0f, 320f, t.y - m)
    Dim(v, 0f, t.y + t.h + m, 320f, 200f - (t.y + t.h + m))
    Dim(v, 0f, t.y - m, t.x - m, t.h + 2 * m)
    Dim(v, t.x + t.w + m, t.y - m, 320f - (t.x + t.w + m), t.h + 2 * m)

    // ---- pulsing highlight ring (decorative: no pointer input, so taps reach the control) ----
    place(v, t.x - m, t.y - m, t.w + 2 * m, t.h + 2 * m) {
        Box(Modifier.fillMaxSize()
            .border(BorderStroke(v.w(1.5f), Vga.Yellow.copy(alpha = pulse)))
            .padding(v.w(1f))
            .border(BorderStroke(v.w(0.6f), Vga.White.copy(alpha = pulse * 0.8f))))
    }

    // ---- the tip card, hugging the free side of the target with a caret that points at it ----
    val targetCx = t.x + t.w / 2f
    val below = (t.y + t.h / 2f) <= 100f
    // The travel-list tip quotes the witness's clue verbatim, which can run long — give it a wider
    // card (fewer wrapped lines) so its footer (Skip tour) never overflows the allotted height below
    // the destination list. Every other lesson's text is short enough for the narrower default.
    val cardW = if (t === TRAVEL_LIST) 280f else 200f
    val cardCx = targetCx.coerceIn(cardW / 2f + 4f, 316f - cardW / 2f)
    val cardX = cardCx - cardW / 2f
    val caretLocalX = (targetCx - cardX - CARET_W / 2f).coerceIn(6f, cardW - 6f - CARET_W)

    if (below) v.At(cardX, t.y + t.h + m, cardW, 200f - (t.y + t.h + m) - 3f, Alignment.TopStart) {
        Column(Modifier.fillMaxWidth()) {
            Caret(v, up = true, localX = caretLocalX)
            TipPanel(v, vm, shown)
        }
    } else v.At(cardX, 12f, cardW, (t.y - m) - 12f, Alignment.BottomStart) {
        Column(Modifier.fillMaxWidth()) {
            TipPanel(v, vm, shown)
            Caret(v, up = false, localX = caretLocalX)
        }
    }
}

/** A lesson currently being shown: which control (virtual rect) to spotlight, and what to say.
 *  [info] tips have no single completing action, so they carry a GOT IT button. */
private class Shown(val id: String, val target: R, val title: String, val text: String, val info: Boolean)

/** The one relevant lesson for this game state, or null when the tour should stay quiet. Scanned in
 *  teaching priority: interview → mind-the-clock → crime computer → follow-trail → warrant → arrest. */
private fun lessonFor(s: GameState): Shown? {
    val seen = s.tutorialSeen
    fun un(id: String) = id !in seen

    if (un("wrongflight") && !s.onTrack && s.phase == Phase.CITY && s.route.isNotEmpty())
        return Shown("wrongflight", TOOL_DEPART, Strings.ui("WRONG TURN"),
            Strings.ui("This isn't where the suspect went — nobody here has seen them. Tap the plane and fly back to try the right city."), false)

    if (un("interview") && s.phase == Phase.CITY && s.onTrack && s.visited.size < 3)
        return Shown("interview", TOOL_INVESTIGATE, Strings.ui("QUESTION THE WITNESSES"),
            Strings.ui("Open a place and talk to a witness — try all three here ({0}/3). Each offers something different: where the suspect fled, what they look like, or just gossip.", s.visited.size), false)

    // Checked before "time": the moment all three venues are tried, "interview" is marked seen and
    // (in the normal on-track flow) a trait clue was already heard at venue 2 — so without this
    // ordering the unlocked clock tip would show first and let the player wander off (e.g. tap
    // DEPART) before ever being funneled into the crime computer.
    if (un("computer") && s.sawTraitClue && s.warrantFor == null) when (s.phase) {
        Phase.CITY -> return Shown("computer", TOOL_CRIME, Strings.ui("RECORD THE THIEF"),
            Strings.ui("A witness described the crook. Open the crime computer to log what you heard — one detail per city."), false)
        Phase.CRIME -> {
            // Keep coaching the exact row for a clue you've heard but not yet entered — repeating the
            // hint — and only prompt COMPUTE once it's actually in, so you can't compute prematurely.
            val need = s.revealedTraits.firstOrNull { (c, v) -> compVal(s, c) != v }
            return if (need != null) {
                val label = rowLabelOf(need.first)
                Shown("computer", crtRow(rowIndexOf(need.first)), Strings.ui("ENTER THE CLUE"),
                    Strings.ui("A witness said the thief's {0} is “{1}”. Tap the {2} row, then tap it again until it reads {1}.", label.lowercase(), need.second, label), false)
            } else Shown("computer", CRT_COMPUTE, Strings.ui("RUN THE COMPUTER"),
                Strings.ui("That clue's logged. Tap COMPUTE — if several suspects still match, gather more as you travel until one is left."), false)
        }
        else -> {}
    }

    // Falls through to here whenever "computer" didn't fire (off-track city, or the warrant's
    // already issued) — the clock tip still gets its turn in those cases.
    if (un("time") && s.phase == Phase.CITY && "interview" in seen)
        return Shown("time", CLOCK, Strings.ui("MIND THE CLOCK"),
            Strings.ui("Three interviews — and look how far the clock jumped. You're on a deadline, so from here on you needn't question everyone: once you have a lead and a description, move on."), true)

    if (un("trail") && s.sawTrailClue) when (s.phase) {
        Phase.CITY, Phase.CRIME -> return Shown("trail", TOOL_DEPART, Strings.ui("CHASE THE SUSPECT"),
            Strings.ui("Your witnesses hinted where the suspect fled next. Not sure of a place? Open Bureau ▸ World Database to read up on each destination. Then tap the plane to fly there."), false)
        Phase.TRAVEL -> {
            // On the map, remind the player of the witness's hint and spotlight the destination list.
            val clue = s.journal.lastOrNull { it.city == s.currentCity && it.kind == ClueKind.DESTINATION }?.text
            return Shown("trail", TRAVEL_LIST, Strings.ui("WHERE NEXT?"),
                if (clue != null) Strings.ui("“{0}” — pick the matching destination.", clue)
                else Strings.ui("Pick the destination your witnesses pointed to."), false)
        }
        else -> {}
    }

    if (un("warrant") && s.warrantFor != null && (s.phase == Phase.CITY || s.phase == Phase.CRIME))
        return Shown("warrant", CLOCK, Strings.ui("WARRANT ISSUED"),
            Strings.ui("That's your suspect! Now just follow the trail to their hideout and close in."), true)

    if (un("arrest") && s.warrantFor != null && s.atHideout && s.phase == Phase.CITY)
        return Shown("arrest", TOOL_INVESTIGATE, Strings.ui("MAKE THE ARREST"),
            Strings.ui("This is the hideout — search the venues here to catch them red-handed."), false)

    return null
}

/** During a guided step, only the control that step is about is tappable — everything else on the
 *  toolbar is disabled so the Rookie can't wander off. Tools: 0 SEE · 1 DEPART · 2 INVESTIGATE ·
 *  3 CRIME. Returns true (all enabled) whenever the tour is off or the current step doesn't force. */
fun tourAllowsTool(s: GameState, tool: Int): Boolean {
    if (!s.tutorialActive) return true
    val shown = lessonFor(s) ?: return true
    val allowed: Set<Int> = when (shown.id) {
        "wrongflight" -> setOf(1)                                     // fly back
        "interview" -> setOf(2)                                       // question witnesses
        "computer" -> if (s.phase == Phase.CRIME) emptySet() else setOf(3)  // open it, then stay in it
        "trail" -> setOf(1)                                           // depart
        "arrest" -> setOf(2)                                          // search to arrest
        else -> setOf(0, 1, 2, 3)                                     // info tips (clock, warrant): no lock
    }
    return tool in allowed
}

private class R(val x: Float, val y: Float, val w: Float, val h: Float)

// The game's fixed layout, in 320x200 virtual pixels (see GameScreens.kt).
private val TOOL_DEPART = R(190.75f, 163f, 41.75f, 32f)       // toolbar: plane / fly
private val TOOL_INVESTIGATE = R(232.5f, 163f, 41.75f, 32f)  // toolbar: magnifying glass
private val TOOL_CRIME = R(274.25f, 163f, 41.75f, 32f)       // toolbar: crime computer
private val CLOCK = R(4f, 13f, 141f, 30f)                    // top-left city name / clock box
private val CRT_COMPUTE = R(162f, 84f, 142f, 11f)          // crime computer: the COMPUTE row
private val TRAVEL_LIST = R(3f, 12f, 143f, 92f)            // travel screen: the destination dropdown

// The five CRT trait rows (image-relative y = 9 + i*10 inside the CRT box at 150,16 → absolute).
private fun crtRow(i: Int) = R(162f, 24f + i * 10f, 142f, 11f)
private fun rowIndexOf(cat: String) = when (cat) { "sex" -> 0; "hobby" -> 1; "hair" -> 2; "feature" -> 3; else -> 4 }
private fun rowLabelOf(cat: String) = listOf("SEX", "HOBBY", "HAIR", "FEATURE", "VEHICLE")[rowIndexOf(cat)]
private fun compVal(s: GameState, cat: String) = when (cat) {
    "sex" -> s.compSex; "hobby" -> s.compHobby; "hair" -> s.compHair; "feature" -> s.compFeature; else -> s.compVehicle
}

private const val CARET_W = 9f
private const val CARET_H = 4.5f

/** A small triangle pointing at the target, sitting on the card edge nearest it. */
@Composable
private fun Caret(v: Virtual, up: Boolean, localX: Float) {
    Box(Modifier.fillMaxWidth().height(v.w(CARET_H))) {
        Canvas(Modifier.offset(v.w(localX), v.w(0)).size(v.w(CARET_W), v.w(CARET_H))) {
            val w = size.width; val h = size.height
            val path = Path().apply {
                if (up) { moveTo(w / 2f, 0f); lineTo(0f, h); lineTo(w, h) }
                else { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w / 2f, h) }
                close()
            }
            // Up-caret merges into the yellow header; down-caret is the panel narrowing to a point.
            drawPath(path, if (up) Vga.Yellow else Vga.Black)
            if (!up) {
                val stroke = h * 0.16f
                drawLine(Vga.White, Offset(0f, 0f), Offset(w / 2f, h), stroke)
                drawLine(Vga.White, Offset(w, 0f), Offset(w / 2f, h), stroke)
            }
        }
    }
}

@Composable
private fun TipPanel(v: Virtual, vm: ClaraViewModel, shown: Shown) {
    Column(
        Modifier.fillMaxWidth()
            .border(BorderStroke(v.w(1.5f), Vga.White)).background(Vga.Black)
            .padding(v.w(1.2f)).border(BorderStroke(v.w(0.6f), Vga.Yellow)),
    ) {
        // ---- header: a little "?" badge + the lesson title ----
        Row(
            Modifier.fillMaxWidth().background(Vga.Yellow).padding(horizontal = v.w(5), vertical = v.w(3.2f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(v.w(7f)).background(Vga.Black), contentAlignment = Alignment.Center) {
                Text("?", style = v.text(6.5f, Vga.Yellow, bold = true))
            }
            Spacer(Modifier.width(v.w(4)))
            Text(shown.title, style = v.text(9, Vga.Black, bold = true))
        }
        // ---- body ----
        Text(shown.text, style = v.text(8, Vga.White),
            modifier = Modifier.padding(horizontal = v.w(6), vertical = v.w(4.5f)))
        // ---- footer: GOT IT (info tips) or a "do it" nudge, plus Skip tour ----
        Row(
            Modifier.fillMaxWidth().padding(start = v.w(6), end = v.w(5), bottom = v.w(4)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (shown.info) Pill(v, Strings.ui("GOT IT"), primary = true) { vm.dismissTip(shown.id) }
            else Text("▸ " + Strings.ui("try it"), style = v.text(6.5f, Vga.LightGreen, bold = true))
            Spacer(Modifier.weight(1f))
            Pill(v, Strings.ui("Skip tour"), primary = false) { vm.skipTutorial() }
        }
    }
}

/** A small DOS-flavoured button: primary = yellow fill + red label; ghost = outlined + grey label. */
@Composable
private fun Pill(v: Virtual, label: String, primary: Boolean, onClick: () -> Unit) {
    if (primary) Box(
        Modifier.background(Vga.Yellow).border(BorderStroke(v.w(0.8f), Vga.Black))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .labelled(label).padding(horizontal = v.w(6), vertical = v.w(2.6f)),
    ) { Text(label, style = v.text(7.5f, Vga.Red, bold = true)) }
    else Box(
        Modifier.border(BorderStroke(v.w(0.6f), Vga.LightGray))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .tappable(label).padding(horizontal = v.w(5), vertical = v.w(2.6f)),
    ) { Text(label, style = v.text(7f, Vga.LightGray, bold = true)) }
}

/** A dim band; skips zero/negative rects and swallows taps so only the spotlight is tappable. */
@Composable
private fun Dim(v: Virtual, x: Float, y: Float, w: Float, h: Float) {
    if (w <= 0.4f || h <= 0.4f) return
    place(v, x, y, w, h) {
        Box(Modifier.fillMaxSize().background(Vga.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {})
    }
}

/** Position a box at a virtual rect (clamped into the canvas). */
@Composable
private fun place(v: Virtual, x: Float, y: Float, w: Float, h: Float, content: @Composable BoxScope.() -> Unit) {
    val cx = x.coerceIn(0f, 320f); val cy = y.coerceIn(0f, 200f)
    v.At(cx, cy, (x + w - cx).coerceAtMost(320f - cx).coerceAtLeast(0f),
        (y + h - cy).coerceAtMost(200f - cy).coerceAtLeast(0f), content = content)
}
