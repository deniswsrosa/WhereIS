package com.acme.clara.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.acme.clara.data.CityInfo
import com.acme.clara.data.CityMeta
import com.acme.clara.data.Expansion
import com.acme.clara.data.Expansion2
import com.acme.clara.data.GameData
import com.acme.clara.data.Progression
import com.acme.clara.data.Suspect
import com.acme.clara.data.WorldMap
import com.acme.clara.save.SaveData
import com.acme.clara.save.SaveMeta
import com.acme.clara.save.SaveRepository
import kotlin.random.Random

enum class Phase { INTRO, TITLE, SIGN_ON, BRIEFING, CITY, TRAVEL, CRIME, CHASE, RESULT, CHOOSE_GAME }

/** Event stingers mapped to game moments (the UI layer resolves each to its
 *  assets/audio/ MIDI file and plays it over the theme). */
enum class SoundCue {
    BRIEFING, FLASH, CLUE, DANGER, WARRANT, ARRIVE, TRAVEL, CHASE, WIN, WRONG_ARREST, OUT_OF_TIME
}

enum class ClueKind { DESTINATION, TRAIT, DANGER, NONE }

/** Menu-bar overlays. */
sealed interface Overlay {
    data object About : Overlay
    data object Roster : Overlay
    data object HallOfFame : Overlay
    /** The Most-Wanted gallery — villains reveal as they're captured. */
    data object MostWanted : Overlay
    /** Commendations earned + career stats. */
    data object Commendations : Overlay
    /** The browsable world database (in-game almanac). */
    data object Almanac : Overlay
    /** Passport (C4): the painted world map of countries visited. */
    data object Passport : Overlay
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

/** One line the detective has logged this case — a lead or a trait, tagged by city.
 *  Feeds the case journal / "Previously…" recap so a case survives a break away. */
data class JournalEntry(val kind: ClueKind, val text: String, val city: String)

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
    val hapticsOn: Boolean = true,
    val captionsOn: Boolean = false,   // Options ▸ Captions: on-screen text for audio cues
    // per-case tallies (reset every newCase) — feed stats, achievements, the share card
    val wrongFlights: Int = 0,
    val hintsUsed: Int = 0,
    // the running case journal: leads and traits as they're uncovered (reset per case)
    val journal: List<JournalEntry> = emptyList(),
    // career record — persists across cases within a saved profile
    val capturedVillains: Set<String> = emptySet(),
    val unlockedAchievements: Set<String> = emptySet(),
    // Passport (C4): every place the detective has ever landed in, across the whole career and
    // both tiers. Recorded silently from day one on the free tier; on the paid unlock the
    // world map paints in every country already visited here. Place names -> countries via
    // data.CountryShapes.placeCountry.
    val visitedPlaces: Set<String> = emptySet(),
    // H3 welcome-back warm-up: the next fresh case after a long absence is kinder (a shorter
    // route + one trait pre-solved). Set on resume() after a gap, consumed by newCase().
    val warmUpNextCase: Boolean = false,
    // H4 case-a-day streak: consecutive days with a solved case, a weekly "streak freeze" that
    // absorbs one missed day, and the epoch-day of the last solve (0 = never).
    val streakDays: Int = 0,
    val streakFreezes: Int = 0,
    val lastSolveEpochDay: Int = 0,
    // L4 spaced repetition: the case index (casesSolved) each place was last seen, so route
    // picking can resurface geography on an expanding schedule and the almanac can flag it.
    val cityLastSeen: Map<String, Int> = emptyMap(),
    // paid-tier entitlement: unlocks the 68 expansion destinations + new venues for case routes,
    // decoys, the map and flight times. Free play stays on the original 30. (Persisted with the
    // save until a global entitlement store exists.)
    val expansionUnlocked: Boolean = false,
    val hintFreeSolves: Int = 0,
    val hadCleanCase: Boolean = false,
    // free hints banked from returning after time away (spend without losing the hint-free badge)
    val freeHints: Int = 0,
    // the guided first case: tutorialStep 0..6 while it runs (-1 = none); tutorialDone once seen
    val tutorialDone: Boolean = false,
    val tutorialStep: Int = -1,
    // Level rules: the deadline for THIS case, set by Progression from the route's travel need +
    // the rank's slack (see docs/05-game-design-and-progression.md). 152 = the legacy fixed value.
    val caseDeadlineHours: Int = 152,
) {
    val revealedTraits: List<Pair<String, String>> get() = revealOrder.take(revealedCount)
    val deadlinePassed: Boolean get() = clock > caseDeadlineHours
    val hideout: String get() = route.lastOrNull() ?: ""
    val atHideout: Boolean get() = currentCity == hideout && onTrack
    companion object {
        const val DEADLINE_HOURS = 152     // Mon 9am -> Sun 5pm
        // Career length: promotions at 1, 5, 9, 13 solved (4 cases per middle rank); the
        // first case as Ace Detective is always Clara San Diego herself — jail her and the
        // career ends in the Hall of Fame (1990 revised rules).
        const val CAREER_CASES = 14
    }
}

class ClaraViewModel : ViewModel() {
    var s by mutableStateOf(GameState())
        private set

    // One-shot sound cue for the UI to play. The seq makes each emit distinct so a repeated
    // cue (e.g. two clues in a row) still re-triggers the LaunchedEffect that observes it.
    var soundCue by mutableStateOf<Pair<Int, SoundCue>?>(null)
        private set
    private var cueSeq = 0
    private fun cue(c: SoundCue) { cueSeq++; soundCue = cueSeq to c }

    // ---------- persistence (continuous autosave) ----------
    private var repo: SaveRepository? = null
    private var profileId: String? = null
    private var clock: () -> Long = { 0L }

    /** Wire continuous autosave to a repository + profile. A no-op until attached (e.g. in tests). */
    fun attachSave(repository: SaveRepository, id: String, now: () -> Long = { System.currentTimeMillis() }) {
        repo = repository; profileId = id; clock = now
    }

    /** Bind the repository without picking a profile yet (the launch flow chooses one). */
    fun bindRepository(repository: SaveRepository, now: () -> Long = { System.currentTimeMillis() }) {
        repo = repository; clock = now
    }

    private fun newProfileId(): String = "career-" + java.util.UUID.randomUUID().toString().take(8)

    /** Existing saved careers, newest first (empty when no repository is bound). */
    fun savedGames(): List<SaveMeta> = repo?.list().orEmpty()

    /** Resume a saved career (launch continue / picker). Returning after time away banks a free hint. */
    fun resume(data: SaveData) {
        profileId = data.meta.id
        var st = data.state
        val backAfterGap = WelcomeBack.grantsHint(data.meta.lastPlayed, clock())
        // H3: after a long absence, bank a free hint and queue a kinder warm-up next case.
        if (backAfterGap) st = st.copy(freeHints = st.freeHints + 1, warmUpNextCase = true)
        // P5 recap card: the game already logs the whole trail + traits (CaseJournal); draw it
        // on return so a case survives the break, folding in the welcome-back hint if any.
        val recap = CaseJournal.recap(st)
        val caseUnderway = st.phase == Phase.CITY && st.route.isNotEmpty() && st.progress > 0
        st = if (recap != null && caseUnderway) {
            val trail = st.route.take(st.progress + 1).joinToString(" ▸ ")
            val traits = st.revealedTraits.joinToString(", ") { it.second }
            val lines = buildList {
                add(recap)
                add("")
                add("Trail: $trail")
                if (traits.isNotBlank()) add("Suspect so far: $traits")
                if (backAfterGap) { add(""); add("A fresh lead surfaced — here's a free hint to spend.") }
            }
            st.copy(overlay = Overlay.Info("PREVIOUSLY ON THIS CASE", lines))
        } else if (backAfterGap) {
            st.copy(overlay = Overlay.Info("WELCOME BACK", listOf(
                "Been a while, ${st.detectiveName}.",
                "A fresh lead has surfaced —",
                "here's a free hint to spend.",
            )))
        } else st
        s = st
    }

    /** Resume by id from the bound repository (used by the picker). */
    fun resumeById(id: String) { repo?.load(id)?.let { resume(it) } }

    /** Delete a saved career from the picker. */
    fun deleteGame(id: String) { repo?.delete(id) }

    /** Snapshot the current state as a save (transient UI/animation fields cleared). */
    fun snapshot(id: String, at: Long): SaveData = SaveData(
        SaveMeta(id, s.detectiveName, s.rankIndex, s.casesSolved, at),
        s.copy(overlay = null, openClue = null, flying = null, flightHours = 0,
            sightingLevel = 0, sleeping = false),
    )

    /** Load a saved career into this ViewModel (launch continue / picker). */
    fun loadCareer(data: SaveData) { profileId = data.meta.id; s = data.state }

    /** Persist the current state to the active profile — the state on disk always equals the screen. */
    private fun autosave() {
        val r = repo ?: return
        val id = profileId ?: return
        r.save(snapshot(id, clock()))
    }

    fun toggleHaptics() { s = s.copy(hapticsOn = !s.hapticsOn, overlay = null) }
    fun toggleCaptions() { s = s.copy(captionsOn = !s.captionsOn, overlay = null) }

    // ---------- tutorial ----------
    /** Advance the guided tutorial when the player performs the step's taught action. */
    private fun advanceTut(from: Int) { if (s.tutorialStep == from) s = s.copy(tutorialStep = from + 1) }
    fun skipTutorial() { s = s.copy(tutorialStep = -1, tutorialDone = true); autosave() }

    /** Ask for a layered hint. Spends a banked free hint if there is one; otherwise it costs the
     *  case's hint-free badge. The hint is shown as an Info overlay. */
    fun requestHint() {
        val hint = hintText()
        val free = s.freeHints > 0
        s = if (free) s.copy(freeHints = s.freeHints - 1) else s.copy(hintsUsed = s.hintsUsed + 1)
        val note = if (free) "(free hint — your hint-free record is safe)"
                   else "(this case is no longer a hint-free solve)"
        s = s.copy(overlay = Overlay.Info("HINT", listOf(hint, "", note)))
        autosave()
    }

    /** A tiered hint: a computer nudge, a directional (region-only) lead, or an arrest prompt —
     *  never the next city outright, so it helps without solving the case. */
    private fun hintText(): String {
        val next = s.route.getOrNull(s.progress + 1)
        return when {
            s.atHideout && s.warrantFor?.name == s.culprit?.name ->
                "You've cornered the thief — search the venues here to make the arrest."
            s.atHideout ->
                "This is the hideout. Make sure your warrant names the right suspect before closing in."
            s.warrantFor == null && matches().size == 1 && anyFilterSet() ->
                "You have enough of the description — run the crime computer to issue the warrant."
            s.warrantFor == null ->
                "Question more witnesses; the crime computer still lists several suspects."
            next != null ->
                "The trail leads toward ${CityMeta.of(next).region}. Find a lead pointing that way."
            else ->
                "Follow your last lead to the thief's hideout."
        }
    }

    // ---------- flow ----------
    fun introDone() { if (s.phase == Phase.INTRO) s = s.copy(phase = Phase.TITLE) }
    fun start() { s = s.copy(phase = Phase.SIGN_ON) }

    fun signOn(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        if (profileId == null) profileId = newProfileId()
        s = GameState(phase = Phase.BRIEFING, detectiveName = nm)
        newCase()
        autosave()
    }

    /**
     * Sign-on used by the HQ printer teletype: register the detective name and generate the
     * first case, but stay on the SIGN_ON screen so the same printer keeps printing the
     * briefing (faithful to the original — the computer never switches to a different UI).
     */
    fun signOnStart(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        if (profileId == null) profileId = newProfileId()
        s = GameState(phase = Phase.SIGN_ON, detectiveName = nm)
        newCase()                          // newCase() flips phase to BRIEFING...
        s = s.copy(phase = Phase.SIGN_ON)  // ...keep the printer on-screen until "begin"
        autosave()
    }

    fun beginInvestigation() { s = s.copy(phase = Phase.CITY); advanceTut(0) }

    // ---------- navigation ----------
    fun gotoCity() { s = s.copy(phase = Phase.CITY) }
    fun gotoTravel() { s = s.copy(phase = Phase.TRAVEL, selectedTool = 1) }
    // The player fills in the suspect's description themselves — the computer is not pre-populated.
    fun gotoCrime() { s = s.copy(phase = Phase.CRIME, selectedTool = 3); cue(SoundCue.FLASH) }
    fun selectTool(i: Int) { s = s.copy(selectedTool = i) }

    // ---------- menu bar ----------
    fun openOverlay(o: Overlay) { s = s.copy(overlay = o); if (o is Overlay.Almanac) advanceTut(2) }
    fun dismissOverlay() { s = s.copy(overlay = null) }
    fun menuNewCase() { s = s.copy(overlay = null, phase = Phase.BRIEFING); newCase(); cue(SoundCue.BRIEFING); autosave() }

    /** Grant the paid-tier entitlement: the 68 expansion destinations + new venues enter the pool
     *  from the next case on. Called by the paywall after a successful purchase. Idempotent. */
    fun unlockExpansion() { if (!s.expansionUnlocked) { s = s.copy(expansionUnlocked = true); autosave() } }

    fun menuQuitToTitle() { profileId = null; s = GameState(phase = Phase.TITLE) }
    /** Game ▸ New Game — start a fresh career (a new saved profile). Names it on the sign-on screen. */
    fun newGameFlow() { profileId = null; s = GameState(phase = Phase.SIGN_ON) }
    /** Show the "Choose a game" picker. */
    fun toChooseGame() { s = s.copy(overlay = null, phase = Phase.CHOOSE_GAME) }
    // Options > Sound is a silent checkmark toggle in the original (the √ beside the item
    // reflects the state); the actual mute is applied by the audio engine in the UI layer.
    fun toggleSound() { s = s.copy(soundOn = !s.soundOn, overlay = null) }
    fun showJoystick() {
        s = s.copy(overlay = Overlay.Info("JOYSTICK",
            listOf("Joystick missing", "or badly centered.")))
    }

    // ---------- case generation ----------
    private fun newCase() {
        val carmen = GameData.suspects.first { it.name == "Clara San Diego" }
        val pool = GameData.suspects.filter { it.name != "Clara San Diego" }
        // Clara is the culprit on the final case of the free career (case 14). Catching her retires
        // free players; paid players are promoted into the International track and keep going — so she
        // is forced only on that one case, not on every case after it.
        val culprit = if (s.casesSolved == GameState.CAREER_CASES - 1) carmen else pool.random()

        val order = discriminatingOrder(culprit)
        // Route length per rank from the level rules (free 5..9, International 9..12); the H3
        // welcome-back warm-up trims one hop.
        val warm = s.warmUpNextCase
        val routeLen = (Progression.hops(s.rankIndex) - if (warm) 1 else 0).coerceAtLeast(4)

        // L4: pick the route on a spaced-repetition curve so geography recurs for review, then cap
        // brand-new (never-seen) countries to this rank's allowance so the world stays learnable.
        val cityPool = activeCities()
        val picked = SpacedRepetition.pickRoute(cityPool, s.cityLastSeen, s.casesSolved, routeLen)
        val cities = capNewPerCase(picked, s.cityLastSeen.keys, Progression.newPerCase(s.rankIndex), cityPool)
        // Level rules: the case deadline covers the route's real travel (+ overnight) plus this
        // rank's slack, so every case is completable and slack is the shrinking fairness margin.
        val flightSum = cities.zipWithNext { a, b -> flightCost(a, b) }.sum()
        val caseDeadline = Progression.caseDeadlineHours(s.rankIndex, flightSum, routeLen)
        // the guided first case runs once, on a brand-new career's opening Rookie case
        val isTutorial = !s.tutorialDone && s.casesSolved == 0 && s.rankIndex == 0
        android.util.Log.d("Carmen", "case: culprit=${culprit.name} route=$cities")
        s = s.copy(
            phase = Phase.BRIEFING,
            culprit = culprit,
            treasure = Treasures.list.random(),
            route = cities,
            progress = 0,
            currentCity = cities.first(),
            clock = 0,
            caseDeadlineHours = caseDeadline,
            onTrack = true,
            revealOrder = order,
            // H3 warm-up: the first (most telling) trait is already on the board.
            revealedCount = if (warm) 1 else 0,
            warmUpNextCase = false,
            visited = emptySet(),
            // Passport: the briefing city is the first place logged this case.
            visitedPlaces = s.visitedPlaces + cities.first(),
            // L4: mark the briefing city as seen this case.
            cityLastSeen = s.cityLastSeen + (cities.first() to s.casesSolved),
            wrongFlights = 0, hintsUsed = 0, journal = emptyList(),
            tutorialStep = if (isTutorial) 0 else -1, tutorialDone = s.tutorialDone || isTutorial,
            openClue = null,
            compSex = null, compHobby = null, compHair = null, compFeature = null, compVehicle = null,
            warrantFor = null, computed = false, won = false, resultLines = emptyList(),
            selectedTool = -1, sightingLevel = 0, sleeping = false, careerOver = false,
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
        val places = pickVenues(st.currentCity)
        // each venue is staffed by one of its own witnesses (Harbor -> Sailor etc., like the original;
        // new expansion venues fall back to their placeholder occupation)
        val occs = places.map { p ->
            (GameData.venueOccupations[p] ?: Expansion.venueOccupations[p] ?: GameData.occupations).random()
        }
        val list = mutableListOf<Venue>()
        val onTrack = st.currentCity == st.route.getOrNull(st.progress) && st.onTrack

        if (!onTrack) {
            places.forEachIndexed { i, p ->
                // wrong city: each venue answers with its own DOS no-information line
                val line = GameData.noInformationByVenue[p] ?: Expansion.noInformationByVenue[p]
                    ?: GameData.noInformation.random()
                list.add(Venue(p, occs[i], ClueKind.NONE, line))
            }
        } else if (st.currentCity == st.hideout) {
            // Hideout city: every venue shows the special line until the crook is found
            // (which venue that is gets decided in openVenue — never the first pick,
            // 50/50 on the second, certain on the third, per the 1990 release's rules)
            places.forEachIndexed { i, p ->
                list.add(Venue(p, occs[i], ClueKind.DANGER,
                    "Word on the street says the gang is hiding somewhere in town."))
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
                    // Non-destination venue. Reveal the next unseen trait; once the whole
                    // description is known, cycle back through the traits so a witness re-confirms
                    // one (traitClue re-phrases it each time) instead of parroting the same flavour
                    // filler. C3: an occasional nemesis whisper adds colour (never a real clue).
                    val n = st.revealOrder.size
                    val slotIdx = st.revealedCount + list.count { it.kind == ClueKind.TRAIT }
                    when {
                        shouldTeaseNemesis(st) ->
                            list.add(Venue(p, occ, ClueKind.DANGER, nemesisTease()))
                        n > 0 -> {
                            val tr = st.revealOrder[slotIdx % n]
                            list.add(Venue(p, occ, ClueKind.TRAIT, traitClue(tr), tr))
                        }
                        else ->
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
        // forward-looking fact you look up. Expansion cities carry hand-authored, reviewed leads
        // (one per angle); the original 30 fall back to the region+landmark templates.
        val frag = if (info.clues.isNotEmpty()) pronouns("{s} " + info.clues.random())
            else pronouns(templatedClueFragment(info))
        return "$lead $frag."
    }

    /** Build a clue fragment from a city's structured attributes — a template per clue type
     *  (region · landmark · flag · currency · language), matching the 1990 game's clue mix.
     *  region + landmark are always available; flag/currency/greeting fire when the almanac has them. */
    private fun templatedClueFragment(info: CityInfo): String {
        val options = mutableListOf(
            "{s} was headed for a country in ${info.region}",
            "{s} planned to visit a place known for ${info.landmark}",
            "{s} was headed somewhere in ${info.region}, near ${info.landmark}",
        )
        info.flag?.let { options += "{s} sketched a flag with $it" }
        info.currency?.let { options += "{s} counted money called $it" }
        // the greeting is a full "In X, hello is …(pron)." line; the clue only wants the sound,
        // so pull the parenthetical pronunciation out of it.
        info.greeting?.let { g ->
            val sound = Regex("\\(([^)]+)\\)").find(g)?.groupValues?.get(1) ?: g
            options += "{s} practiced a hello that sounded like “$sound”"
        }
        return options.random()
    }

    /** Cities in play: the free original 30, plus the paid destinations of every recognition wave
     *  unlocked at the current rank (International grades 5..14 reveal waves 0..9, famous first). */
    private fun activeCities(): List<String> {
        if (!s.expansionUnlocked) return GameData.cities
        val extra = Progression.citiesUpToWave(Progression.unlockedMaxWave(s.rankIndex))
        return GameData.cities + extra
    }

    /** Venues in play: base 12, plus the expansion's once unlocked. */
    private fun activeVenues(): List<String> =
        if (s.expansionUnlocked) GameData.venues + Expansion.venues else GameData.venues

    /** Three distinct venues for a city. When unlocked and the city has an affinity, its
     *  characteristic venues (Las Vegas -> the casino) are weighted up but the picks stay distinct. */
    private fun pickVenues(city: String): List<String> {
        val pool = activeVenues()
        val affinity = if (s.expansionUnlocked) Expansion.cityVenueAffinity[city].orEmpty() else emptyList()
        if (affinity.isEmpty()) return pool.shuffled().take(3)
        val bag = (affinity + pool).toMutableList()   // affinity multiplicity = extra weight
        val picks = LinkedHashSet<String>()
        while (picks.size < 3 && bag.isNotEmpty()) picks.add(bag.removeAt(Random.nextInt(bag.size)))
        pool.shuffled().forEach { if (picks.size < 3) picks.add(it) }   // top up tiny pools
        return picks.toList()
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

    // C3: the finale is always Clara San Diego, but nothing foreshadows her. Seed her name as
    // the shadowy boss behind ordinary cases so the ~14-case arc has a villain to build toward.
    private fun shouldTeaseNemesis(st: GameState): Boolean =
        st.culprit?.name != "Clara San Diego" && st.casesSolved >= 1 && Random.nextInt(4) == 0

    private fun nemesisTease(): String = listOf(
        "The witness drops their voice: word is a woman named Clara San Diego runs the whole operation.",
        "\"These capers all trace back to one boss,\" the witness whispers — \"a Clara San Diego.\"",
        "Someone mutters that the real mastermind, a Clara San Diego, is still out there and untouchable.",
    ).random()

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
        val firstVisit = index !in st.visited
        if (firstVisit && v.kind == ClueKind.TRAIT)
            st = st.copy(revealedCount = st.revealedCount + 1)
        // log leads and traits to the case journal the first time each venue is opened
        if (firstVisit && (v.kind == ClueKind.DESTINATION || v.kind == ClueKind.TRAIT))
            st = st.copy(journal = st.journal + JournalEntry(v.kind, v.text, st.currentCity))
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
        advanceTut(1)
        checkDeadline()
        autosave()
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
        val want = Random.nextInt(2, 5)               // 2-4 connections, like the original
        // Keep the fly-to markers legible on the world map: greedily choose decoys that stay a
        // minimum distance from the origin and from every option already picked, so two close
        // cities (e.g. New Delhi & Kathmandu) never stack their dots/labels on top of each other.
        val chosen = mutableListOf<String>()
        next?.let { chosen.add(it) }
        val pool = activeCities().filter { it != next && it != s.currentCity && it !in s.route }.shuffled()
        for (c in pool) {
            if (chosen.size >= want) break
            if (mapTooClose(s.currentCity, c) || chosen.any { mapTooClose(it, c) }) continue
            chosen.add(c)
        }
        // Dense region (couldn't find enough spaced cities): pad with any remaining so the player
        // still gets a full set of choices, even if a pair ends up a little close.
        if (chosen.size < want) for (c in pool) { if (chosen.size >= want) break; if (c !in chosen) chosen.add(c) }
        return chosen.shuffled()
    }

    /** True if two cities sit so close on the world map that their dots/labels would overlap.
     *  Unpositioned places (the new-country expansion) draw no dot, so they're never "too close". */
    private fun mapTooClose(a: String, b: String): Boolean {
        val pa = WorldMap.of(a) ?: return false
        val pb = WorldMap.of(b) ?: return false
        val d = kotlin.math.hypot(((pa.x - pb.x) * WorldMap.WV).toDouble(), ((pa.y - pb.y) * WorldMap.HV).toDouble())
        return d < 24.0
    }

    /** Flight time from the current city, scaled by map distance (the original's travel
     *  times depend on how far apart the cities are; short hops ~2-3 h). Deterministic, so
     *  the DEPART preview shows exactly what the flight will cost. */
    fun flightHoursTo(city: String): Int = flightCost(s.currentCity, city)

    /** Flight hours between any two places (distance-scaled 2..14h; 4h when either is unpositioned,
     *  e.g. the new-country expansion). Basis for a case's travel need and its deadline. */
    private fun flightCost(from: String, to: String): Int {
        val a = WorldMap.of(from)
        val b = WorldMap.of(to)
        return if (a != null && b != null) {
            val d = kotlin.math.hypot(((a.x - b.x) * 2f).toDouble(), (a.y - b.y).toDouble())
            (2 + d * 6).toInt().coerceIn(2, 14)
        } else 4
    }

    /** Level rules: hold first-sightings to at most `cap` per case. Surplus never-seen cities are
     *  swapped for already-seen ones from the pool so most hops reinforce known geography. Early
     *  cases (nothing seen yet) are naturally all-new — nothing to swap in — and pass through. */
    private fun capNewPerCase(route: List<String>, seen: Set<String>, cap: Int, pool: List<String>): List<String> {
        val newIdx = route.indices.filter { route[it] !in seen }
        if (newIdx.size <= cap) return route
        val fill = pool.filter { it in seen && it !in route }.shuffled().toMutableList()
        val out = route.toMutableList()
        for (i in newIdx.drop(cap)) {
            if (fill.isEmpty()) break
            out[i] = fill.removeAt(0)
        }
        return out
    }

    /** Hours remaining before the Sunday 5 p.m. deadline. */
    fun hoursLeft(): Int = (s.caseDeadlineHours - s.clock).coerceAtLeast(0)

    /** Start the flight: the travel screen animates the red route line, then calls arrive(). */
    fun travelTo(city: String) {
        if (s.flying != null) return
        s = s.copy(flying = city, flightHours = flightHoursTo(city))
        cue(SoundCue.TRAVEL)
        advanceTut(3)
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
            else -> st.copy(currentCity = city, onTrack = false, wrongFlights = st.wrongFlights + 1)
        }
        // No tool is pre-selected on arrival: the green selection border appears only once the
        // player actually taps a tool, so it never reads as a persistent tutorial highlight.
        // Passport (C4): log every place we actually land in — right trail or wrong — so the
        // painted world map fills in from day one (revealed only once the expansion is bought).
        // L4: stamp its last-seen case index for spaced-repetition scheduling.
        s = st.copy(selectedTool = -1, visitedPlaces = st.visitedPlaces + city,
            cityLastSeen = st.cityLastSeen + (city to st.casesSolved))
        if (s.deadlinePassed) { escaped("time"); return }
        s = s.copy(phase = Phase.CITY)
        buildVenues()
        s = s.copy(departOptions = makeDepartOptions())
        cue(SoundCue.ARRIVE)
        autosave()
    }

    // ---------- crime computer ----------
    fun setComp(cat: String, value: String?) {
        s = when (cat) {
            "sex" -> s.copy(compSex = value); "hobby" -> s.copy(compHobby = value)
            "hair" -> s.copy(compHair = value); "feature" -> s.copy(compFeature = value)
            else -> s.copy(compVehicle = value)
        }.copy(computed = false)
        advanceTut(4)
        autosave()   // a computer entry is case state — persist so a reload can't rewind it
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
        if (issuedWarrant) advanceTut(5)
        checkDeadline()
        autosave()
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
                    "This is Interpol.",
                    "You have finally cornered ${c.name}.",
                    GameData.NO_WARRANT.let { "But with no warrant in hand, no lawful arrest can be made!" },
                    "The gang has pulled off another caper and vanished!",
                ))
            }
            w.name != c.name -> {
                s = s.copy(won = false, resultLines = listOf(
                    "Your trail has led you to ${s.currentCity}.",
                    "Sadly, the warrant you hold names ${w.name}.",
                    "Watch out - a wrongful arrest could land us all in court!",
                    "Better luck on your next assignment.",
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
        if (c.name == "Clara San Diego") {
            lines += "You've captured the ring-leader herself - Clara San Diego is behind bars for good!"
            lines += "Congratulations - you've earned a place in the Interpol Hall of Fame!"
        } else {
            // faithful phrasing: the CRIME city's police make the arrest and get the loot back
            lines += "With your help, police in $crimeCity have taken ${c.name} into custody."
            lines += "${c.name} was carrying the stolen ${s.treasure}, now on its way home to the thankful people of $crimeCity."
        }
        // R1 peak-end: sign off warm and personal, not on a cold Interpol form line.
        lines += if (c.name == "Clara San Diego")
            "Take a bow, ${s.detectiveName}. You began as a rookie — and you brought in the one who slipped past everyone else."
        else
            "Get some rest, ${s.detectiveName}. Thanks to you, $crimeCity sleeps easier tonight."
        // H4 streak: fold today's solve into the case-a-day streak (a weekly freeze absorbs
        // one missed day). clock() is 0 in tests, which harmlessly keeps the streak at 1.
        val today = (clock() / 86_400_000L).toInt()
        var streak = s.streakDays
        var freezes = s.streakFreezes
        when {
            s.lastSolveEpochDay == 0 -> streak = 1
            today - s.lastSolveEpochDay <= 0 -> if (streak == 0) streak = 1  // another win same day
            today - s.lastSolveEpochDay == 1 -> streak += 1                  // consecutive day
            today - s.lastSolveEpochDay == 2 && freezes > 0 -> { freezes -= 1; streak += 1 }
            else -> streak = 1                                              // streak broke
        }
        if (streak > 0 && streak % 7 == 0 && freezes < 1) freezes = 1        // earn a weekly freeze
        if (streak >= 2) lines += "🔥 $streak-day case streak!"
        val newCases = s.casesSolved + 1
        val paid = s.expansionUnlocked
        // Jailing Clara ends the free career (retire to the Hall of Fame). If the paid International
        // track is unlocked, catching her instead promotes you into it — the world still needs you.
        val careerOver = c.name == "Clara San Diego" && !paid
        // Free promotion cadence (1990 rules): cases 1, 5, 9, 13 -> Sleuth..Ace. International grades
        // then promote once every 8 cases past the free arc (cases 14, 22, 30, ...).
        val freeThreshold = newCases in setOf(1, 5, 9, 13)
        val intlThreshold = paid && newCases >= GameState.CAREER_CASES &&
            (newCases - GameState.CAREER_CASES) % 8 == 0
        val promote = !careerOver && s.rankIndex < GameData.ranks.lastIndex && (freeThreshold || intlThreshold)
        if (promote) {
            lines += "Well done, ${s.detectiveName} - a promotion is yours."
            lines += "One last puzzle stands between you and the promotion."
        }
        // update the career record: capture the villain, tally clean / hint-free solves,
        // then unlock any newly-earned commendations from the resulting record
        val captured = s.capturedVillains + c.name
        val cleanSweep = s.hadCleanCase || s.wrongFlights == 0
        val hintFree = if (s.hintsUsed == 0) s.hintFreeSolves + 1 else s.hintFreeSolves
        var next = s.copy(
            phase = Phase.RESULT, won = true, casesSolved = newCases,
            resultLines = lines, pendingPromotion = promote, careerOver = careerOver,
            capturedVillains = captured, hadCleanCase = cleanSweep, hintFreeSolves = hintFree,
            streakDays = streak, streakFreezes = freezes, lastSolveEpochDay = today,
        )
        next = next.copy(
            unlockedAchievements = next.unlockedAchievements +
                Achievements.earned(Achievements.summarise(next)),
            tutorialDone = true, tutorialStep = -1,
        )
        s = next
        autosave()
    }

    /** The promotion quiz was answered: bump the rank only when correct (like the original). */
    fun resolvePromotion(correct: Boolean) {
        val newRank = if (correct && s.rankIndex < GameData.ranks.lastIndex) s.rankIndex + 1 else s.rankIndex
        s = s.copy(rankIndex = newRank, pendingPromotion = false)
        autosave()
    }

    /** Cases remaining until the next promotion threshold (1, 5, 9, 13). */
    fun casesToNextPromotion(): Int {
        val thresholds = if (s.expansionUnlocked)
            listOf(1, 5, 9, 13) + (GameState.CAREER_CASES..200 step 8) else listOf(1, 5, 9, 13)
        val next = thresholds.firstOrNull { it > s.casesSolved } ?: return 0
        return next - s.casesSolved
    }

    private fun escaped(reason: String) {
        val c = s.culprit!!
        // R2 near-miss: only when the loss was genuinely close — on the right trail and at (or
        // one hop from) the hideout when the clock ran out. A wrong-warrant bust isn't a
        // near-miss; that loss is handled in confront() and stays instructive.
        val nearMiss = reason == "time" && s.onTrack &&
            (s.atHideout || s.progress >= s.route.size - 1)
        val lines = when {
            nearMiss -> listOf(
                "So close.",
                "You reached ${s.hideout} just as ${c.name} slipped out the back —",
                "minutes too late. The trail was right; the clock beat you.",
            )
            reason == "time" -> listOf(
                "Incoming from Interpol:", "Unwelcome news...",
                "Word just came in: ${c.name} escaped because the investigation ran out of time!",
            )
            else -> listOf("The suspect has escaped!")
        }
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
        val left = (s.caseDeadlineHours - s.clock - offsetHours).coerceAtLeast(0)
        return if (left >= 24) "${left / 24}d ${left % 24}h left" else "${left}h left"
    }
}
