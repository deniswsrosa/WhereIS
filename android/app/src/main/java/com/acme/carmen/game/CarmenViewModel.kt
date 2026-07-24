package com.acme.carmen.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.acme.carmen.data.CityMeta
import com.acme.carmen.data.GameData
import com.acme.carmen.data.Suspect
import com.acme.carmen.data.WorldMap
import kotlin.random.Random

enum class Phase { INTRO, TITLE, SIGN_ON, BRIEFING, CITY, TRAVEL, CRIME, CHASE, RESULT }

/** Event stingers from the original MIDISND.DAT, mapped to game moments (the UI layer
 *  resolves each to its res/raw MIDI and plays it over the theme). */
enum class SoundCue {
    BRIEFING, FLASH, CLUE, DANGER, WARRANT, ARRIVE, TRAVEL, CHASE, WIN, WRONG_ARREST, OUT_OF_TIME
}

enum class ClueKind { DESTINATION, TRAIT, DANGER, NONE }

/** Menu-bar overlays. */
sealed interface Overlay {
    data object About : Overlay
    data object Roster : Overlay
    data object HallOfFame : Overlay
    /** Game > Quit — the original's "Do you really want to quit?" Yes/No dialog. */
    data object ConfirmQuit : Overlay
    /** One suspect's dossier — the white typed-on window from the original's Dossiers menu. */
    data class Dossier(val suspect: Suspect) : Overlay
    data class Info(val title: String, val lines: List<String>) : Overlay
}

/** One venue a player can walk into in a city. */
data class Venue(
    val place: String,       // e.g. "Bank"
    val occupation: String,  // e.g. "Bank Guard"
    val kind: ClueKind,
    val text: String,        // fully assembled witness line
    val trait: Pair<String, String>? = null, // (category,value) if TRAIT
)

/** National treasures — remake-authored (the Enhanced build stores none as strings). */
object Treasures {
    val list = listOf(
        "the Crown Jewels", "a priceless Ming vase", "the Star of Africa diamond",
        "an ancient golden mask", "a rare Stradivarius violin", "the original Magna Carta",
        "a jewel-encrusted scepter", "a set of Fabergé eggs", "a stolen Rembrandt",
        "the sacred temple ruby", "a 2,000-year-old mummy", "the national flag",
    )
}

data class GameState(
    val phase: Phase = Phase.INTRO,
    val detectiveName: String = "",
    val rankIndex: Int = 0,
    val casesSolved: Int = 0,
    // case
    val culprit: Suspect? = null,
    val treasure: String = "",
    val route: List<String> = emptyList(),
    val progress: Int = 0,              // furthest correct index reached
    val currentCity: String = "",
    val clock: Int = 0,                 // hours since Monday 9:00
    val onTrack: Boolean = true,
    // discovery
    val revealOrder: List<Pair<String, String>> = emptyList(), // culprit traits, discriminating first
    val revealedCount: Int = 0,
    // venues at current city
    val venues: List<Venue> = emptyList(),
    val visited: Set<Int> = emptySet(),
    val openClue: Venue? = null,        // overlay
    // crime computer
    val compSex: String? = null,
    val compHobby: String? = null,
    val compHair: String? = null,
    val compFeature: String? = null,
    val compVehicle: String? = null,
    val warrantFor: Suspect? = null,
    val computed: Boolean = false,
    // travel animation: destination while the red route line is being drawn (null = not flying)
    val flying: String? = null,
    val flightHours: Int = 0,
    // stable destination set for this city — the SEE dropdown and the DEPART list share it
    val departOptions: List<String> = emptyList(),
    // sighting interstitial to play before the witness: 0 none · 1 masked face · 2 thug ·
    // 3 burglar with sack · 4 dagger (hideout wrong venue) — DOS escalation ladder
    val sightingLevel: Int = 0,
    // Carmen herself was jailed on the final case: the career is over and the detective
    // is retired from the roster (back to the title after the report)
    val careerOver: Boolean = false,
    // toolbar green selection border follows the last activated tool (0 SEE · 1 DEPART ·
    // 2 INVESTIGATE · 3 CRIME); arriving in a city resets it to INVESTIGATE
    val selectedTool: Int = 2,
    // the overnight clamp fired: the city box shows "SLEEPING…" briefly
    val sleeping: Boolean = false,
    // result
    val won: Boolean = false,
    val resultLines: List<String> = emptyList(),
    // a promotion was earned and awaits the almanac quiz (original: "one more clue to unravel")
    val pendingPromotion: Boolean = false,
    // ui
    val overlay: Overlay? = null,
    val soundOn: Boolean = true,
) {
    val revealedTraits: List<Pair<String, String>> get() = revealOrder.take(revealedCount)
    val deadlinePassed: Boolean get() = clock > DEADLINE_HOURS
    val hideout: String get() = route.lastOrNull() ?: ""
    val atHideout: Boolean get() = currentCity == hideout && onTrack
    companion object {
        const val DEADLINE_HOURS = 152     // Mon 9am -> Sun 5pm
        // Career length: promotions at 1, 5, 9, 13 solved (4 cases per middle rank); the
        // first case as Ace Detective is always Carmen Sandiego herself — jail her and the
        // career ends in the Hall of Fame (1990 revised rules).
        const val CAREER_CASES = 14
    }
}

class CarmenViewModel : ViewModel() {
    var s by mutableStateOf(GameState())
        private set

    // One-shot sound cue for the UI to play. The seq makes each emit distinct so a repeated
    // cue (e.g. two clues in a row) still re-triggers the LaunchedEffect that observes it.
    var soundCue by mutableStateOf<Pair<Int, SoundCue>?>(null)
        private set
    private var cueSeq = 0
    private fun cue(c: SoundCue) { cueSeq++; soundCue = cueSeq to c }

    // ---------- flow ----------
    fun introDone() { if (s.phase == Phase.INTRO) s = s.copy(phase = Phase.TITLE) }
    fun start() { s = s.copy(phase = Phase.SIGN_ON) }

    fun signOn(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        s = GameState(phase = Phase.BRIEFING, detectiveName = nm)
        newCase()
    }

    /**
     * Sign-on used by the HQ printer teletype: register the detective name and generate the
     * first case, but stay on the SIGN_ON screen so the same printer keeps printing the
     * briefing (faithful to the original — the computer never switches to a different UI).
     */
    fun signOnStart(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        s = GameState(phase = Phase.SIGN_ON, detectiveName = nm)
        newCase()                          // newCase() flips phase to BRIEFING...
        s = s.copy(phase = Phase.SIGN_ON)  // ...keep the printer on-screen until "begin"
    }

    fun beginInvestigation() { s = s.copy(phase = Phase.CITY) }

    // ---------- navigation ----------
    fun gotoCity() { s = s.copy(phase = Phase.CITY) }
    fun gotoTravel() { s = s.copy(phase = Phase.TRAVEL, selectedTool = 1) }
    // The player fills in the suspect's description themselves — the computer is not pre-populated.
    fun gotoCrime() { s = s.copy(phase = Phase.CRIME, selectedTool = 3); cue(SoundCue.FLASH) }
    fun selectTool(i: Int) { s = s.copy(selectedTool = i) }

    // ---------- menu bar ----------
    fun openOverlay(o: Overlay) { s = s.copy(overlay = o) }
    fun dismissOverlay() { s = s.copy(overlay = null) }
    fun menuNewCase() { s = s.copy(overlay = null, phase = Phase.BRIEFING); newCase(); cue(SoundCue.BRIEFING) }
    fun menuQuitToTitle() { s = GameState(phase = Phase.TITLE) }
    // Options > Sound is a silent checkmark toggle in the original (the √ beside the item
    // reflects the state); the actual mute is applied by the audio engine in the UI layer.
    fun toggleSound() { s = s.copy(soundOn = !s.soundOn, overlay = null) }
    fun showJoystick() {
        s = s.copy(overlay = Overlay.Info("JOYSTICK",
            listOf("Joystick improperly centered", "or not present.")))
    }

    // ---------- case generation ----------
    private fun newCase() {
        val carmen = GameData.suspects.first { it.name == "Carmen Sandiego" }
        val pool = GameData.suspects.filter { it.name != "Carmen Sandiego" }
        // Carmen is only ever the culprit on the very last case of the career (1990 rules:
        // catching her is guaranteed, then the detective is retired from the roster)
        val culprit = if (s.casesSolved >= GameState.CAREER_CASES - 1) carmen else pool.random()

        val order = discriminatingOrder(culprit)
        // cities per case by rank (ADG analysis of the 1990 release): Rookie 5, Sleuth 6,
        // Private Eye 7, Investigator 8, Ace Detective 9
        val routeLen = (5 + s.rankIndex).coerceAtMost(9)

        val cities = GameData.cities.shuffled().take(routeLen)
        android.util.Log.d("Carmen", "case: culprit=${culprit.name} route=$cities")
        s = s.copy(
            phase = Phase.BRIEFING,
            culprit = culprit,
            treasure = Treasures.list.random(),
            route = cities,
            progress = 0,
            currentCity = cities.first(),
            clock = 0,
            onTrack = true,
            revealOrder = order,
            revealedCount = 0,
            visited = emptySet(),
            openClue = null,
            compSex = null, compHobby = null, compHair = null, compFeature = null, compVehicle = null,
            warrantFor = null, computed = false, won = false, resultLines = emptyList(),
            selectedTool = 2, sightingLevel = 0, sleeping = false, careerOver = false,
        )
        buildVenues()
        s = s.copy(departOptions = makeDepartOptions())
    }

    /** Order culprit traits so a prefix uniquely identifies them among all 10 suspects. */
    private fun discriminatingOrder(c: Suspect): List<Pair<String, String>> {
        val cats = linkedMapOf(
            "sex" to c.tSex, "hobby" to c.tHobby, "hair" to c.tHair,
            "feature" to c.tFeature, "vehicle" to c.tVehicle,
        )
        fun value(su: Suspect, cat: String) = when (cat) {
            "sex" -> su.tSex; "hobby" -> su.tHobby; "hair" -> su.tHair
            "feature" -> su.tFeature; else -> su.tVehicle
        }
        val chosen = mutableListOf<Pair<String, String>>()
        var candidates = GameData.suspects
        val remaining = cats.keys.toMutableList()
        while (candidates.size > 1 && remaining.isNotEmpty()) {
            // pick the category that shrinks the candidate set the most
            val best = remaining.minByOrNull { cat ->
                candidates.count { value(it, cat) == cats[cat] }
            }!!
            remaining.remove(best)
            chosen.add(best to cats[best]!!)
            candidates = candidates.filter { value(it, best) == cats[best] }
        }
        // append leftover traits (flavour / redundancy)
        remaining.forEach { chosen.add(it to cats[it]!!) }
        return chosen
    }

    // ---------- venues per city ----------
    private fun buildVenues() {
        val st = s
        val places = GameData.venues.shuffled().take(3)
        // each venue is staffed by one of its own witnesses (Harbor -> Sailor etc., like the original)
        val occs = places.map { p ->
            (GameData.venueOccupations[p] ?: GameData.occupations).random()
        }
        val list = mutableListOf<Venue>()
        val onTrack = st.currentCity == st.route.getOrNull(st.progress) && st.onTrack

        if (!onTrack) {
            places.forEachIndexed { i, p ->
                // wrong city: each venue answers with its own DOS no-information line
                val line = GameData.noInformationByVenue[p] ?: GameData.noInformation.random()
                list.add(Venue(p, occs[i], ClueKind.NONE, line))
            }
        } else if (st.currentCity == st.hideout) {
            // Hideout city: every venue shows the special line until the crook is found
            // (which venue that is gets decided in openVenue — never the first pick,
            // 50/50 on the second, certain on the third, per the 1990 release's rules)
            places.forEachIndexed { i, p ->
                list.add(Venue(p, occs[i], ClueKind.DANGER,
                    "Rumor has it that the gang is in town somewhere."))
            }
        } else {
            val nextCity = st.route.getOrNull(st.progress + 1)
            var slot = 0
            places.forEachIndexed { i, p ->
                val occ = occs[i]
                if (slot == 0 && nextCity != null) {
                    slot++
                    list.add(Venue(p, occ, ClueKind.DESTINATION, destinationClue(nextCity)))
                } else {
                    // trait clue if any left, else danger/flavour
                    val idx = st.revealedCount + list.count { it.kind == ClueKind.TRAIT }
                    val tr = st.revealOrder.getOrNull(idx)
                    if (tr != null) {
                        list.add(Venue(p, occ, ClueKind.TRAIT, traitClue(tr), tr))
                    } else {
                        // out of discriminating traits: a food/flavour remark (still a full
                        // DOS sentence, so no extra lead-in — matches "She mentioned…")
                        list.add(Venue(p, occ, ClueKind.DANGER, "${flavourFood(st.culprit!!)}."))
                    }
                }
            }
        }
        s = s.copy(venues = list, visited = emptySet(), openClue = null)
    }

    private fun destinationClue(next: String): String {
        val info = CityMeta.of(next)
        val lead = GameData.clueLeadIns.random()
        // Like the original, the destination is never named outright — the witness cites a
        // fact you look up (a region + a distinctive landmark), phrased a few different ways.
        val frag = pronouns(when (Random.nextInt(3)) {
            0 -> "{s} was headed for a country in ${info.region}"
            1 -> "{s} planned to visit a place known for ${info.landmark}"
            else -> "{s} was headed somewhere in ${info.region}, near ${info.landmark}"
        })
        return "$lead $frag."
    }

    /** Substitute the DOS pronoun slots for the culprit's sex. {S}=She/He (sentence start),
     *  {s}=she/he, {p}=her/his. The original leaks the suspect's sex through these pronouns. */
    private fun pronouns(frag: String): String {
        val female = s.culprit?.sex == "Female"
        return frag.replace("{S}", if (female) "She" else "He")
            .replace("{s}", if (female) "she" else "he")
            .replace("{p}", if (female) "her" else "his")
    }

    private fun traitClue(tr: Pair<String, String>): String {
        val (cat, v) = tr
        // §19: sex is never its own clue in the original — it rides inside every trait
        // sentence's pronouns. For a bare sex trait, fall back to a jewelry-neutral remark.
        val frags = GameData.traitClueFragments["$cat:$v"]
        val frag = if (frags != null) pronouns(frags.random())
            else pronouns(when (cat) {
                "sex" -> "{S} looked like the person you're after"
                "hair" -> "{S} had $v hair"
                else -> "{S} matched your description"
            })
        // ~⅓ of DOS trait lines are the bare sentence; the rest carry a lead-in
        val lead = GameData.clueLeadIns.random()
        return if (Random.nextInt(3) == 0) "$frag." else "$lead ${frag.replaceFirstChar { it.lowercase() }}."
    }

    private fun flavourFood(c: Suspect): String {
        val f = (c.feature2 + " " + c.feature1).lowercase()
        val frag = when {
            "taco" in f || "mexican" in f -> "{S} mentioned {s} liked Mexican food"
            "seafood" in f || "shellfish" in f || "lobster" in f -> "{S} mentioned {s} liked seafood"
            "spicy" in f -> "{S} mentioned {s} liked spicy food"
            else -> "{S} said {s} didn't like seafood"
        }
        return pronouns(frag)
    }

    // ---------- player actions in a city ----------
    fun openVenue(index: Int) {
        val v = s.venues.getOrNull(index) ?: return
        // Hideout city (1990 rules, per the ADG analysis): the crook is NEVER at the first
        // venue you try, the second is a 50/50 coin flip, the third is certain. Catching
        // them happens instantly and costs no time.
        if (s.atHideout && index !in s.visited) {
            val caught = when (s.visited.size) {
                0 -> false
                1 -> Random.nextBoolean()
                else -> true
            }
            if (caught) { confront(); return }
        }
        // clue vs. warning stinger, keyed on what this venue's witness will say
        cue(if (v.kind == ClueKind.DANGER) SoundCue.DANGER else SoundCue.CLUE)
        var st = s
        if (index !in st.visited && v.kind == ClueKind.TRAIT)
            st = st.copy(revealedCount = st.revealedCount + 1)
        // DOS time costs: first venue visit in a city 2 h, every visit after that 3 h
        st = advanceClock(st, if (st.visited.isEmpty()) 2 else 3)
        st = st.copy(visited = st.visited + index)
        // sighting escalation ladder: plays in the panel before the witness pops up
        val dist = st.route.size - 1 - st.progress
        val level = when {
            st.atHideout -> 4                          // dagger + "Rumor has it..."
            st.onTrack && dist in 1..3 -> 4 - dist     // 3 away: face · 2: thug · 1: burglar
            else -> 0
        }
        s = st.copy(openClue = v, sightingLevel = level)
        checkDeadline()
    }

    fun closeClue() { s = s.copy(openClue = null) }

    /** The sighting interstitial finished — reveal the witness. */
    fun sightingDone() { s = s.copy(sightingLevel = 0) }

    /** The "SLEEPING…" overlay in the city box has been shown. */
    fun sleepingShown() { s = s.copy(sleeping = false) }

    /** Advance the clock; landing in the 10 p.m. – 8 a.m. window rolls forward to 8 a.m.
     *  (the detective sleeps) and flags the transient SLEEPING… display. */
    private fun advanceClock(st: GameState, hours: Int): GameState {
        var clock = st.clock + hours
        val hour = (9 + clock) % 24
        var slept = false
        if (hour >= 22) { clock += (24 - hour) + 8; slept = true }
        else if (hour < 8) { clock += 8 - hour; slept = true }
        return st.copy(clock = clock, sleeping = st.sleeping || slept)
    }

    // ---------- travel ----------
    /** Build this city's destination set once per arrival — the SEE dropdown and the DEPART
     *  list must show the same, stable connections (regenerating each frame is un-DOS).
     *  Like the original's flight matrix, a city links to 2-4 others, and a wrong flight can
     *  never land you somewhere you're supposed to go later in the case (decoys exclude the
     *  whole route). */
    private fun makeDepartOptions(): List<String> {
        val next = when {
            s.progress < s.route.size - 1 -> s.route[s.progress + 1]
            s.currentCity != s.hideout -> s.hideout   // strayed after the hideout: allow the way back
            else -> null
        }
        val links = Random.nextInt(2, 5)              // 2-4 connections, like the original
        val decoys = GameData.cities
            .filter { it != next && it != s.currentCity && it !in s.route }
            .shuffled().take(if (next != null) links - 1 else links)
        return if (next != null) (decoys + next).shuffled() else decoys
    }

    /** Flight time from the current city, scaled by map distance (the original's travel
     *  times depend on how far apart the cities are; short hops ~2-3 h). Deterministic, so
     *  the DEPART preview shows exactly what the flight will cost. */
    fun flightHoursTo(city: String): Int {
        val a = WorldMap.pos[s.currentCity]
        val b = WorldMap.pos[city]
        return if (a != null && b != null) {
            val d = kotlin.math.hypot(((a.x - b.x) * 2f).toDouble(), (a.y - b.y).toDouble())
            (2 + d * 6).toInt().coerceIn(2, 14)
        } else 4
    }

    /** Hours remaining before the Sunday 5 p.m. deadline. */
    fun hoursLeft(): Int = (GameState.DEADLINE_HOURS - s.clock).coerceAtLeast(0)

    /** Start the flight: the travel screen animates the red route line, then calls arrive(). */
    fun travelTo(city: String) {
        if (s.flying != null) return
        s = s.copy(flying = city, flightHours = flightHoursTo(city))
        cue(SoundCue.TRAVEL)
    }

    /** Flight animation finished: apply the arrival. */
    fun arrive() {
        val city = s.flying ?: return
        val correct = s.route.getOrNull(s.progress + 1)
        // overnight rule (observed in the original): landing between 10 p.m. and 8 a.m.
        // rolls the clock forward to 8 a.m. — the detective rests for the night
        var st = advanceClock(s, s.flightHours).copy(flying = null, flightHours = 0)
        st = when {
            city == correct ->
                st.copy(progress = st.progress + 1, currentCity = city, onTrack = true)
            city == st.hideout && st.progress == st.route.size - 1 ->
                st.copy(currentCity = city, onTrack = true)   // flew back to the hideout
            else -> st.copy(currentCity = city, onTrack = false)
        }
        // DOS: after arriving in a new city the toolbar selection is INVESTIGATE
        s = st.copy(selectedTool = 2)
        if (s.deadlinePassed) { escaped("time"); return }
        s = s.copy(phase = Phase.CITY)
        buildVenues()
        s = s.copy(departOptions = makeDepartOptions())
        cue(SoundCue.ARRIVE)
    }

    // ---------- crime computer ----------
    fun setComp(cat: String, value: String?) {
        s = when (cat) {
            "sex" -> s.copy(compSex = value); "hobby" -> s.copy(compHobby = value)
            "hair" -> s.copy(compHair = value); "feature" -> s.copy(compFeature = value)
            else -> s.copy(compVehicle = value)
        }.copy(computed = false)
    }

    fun matches(): List<Suspect> = GameData.suspects.filter { su ->
        (s.compSex == null || su.tSex == s.compSex) &&
        (s.compHobby == null || su.tHobby == s.compHobby) &&
        (s.compHair == null || su.tHair == s.compHair) &&
        (s.compFeature == null || su.tFeature == s.compFeature) &&
        (s.compVehicle == null || su.tVehicle == s.compVehicle)
    }

    /**
     * Run the crime computer. Faithful to the original: the printer prints the matching
     * suspects, and when the description narrows to exactly one, Interpol automatically
     * issues the arrest warrant ("You now have a warrant to arrest X.").
     */
    fun compute() {
        // DOS COMPUTE costs 3 h (observed 2 p.m. -> 5 p.m., and 10 p.m. -> next morning)
        var st = advanceClock(s.copy(computed = true), 3)
        val m = GameData.suspects.filter { su ->
            (st.compSex == null || su.tSex == st.compSex) &&
            (st.compHobby == null || su.tHobby == st.compHobby) &&
            (st.compHair == null || su.tHair == st.compHair) &&
            (st.compFeature == null || su.tFeature == st.compFeature) &&
            (st.compVehicle == null || su.tVehicle == st.compVehicle)
        }
        val issuedWarrant = m.size == 1 && anyFilterSet()
        if (issuedWarrant) st = st.copy(warrantFor = m.first())
        s = st
        if (issuedWarrant) cue(SoundCue.WARRANT)   // "You now have a warrant to arrest X."
        checkDeadline()
    }

    fun anyFilterSet(): Boolean =
        listOf(s.compSex, s.compHobby, s.compHair, s.compFeature, s.compVehicle).any { it != null }

    // ---------- endings ----------
    private fun checkDeadline() { if (s.deadlinePassed) escaped("time") }

    /** Arrived at the hideout: play the chase animation first, then show the result. */
    private fun confront() {
        val c = s.culprit!!
        val w = s.warrantFor
        when {
            w == null -> {
                s = s.copy(won = false, resultLines = listOf(
                    "Interpol here.",
                    "You have caught up with ${c.name}.",
                    GameData.NO_WARRANT.let { "However, without a warrant we cannot make a legal arrest!" },
                    "It looks like the gang has gotten away with another caper!",
                ))
            }
            w.name != c.name -> {
                s = s.copy(won = false, resultLines = listOf(
                    "You have trailed the suspect to ${s.currentCity}.",
                    "Unfortunately, you have a warrant for ${w.name}.",
                    "Be careful, we could all be sued for false arrest!",
                    "We hope you do better on your next case.",
                ))
            }
            else -> win(c)
        }
        // win() sets phase=RESULT; route everything through the chase animation instead
        s = s.copy(phase = Phase.CHASE)
        cue(SoundCue.CHASE)
    }

    /** Chase animation finished (or was tapped through) — show the Interpol report. */
    fun chaseDone() {
        if (s.phase != Phase.CHASE) return
        s = s.copy(phase = Phase.RESULT)
        // the report's outcome cue: triumphant on a win, the botched-arrest sting otherwise
        cue(if (s.won) SoundCue.WIN else SoundCue.WRONG_ARREST)
    }

    private fun win(c: Suspect) {
        val crimeCity = s.route.firstOrNull() ?: s.currentCity
        val lines = mutableListOf<String>()
        if (c.name == "Carmen Sandiego") {
            lines += "You have successfully arrested the ring-leader, Carmen Sandiego, and sent her to jail for good!"
            lines += "Congratulations, your name will go into the Interpol Hall of Fame!"
        } else {
            // faithful phrasing: the CRIME city's police make the arrest and get the loot back
            lines += "Thanks to your help, the $crimeCity police have apprehended ${c.name}."
            lines += "${c.name} had the loot, ${s.treasure}, which will be returned to the grateful residents of $crimeCity."
        }
        lines += "We here at Interpol thank you for your good work on this case."
        val newCases = s.casesSolved + 1
        // jailing Carmen herself concludes the career — no promotion, straight to the
        // Hall of Fame report and off the roster
        val careerOver = c.name == "Carmen Sandiego"
        // promotion cadence observed in the original: after case 1, then "four more cases
        // until your next promotion" — thresholds 1, 5, 9, 13
        val promote = !careerOver && s.rankIndex < GameData.ranks.lastIndex && newCases in setOf(1, 5, 9, 13)
        if (promote) {
            lines += "Good job, ${s.detectiveName}, you have earned a promotion."
            lines += "Before you are promoted you have one more clue to unravel."
        }
        s = s.copy(phase = Phase.RESULT, won = true, casesSolved = newCases,
            resultLines = lines, pendingPromotion = promote, careerOver = careerOver)
    }

    /** The promotion quiz was answered: bump the rank only when correct (like the original). */
    fun resolvePromotion(correct: Boolean) {
        val newRank = if (correct && s.rankIndex < GameData.ranks.lastIndex) s.rankIndex + 1 else s.rankIndex
        s = s.copy(rankIndex = newRank, pendingPromotion = false)
    }

    /** Cases remaining until the next promotion threshold (1, 5, 9, 13). */
    fun casesToNextPromotion(): Int {
        val next = listOf(1, 5, 9, 13).firstOrNull { it > s.casesSolved } ?: return 0
        return next - s.casesSolved
    }

    private fun escaped(reason: String) {
        val c = s.culprit!!
        val lines = if (reason == "time") listOf(
            "Message from Interpol:", "Bad news...",
            "We've just received word that ${c.name} slipped through your fingers because your investigation took too long!",
        ) else listOf("The suspect got away!")
        s = s.copy(phase = Phase.RESULT, won = false, resultLines = lines)
        cue(SoundCue.OUT_OF_TIME)
    }

    fun nextCase() { newCase() }
    fun toBriefingForNext() { s = s.copy(phase = Phase.BRIEFING); newCase(); cue(SoundCue.BRIEFING) }

    // ---------- time formatting ----------
    fun clockLabel(offsetHours: Int = 0): String {
        val total = 9 + s.clock + offsetHours
        val day = (total / 24).coerceIn(0, 6)
        val hour = total % 24
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val (h, ampm) = when {
            hour == 0 -> 12 to "a.m."
            hour < 12 -> hour to "a.m."
            hour == 12 -> 12 to "p.m."
            else -> hour - 12 to "p.m."
        }
        return "${days[day]}, $h $ampm"
    }

    /** Compact time-until-deadline hint, e.g. "3d 4h left" or "18h left" when close. */
    fun deadlineLabel(offsetHours: Int = 0): String {
        val left = (GameState.DEADLINE_HOURS - s.clock - offsetHours).coerceAtLeast(0)
        return if (left >= 24) "${left / 24}d ${left % 24}h left" else "${left}h left"
    }
}
