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
import com.acme.clara.data.Masterminds
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
    /** Options > Language — pick the interface language. */
    data object Language : Overlay
    /** The World Campaign purchase dialog. [source] is an analytics tag only (which CTA opened
     *  it — menu bar, Passport, Database, Case 14...), not behavior-affecting. */
    data class PurchaseOffer(val source: String) : Overlay
    /** Shown exactly once, right after [ClaraViewModel.unlockExpansion] first grants entitlement
     *  — confirms the purchase before the receipt-only silence sets in. Never reappears: the
     *  method it's opened from is itself idempotent (guarded by `expansionUnlocked`). */
    data object UnlockCeremony : Overlay
    /** Paid, answer-safe comparison of the current departure choices. */
    data object CasePlanner : Overlay
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
        "the Hope Diamond", "a solid-gold Buddha statue", "an emerald the size of a fist",
        "the world's largest pearl", "a suit of gilded samurai armor", "an Egyptian sarcophagus",
        "a jade burial mask", "the last unicorn tapestry", "a meteorite fragment from outer space",
        "the blueprint of a legendary lost city", "a Viking longship's golden prow",
        "an ivory chess set carved for royalty", "the world's oldest surviving map",
        "a diamond-studded pocket watch", "the ashes of a mythical phoenix",
        "a solid-platinum championship trophy", "the world's rarest postage stamp",
        "a pirate's buried treasure chest", "a jewel-encrusted ceremonial sword",
        "the skeleton of a prehistoric dinosaur", "a silk tapestry woven with real gold thread",
        "the world's largest uncut sapphire", "a first-edition manuscript by a legendary author",
        "a solid-gold llama figurine", "the world's most valuable violin bow",
        "an antique globe painted with forgotten kingdoms", "the crown of a lost kingdom",
        "a bronze statue said to grant wishes", "the world's largest cut ruby",
        "a stuffed specimen of an extinct bird", "the original score of a legendary symphony",
        "a solid-silver ceremonial mask", "the world's finest hand-woven rug",
        "a fossilized dinosaur egg", "a trident said to belong to a sea god",
        "a jewel-encrusted royal orb",
    )

    /** The saved/state value stays the English string; localize only at render time. */
    fun localized(treasure: String): String {
        val i = list.indexOf(treasure)
        return if (i < 0) treasure else com.acme.clara.i18n.Strings.opt("treasure.$i") ?: treasure
    }
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
    // paid-tier Bureau tip: one concrete lead per case (see requestHint()), reset every newCase
    val bureauTipUsed: Boolean = false,
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
    // paid-tier entitlement: unlocks the 201 campaign destinations wave by wave, plus the paid
    // world tools and comforts. Free play stays on the original 30.
    val expansionUnlocked: Boolean = false,
    // Optional paid comfort perk. It adds eight hours to a freshly generated case deadline and
    // can be disabled from Options without affecting campaign progression.
    val travelBufferEnabled: Boolean = true,
    val hintFreeSolves: Int = 0,
    val hadCleanCase: Boolean = false,
    // free hints banked from returning after time away (spend without losing the hint-free badge).
    // Capped at 1 in resume() — a paid player's per-case bureauTipUsed perk stacks on top, so the
    // combined ceiling in any one case is 2, never more.
    val freeHints: Int = 0,
    // The guided first case is a set of contextual, teach-once lessons rather than a linear step
    // counter: each lesson fires the first time its game state is true and clears when the player
    // does the action. [tutorialActive] = the tour is running now; [tutorialDone] = it has run once
    // (never re-arms); [tutorialSeen] = lesson ids already taught; the sawClue flags arm the lessons
    // that only make sense once you've actually heard that kind of witness.
    val tutorialDone: Boolean = false,
    val tutorialActive: Boolean = false,
    val tutorialSeen: Set<String> = emptySet(),
    val sawTraitClue: Boolean = false,
    val sawTrailClue: Boolean = false,
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
        // Career length: promotions at 1, 5, 9, 13 solved (4 cases per middle rank); the first
        // case as Ace Detective is always Clara San Diego herself, but she escapes there — the
        // free career's inciting incident, not its end (see Masterminds.kt / win()'s
        // isCase14Clara). The career only truly ends at the paid campaign's finale.
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
        mergeGlobalEntitlement()
    }

    /** Bind the repository without picking a profile yet (the launch flow chooses one). */
    fun bindRepository(repository: SaveRepository, now: () -> Long = { System.currentTimeMillis() }) {
        repo = repository; clock = now
        mergeGlobalEntitlement()
        // Binding normally happens before the intro can be advanced, but preserve correctness on
        // a very slow cold start too: a player may already have reached or confirmed sign-on.
        when {
            profileId != null && s.detectiveName.isNotBlank() && s.route.isNotEmpty() ->
                repository.saveNewCareer(snapshot(profileId!!, clock()))
            s.phase == Phase.SIGN_ON && s.detectiveName.isBlank() ->
                repository.setPendingSignOn(true)
        }
    }

    private fun mergeGlobalEntitlement() {
        val r = repo ?: return
        // One-time migration for saves created before ownership became app-wide. Once Play has
        // explicitly reconciled ownership, the durable repository decision wins over stale saves
        // so a refunded career cannot grant itself again when loaded.
        if (!r.isExpansionOwnershipKnown() && s.expansionUnlocked) r.setExpansionOwned()
        s = s.copy(expansionUnlocked = r.ownsExpansion())
    }

    private fun entitlementFor(savedValue: Boolean): Boolean {
        val r = repo ?: return savedValue
        if (!r.isExpansionOwnershipKnown() && savedValue) r.setExpansionOwned()
        return r.ownsExpansion()
    }

    /** Block until any save queued so far has actually landed on disk. Call this from onStop(),
     *  not from any per-action path — autosave()'s whole point is that ordinary callers don't
     *  wait. A real backgrounding (home button, task switch) reaches onStop() before the OS could
     *  ever kill the process, so a bounded flush here — one small pending write, if any — closes
     *  the data-loss window an abrupt kill could otherwise catch mid-write, without reintroducing
     *  a stall on every action. */
    fun flushPendingSaves() {
        // Several navigation transitions are intentionally cheap in memory and do not write on
        // every tap. Leaving the foreground is the final persistence boundary: enqueue the exact
        // current state first, then wait until every older/newer queued snapshot is durable.
        autosave()
        (repo as? com.acme.clara.save.SaveStore)?.awaitPendingWrites()
    }

    private fun newProfileId(): String = "career-" + java.util.UUID.randomUUID().toString().take(8)

    /** Existing saved careers, newest first (empty when no repository is bound). */
    fun savedGames(): List<SaveMeta> = repo?.list().orEmpty()

    /** Read-only half of picker resume; the UI performs this on Dispatchers.IO and calls
     * [resume] back on the main thread. */
    fun savedGame(id: String): SaveData? = repo?.load(id)

    /** Resume a saved career (launch continue / picker). Returning after time away banks a free hint. */
    fun resume(data: SaveData) {
        profileId = data.meta.id
        // Loading an existing detective explicitly abandons any unconfirmed new-career draft.
        // Otherwise that stale marker would send the next cold launch back to the printer.
        repo?.setPendingSignOn(false)
        var st = data.state.copy(expansionUnlocked = entitlementFor(data.state.expansionUnlocked))
        val backAfterGap = WelcomeBack.grantsHint(data.meta.lastPlayed, clock())
        // H3: after a long absence, bank a free hint (capped at 1 — this is a "welcome back"
        // nudge, not a resource to stockpile across repeated unplayed gaps) and queue a kinder
        // warm-up next case.
        if (backAfterGap) st = st.copy(freeHints = minOf(1, st.freeHints + 1), warmUpNextCase = true)
        // P5 recap card: the game already logs the whole trail + traits (CaseJournal); draw it
        // on return so a case survives the break, folding in the welcome-back hint if any.
        val recap = CaseJournal.recap(st)
        val caseUnderway = st.phase == Phase.CITY && st.route.isNotEmpty() && st.progress > 0
        st = if (recap != null && caseUnderway) {
            val i18n = com.acme.clara.i18n.Strings
            val trail = st.route.take(st.progress + 1).joinToString(" ▸ ") { i18n.place(it) }
            val traits = st.revealedTraits.joinToString(", ") { i18n.label("tval", it.second) }
            val lines = buildList {
                add(recap)
                add("")
                add(i18n.ui("Trail: {0}", trail))
                if (traits.isNotBlank()) add(i18n.ui("Suspect so far: {0}", traits))
                if (backAfterGap) { add(""); add(i18n.ui("A fresh lead surfaced — here's a free hint to spend.")) }
            }
            st.copy(overlay = Overlay.Info(com.acme.clara.i18n.Strings.ui("PREVIOUSLY ON THIS CASE"), lines))
        } else if (backAfterGap) {
            st.copy(overlay = Overlay.Info(com.acme.clara.i18n.Strings.ui("WELCOME BACK"), listOf(
                com.acme.clara.i18n.Strings.ui("Been a while, {0}.", st.detectiveName),
                com.acme.clara.i18n.Strings.ui("A fresh lead has surfaced — here's a free hint to spend."),
            )))
        } else st
        s = st
        // Persist the welcome-back grant and refreshed lastPlayed immediately. Without this,
        // repeatedly reopening an old save could replay resume-only benefits and stale ordering.
        autosave()
    }

    /** Resume by id from the bound repository (used by the picker). */
    fun resumeById(id: String) { repo?.load(id)?.let { resume(it) } }

    /** Delete a saved career from the picker. */
    // Left synchronous on purpose: ChooseGameScreen bumps its `remember(refresh)` key in the same
    // click handler right after calling this, so the picker's re-read of the save list must see
    // the delete already applied — an async delete would let the just-deleted save flash back into
    // the list until the write actually lands. A single rare, deliberate tap; not autosave's
    // every-action frequency, so the ANR risk here was never the real one.
    fun deleteGame(id: String) { repo?.delete(id) }

    /** Snapshot the current state as a save (transient UI/animation fields cleared). */
    fun snapshot(id: String, at: Long): SaveData = SaveData(
        SaveMeta(id, s.detectiveName, s.rankIndex, s.casesSolved, at),
        s.copy(phase = when (s.phase) {
                // A confirmed sign-on owns a complete generated case. Resume at the briefing,
                // not at a fresh name prompt whose Compose-local printer stage no longer exists.
                Phase.SIGN_ON -> Phase.BRIEFING
                // An interrupted flight has not charged time or changed city yet.
                Phase.TRAVEL -> Phase.CITY
                // The confrontation already decided and stored the outcome before CHASE.
                Phase.CHASE -> Phase.RESULT
                else -> s.phase
            }, overlay = null, openClue = null, flying = null, flightHours = 0,
            sightingLevel = 0, sleeping = false),
    )

    /** Load a saved career into this ViewModel (launch continue / picker). */
    fun loadCareer(data: SaveData) {
        profileId = data.meta.id
        repo?.setPendingSignOn(false)
        s = data.state.copy(expansionUnlocked = entitlementFor(data.state.expansionUnlocked))
    }

    /** Persist the current state to the active profile — the state on disk always equals the
     *  screen. [SaveRepository.save] itself is responsible for not blocking the caller (see
     *  SaveStore's off-main-thread write) — kept a plain synchronous call here, not dispatched
     *  through a coroutine, so tests using the instant in-memory repository stay deterministic:
     *  a repo.load() right after an action must already see this write, with no coroutine
     *  scheduling in between to race against. */
    private fun autosave() {
        val r = repo ?: return
        val id = profileId ?: return
        r.save(snapshot(id, clock()))
    }

    fun toggleHaptics() { s = s.copy(hapticsOn = !s.hapticsOn, overlay = null); autosave() }
    fun toggleCaptions() { s = s.copy(captionsOn = !s.captionsOn, overlay = null); autosave() }
    fun toggleTravelBuffer() { s = s.copy(travelBufferEnabled = !s.travelBufferEnabled, overlay = null); autosave() }

    // ---------- tutorial ----------
    /** Advance the guided tutorial when the player performs the step's taught action. */
    /** Mark a tour lesson as taught/done, so its coach-mark won't show again. No-op once the tour
     *  is over or the lesson is already seen. */
    private fun teach(id: String) {
        if (s.tutorialActive && id !in s.tutorialSeen) s = s.copy(tutorialSeen = s.tutorialSeen + id)
    }
    /** Dismiss an informational tip (the ones with no single action to complete them). */
    fun dismissTip(id: String) { teach(id) }
    fun skipTutorial() { s = s.copy(tutorialActive = false, tutorialDone = true); autosave() }

    /** Bureau ▸ Hint. A welcome-back free hint (banked by [resume]) always honors its promise
     *  first, paid or not — badge-safe. Otherwise, while sales are live, Hint is a paid-tier perk
     *  (advertised on the purchase card as "a hint"): an unpaid tap offers the purchase instead of
     *  any hint text, and a paid career gets exactly one concrete Bureau tip per case, also
     *  badge-safe, so an absent paid player can stack up to 2 free hints in a case (the banked one
     *  plus this case's tip) — a second ask past that is told there's nothing left to give, rather
     *  than repeating or vaguing out the same lead. While sales are disabled via
     *  [com.acme.clara.billing.BillingManager.SALES_ENABLED], Hint stays exactly as it
     *  always was: unlimited, and non-banked hints still cost the hint-free badge — nothing here
     *  should change what today's players already have. */
    fun requestHint() {
        val i18n = com.acme.clara.i18n.Strings
        if (s.freeHints > 0) {
            val hint = hintText()
            s = s.copy(freeHints = s.freeHints - 1, overlay = Overlay.Info(i18n.ui("HINT"),
                listOf(hint, "", i18n.ui("(free hint — your hint-free record is safe)"))))
            autosave()
            return
        }
        if (!com.acme.clara.billing.BillingManager.SALES_ENABLED) {
            val hint = hintText()
            s = s.copy(hintsUsed = s.hintsUsed + 1, overlay = Overlay.Info(i18n.ui("HINT"),
                listOf(hint, "", i18n.ui("(this case is no longer a hint-free solve)"))))
            autosave()
            return
        }
        if (!s.expansionUnlocked) { s = s.copy(overlay = Overlay.PurchaseOffer("Bureau hint")); return }
        if (s.bureauTipUsed) {
            s = s.copy(overlay = Overlay.Info(i18n.ui("HINT"),
                listOf(i18n.ui("The Bureau has no additional tips for this case."))))
            return
        }
        s = s.copy(bureauTipUsed = true, overlay = Overlay.Info(i18n.ui("HINT"),
            listOf(hintText(), "", i18n.ui("(free hint — your hint-free record is safe)"))))
        autosave()
    }

    /** A tiered hint: a computer nudge, a directional lead, or an arrest prompt. The directional
     *  lead names the region the trail points to (never the exact city — the player still has to
     *  find it), styled as a tip radioed in from the Bureau. */
    private fun hintText(): String {
        val next = s.route.getOrNull(s.progress + 1)
        val i18n = com.acme.clara.i18n.Strings
        return when {
            s.atHideout && s.warrantFor?.name == s.culprit?.name ->
                i18n.ui("You've cornered the thief — search the venues here to make the arrest.")
            s.atHideout ->
                i18n.ui("This is the hideout. Make sure your warrant names the right suspect before closing in.")
            s.warrantFor == null && matches().size == 1 && anyFilterSet() ->
                i18n.ui("You have enough of the description — run the crime computer to issue the warrant.")
            s.warrantFor == null ->
                i18n.ui("Question more witnesses; the crime computer still lists several suspects.")
            next != null ->
                i18n.ui("The Bureau received word — the suspect is flying toward {0}.",
                    CityMeta.of(next).region.let { i18n.label("region.name", it) })
            else ->
                i18n.ui("Follow your last lead to the thief's hideout.")
        }
    }

    // ---------- flow ----------
    fun introDone() { if (s.phase == Phase.INTRO) s = s.copy(phase = Phase.TITLE) }
    fun start() {
        repo?.setPendingSignOn(true)
        s = s.copy(phase = Phase.SIGN_ON)
    }

    fun signOn(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        if (profileId == null) profileId = newProfileId()
        s = GameState(phase = Phase.BRIEFING, detectiveName = nm,
            expansionUnlocked = repo?.ownsExpansion() == true || s.expansionUnlocked)
        newCase()
        repo?.let { r -> profileId?.let { r.saveNewCareer(snapshot(it, clock())) } }
    }

    /**
     * Sign-on used by the HQ printer teletype: register the detective name and generate the
     * first case, but stay on the SIGN_ON screen so the same printer keeps printing the
     * briefing (faithful to the original — the computer never switches to a different UI).
     */
    fun signOnStart(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        if (profileId == null) profileId = newProfileId()
        s = GameState(phase = Phase.SIGN_ON, detectiveName = nm,
            expansionUnlocked = repo?.ownsExpansion() == true || s.expansionUnlocked)
        newCase()                          // newCase() flips phase to BRIEFING...
        s = s.copy(phase = Phase.SIGN_ON)  // ...keep the printer on-screen until "begin"
        repo?.let { r -> profileId?.let { r.saveNewCareer(snapshot(it, clock())) } }
    }

    fun beginInvestigation() { s = s.copy(phase = Phase.CITY); autosave() }

    // ---------- navigation ----------
    fun gotoCity() { s = s.copy(phase = Phase.CITY) }
    fun gotoTravel() {
        val wrongCity = !s.onTrack
        s = s.copy(phase = Phase.TRAVEL, selectedTool = 1)
        teach("interview"); teach("time"); teach("warrant")
        // "trail" is taught on the actual flight (travelTo), so its coach can guide the map pick.
        if (wrongCity) teach("wrongflight")   // they took the hint and are flying back
    }
    // The player fills in the suspect's description themselves — the computer is not pre-populated.
    fun gotoCrime() { s = s.copy(phase = Phase.CRIME, selectedTool = 3); cue(SoundCue.FLASH); teach("time") }
    fun selectTool(i: Int) { s = s.copy(selectedTool = i) }

    // ---------- menu bar ----------
    fun openOverlay(o: Overlay) {
        if (o == Overlay.Almanac) teach("database")
        s = s.copy(overlay = o)
    }
    fun dismissOverlay() { s = s.copy(overlay = null) }
    fun menuNewCase() {
        // The printer's unconfirmed identity is not a career yet. Letting its menu create a case
        // produces a nameless, unsaved game and bypasses the whole sign-on contract.
        if (s.phase == Phase.SIGN_ON) { s = s.copy(overlay = null); return }
        // A case cannot replace the result that owns an unanswered patent quiz. In particular,
        // skipping a wave quiz would leave rank-based wave gating permanently out of sync.
        if (s.pendingPromotion || s.careerOver) { s = s.copy(overlay = null); return }
        if (s.casesSolved >= GameState.CAREER_CASES && !s.expansionUnlocked) {
            val selling = com.acme.clara.billing.BillingManager.SALES_ENABLED
            if (!selling) profileId = null
            s = s.copy(
                overlay = if (selling) Overlay.PurchaseOffer("New case") else null,
                phase = if (selling) s.phase else Phase.TITLE,
            )
            return
        }
        s = s.copy(overlay = null, phase = Phase.BRIEFING); newCase(); cue(SoundCue.BRIEFING); autosave()
    }

    /** Grant the paid-tier entitlement: Wave 1 and the comfort perks are immediate; later waves
     *  enter the pool as their preceding finale is cleared. Called after a successful
     *  purchase or a restore. Idempotent — safe to call repeatedly (e.g. on every app start once
     *  BillingClient reconnects and reports the existing purchase). */
    fun unlockExpansion() {
        repo?.setExpansionOwned()
        if (s.expansionUnlocked) return
        s = s.copy(expansionUnlocked = true, overlay = Overlay.UnlockCeremony)
        autosave()
    }

    /** Apply a successful Play ownership query. Connection/query failures never call this, so an
     *  offline buyer stays unlocked; an explicit successful "not owned" result revokes a refund
     *  app-wide and overrides historical per-career flags on every later load. */
    fun reconcileExpansionOwnership(owned: Boolean) {
        if (owned) {
            unlockExpansion()
            return
        }
        repo?.clearExpansionOwned()
        if (!s.expansionUnlocked) return
        s = s.copy(expansionUnlocked = false)
        autosave()
    }

    fun menuQuitToTitle() {
        flushPendingSaves()
        repo?.setPendingSignOn(false)
        profileId = null
        s = GameState(phase = Phase.TITLE,
            expansionUnlocked = repo?.ownsExpansion() == true || s.expansionUnlocked)
    }
    /** Game ▸ New Game — start a fresh career (a new saved profile). Names it on the sign-on screen. */
    fun newGameFlow() {
        flushPendingSaves() // protect the career being left before detaching from its profile
        profileId = null
        repo?.setPendingSignOn(true)
        s = GameState(phase = Phase.SIGN_ON,
            expansionUnlocked = repo?.ownsExpansion() == true || s.expansionUnlocked)
    }

    /** Restore only the unsaved sign-on screen; never manufacture a nameless picker entry. */
    fun restorePendingSignOn() {
        profileId = null
        s = GameState(phase = Phase.SIGN_ON,
            expansionUnlocked = repo?.ownsExpansion() == true || s.expansionUnlocked)
    }
    /** Show the saved-career picker. Snapshot the active career before detaching it: onStop()
     * must not subsequently persist CHOOSE_GAME into that profile while the picker is open. */
    fun toChooseGame() {
        // Wait here rather than merely queueing autosave(): the player can immediately re-select
        // this same file, and that read must not race the write and restore an older snapshot.
        flushPendingSaves()
        profileId = null
        s = s.copy(overlay = null, phase = Phase.CHOOSE_GAME)
    }
    // Options > Sound is a silent checkmark toggle in the original (the √ beside the item
    // reflects the state); the actual mute is applied by the audio engine in the UI layer.
    fun toggleSound() { s = s.copy(soundOn = !s.soundOn, overlay = null); autosave() }
    /** Debug-only: flip the paid entitlement both ways to test free vs. paid behavior
     *  (the real purchase path only ever grants it — see [unlockExpansion]). */
    fun devTogglePaid() { s = s.copy(expansionUnlocked = !s.expansionUnlocked); autosave() }

    // ---------- case generation ----------
    private fun newCase() {
        val carmen = GameData.suspects.first { it.name == "Clara San Diego" }
        // Campaign masterminds only enter a case at their authored finale. Keeping them out of
        // ordinary random cases prevents a boss appearing in Most Wanted as captured years before
        // the story raid that is meant to bring them in.
        val mastermindNames = Masterminds.arcs.mapTo(hashSetOf()) { it.suspectName }
        val pool = GameData.suspects.filter { it.name != "Clara San Diego" && it.name !in mastermindNames }
        val allNonClara = GameData.suspects.filter { it.name != "Clara San Diego" }
        val nextCases = s.casesSolved + 1
        val campaignCase = nextCases - GameState.CAREER_CASES
        val arc = if (s.expansionUnlocked && campaignCase > 0)
            Masterminds.arcForCampaignCase(campaignCase) else null
        // Clara is forced for the free-career inciting incident. Campaign finales force their
        // boss/successor, including Wave 10 where Clara is captured in the same raid.
        val culprit = when {
            s.casesSolved == GameState.CAREER_CASES - 1 -> carmen
            arc != null -> allNonClara.firstOrNull { it.name == arc.suspectName } ?: pool.random()
            else -> pool.random()
        }

        val order = discriminatingOrder(culprit)
        // Route length per rank from the level rules (free 5..9, International 9..12); the H3
        // welcome-back warm-up trims one hop.
        val warm = s.warmUpNextCase
        val routeLen = (Progression.hops(s.rankIndex) - if (warm) 1 else 0).coerceAtLeast(4)

        // L4: pick the route on a spaced-repetition curve so geography recurs for review, then cap
        // brand-new (never-seen) countries to this rank's allowance so the world stays learnable.
        // A mastermind case restricts the whole route to that arc's home wave — "every destination
        // belongs to the mastermind's region" — falling back to the normal pool if a wave is ever
        // too small to fill a route (the smallest, Oceania marquee, has 5 cities; routes run 9-12
        // hops, so this deliberately allows the spaced-repetition picker to revisit within it).
        val fullPool = activeCities()
        val cityPool = arc?.waveIndex?.let { wave ->
            fullPool.filter { Progression.wave[it] == wave }.takeIf { it.isNotEmpty() }
        } ?: fullPool
        val picked = SpacedRepetition.pickRoute(cityPool, s.cityLastSeen, s.casesSolved, routeLen)
        // A mastermind finale is exempt from the new-per-case cap: it's a deliberate deep dive
        // into one region, and forcing that AND capping fresh introductions is unsatisfiable the
        // first time a player reaches a wave (there's no already-seen pool within it yet to pad
        // the route with instead). Ordinary cases keep the normal fairness cap untouched.
        val cities = if (arc != null) picked
            else capNewPerCase(picked, s.cityLastSeen.keys, Progression.newPerCase(s.rankIndex), cityPool)
        // Deadline = a simulation of an efficient run's clock (a couple of witness opens per city +
        // the real flights, with the same overnight rolls) plus this rank's slack. Simulating rather
        // than approximating means slack stays a true margin whatever the flight lengths — the linear
        // formula was tuned on the free career's short hops and left the long-flight paid grades
        // unwinnable.
        val caseDeadline = estimateEfficientClock(cities) + Progression.slackHours(s.rankIndex) +
            if (s.expansionUnlocked && s.travelBufferEnabled) 8 else 0
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
            wrongFlights = 0, hintsUsed = 0, bureauTipUsed = false, journal = emptyList(),
            tutorialActive = isTutorial, tutorialDone = s.tutorialDone || isTutorial,
            tutorialSeen = emptySet(), sawTraitClue = false, sawTrailClue = false,
            openClue = null,
            compSex = null, compHobby = null, compHair = null, compFeature = null, compVehicle = null,
            warrantFor = null, computed = false, won = false, resultLines = emptyList(),
            selectedTool = -1, sightingLevel = 0, sleeping = false, careerOver = false,
        )
        buildVenues()
        s = s.copy(departOptions = makeDepartOptions())
    }

    /** Order culprit traits so a prefix uniquely identifies them among the whole roster. */
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
                // wrong city: each venue answers with its own DOS no-information line,
                // localized by venue key (the random fallback has no venue to key on)
                val en = GameData.noInformationByVenue[p] ?: Expansion.noInformationByVenue[p]
                val line = if (en != null) com.acme.clara.i18n.Strings.opt("noinfo.$p") ?: en
                           else GameData.noInformation.random()
                list.add(Venue(p, occs[i], ClueKind.NONE, line))
            }
        } else if (st.currentCity == st.hideout) {
            // Hideout city: every venue shows the special line until the crook is found
            // (which venue that is gets decided in openVenue — never the first pick,
            // 50/50 on the second, certain on the third, per the 1990 release's rules)
            places.forEachIndexed { i, p ->
                list.add(Venue(p, occs[i], ClueKind.DANGER,
                    com.acme.clara.i18n.Strings.ui("Word on the street says the gang is hiding somewhere in town.")))
            }
        } else {
            // On-track city (the 3-venue spec):
            //  · Venue 1  → a general trail hint (where the thief headed).
            //  · Venue 2  → a suspect trait until the warrant is issued, then a 2nd distinct trail hint.
            //  · Venue 3  → the "bet": a rank-scaled chance to show the destination's flag/currency
            //               (65/35), otherwise a funny witness aside (no clue). Free career: always pays.
            // No hint repeats within the city (assign V1 → V3 → V2); a real clue sometimes gets a witty
            // flourish (~30%).
            val info = st.route.getOrNull(st.progress + 1)?.let { CityMeta.of(it) }
            val paid = st.expansionUnlocked
            val hasMandate = st.warrantFor != null
            val used = HashSet<String>()
            val generals = ArrayDeque((info?.let { generalCluePool(it) } ?: emptyList()).shuffled())

            // Localized lead-ins + template phrases (English is a no-op via Strings.opt).
            val leadIns = GameData.clueLeadIns.indices.map {
                com.acme.clara.i18n.Strings.opt("clue.leadin.$it") ?: GameData.clueLeadIns[it]
            }
            fun tmpl(key: String, en: String) = com.acme.clara.i18n.Strings.opt(key) ?: en
            fun lead(frag: String) = pronouns("${leadIns.random()} {s} $frag.")
            fun flagText() = pronouns("${leadIns.random()} {s} ${tmpl("clue.tmpl.flag", "sketched a flag —")} ${info!!.flag}.")
            // Currency is stored with its article ("the euro") for the almanac; drop it here so the
            // clue reads "money called euro" rather than the awkward "money called the euro".
            fun currencyText() = pronouns("${leadIns.random()} {s} ${tmpl("clue.tmpl.currency", "counted money called")} ${info!!.currency!!.removePrefix("the ")}.")
            // Whiff (venue 3's no-clue outcome): a standalone aside from THIS witness's own
            // occupation (so it fits the teller), or a nemesis whisper.
            fun funnyText(occ: String) =
                (if (shouldTeaseNemesis(st)) nemesisTease() else Humor.witnessLine(occ, paid))
                    ?: "${flavourFood(st.culprit!!)}."
            // Flourish: tack a line that's ABOUT the suspect onto the clue — drawn from THIS witness's
            // occupation and pronoun-matched, so it reads as the same witness carrying on — or,
            // occasionally, a nemesis whisper.
            fun flourish(t: String, occ: String): String {
                if (Random.nextInt(100) >= 30) return t
                val aside = if (shouldTeaseNemesis(st)) nemesisTease()
                            else Humor.suspectAside(occ, paid)?.let { suspectPronouns(it) }
                return aside?.let { "$t $it" } ?: t
            }
            fun nextGeneral(): String? { val g = generals.removeFirstOrNull() ?: return null; used += "g:$g"; return g }
            fun flagFree() = info?.flag != null && "flag" !in used
            fun curFree() = info?.currency != null && "cur" !in used

            fun trailVenue(p: String, occ: String): Venue {
                val g = nextGeneral()
                return when {
                    g != null -> Venue(p, occ, ClueKind.DESTINATION, flourish(lead(g), occ))
                    flagFree() -> { used += "flag"; Venue(p, occ, ClueKind.DESTINATION, flourish(flagText(), occ)) }
                    curFree() -> { used += "cur"; Venue(p, occ, ClueKind.DESTINATION, flourish(currencyText(), occ)) }
                    else -> Venue(p, occ, ClueKind.DANGER, funnyText(occ))
                }
            }

            // Venue 1 — a general trail hint.
            val v1 = trailVenue(places[0], occs[0])

            // Venue 3 — the bet.
            val v3 = if (Random.nextDouble() < venue3Chance(st.rankIndex)) {
                val useFlag = when {
                    flagFree() && curFree() -> Random.nextInt(100) < 65
                    flagFree() -> true
                    curFree() -> false
                    else -> null
                }
                when (useFlag) {
                    true -> { used += "flag"; Venue(places[2], occs[2], ClueKind.DESTINATION, flourish(flagText(), occs[2])) }
                    false -> { used += "cur"; Venue(places[2], occs[2], ClueKind.DESTINATION, flourish(currencyText(), occs[2])) }
                    null -> Venue(places[2], occs[2], ClueKind.DANGER, funnyText(occs[2]))
                }
            } else Venue(places[2], occs[2], ClueKind.DANGER, funnyText(occs[2]))

            // Venue 2 — trait until the warrant is in hand, then a 2nd distinct trail hint.
            val v2 = if (!hasMandate && st.revealOrder.isNotEmpty()) {
                val tr = st.revealOrder[st.revealedCount % st.revealOrder.size]
                Venue(places[1], occs[1], ClueKind.TRAIT, flourish(traitClue(tr), occs[1]), tr)
            } else trailVenue(places[1], occs[1])

            list.add(v1); list.add(v2); list.add(v3)
        }
        // Shuffle so the trail clue isn't always the first building — otherwise a player learns to
        // check the same slot every time. Indices stay self-consistent for visited/openVenue.
        list.shuffle()
        s = s.copy(venues = list, visited = emptySet(), openClue = null)
    }

    /** The destination's general "where next" hints as subject-less fragments (never flag/currency —
     *  those are their own venues). Expansion cities carry hand-authored leads; the original 30 have
     *  a single landmark fragment, so their 2nd trail slot falls through to the flag/currency. */
    private fun generalCluePool(info: CityInfo): List<String> {
        if (info.clues.isNotEmpty()) return info.clues
        // Base cities have no hand-authored clues: template a trail hint from region + landmark.
        // Region is a logic switch-key, so localize only its DISPLAY here.
        val i18n = com.acme.clara.i18n.Strings
        val lm = info.landmark
        // A directional region phrase ("toward Europe") — per-language so pt can bake in the
        // preposition/contraction ("rumo à Europa") without an English word-order template.
        val rg = i18n.opt("region.dir.${info.region}") ?: "toward ${info.region}"
        // Portuguese landmark strings often start with a bare definite article ("os templos..."),
        // and pt contracts a preceding preposition with it ("por" + "os" = "pelos"). Other languages
        // bake their preposition into the template text and just take the plain landmark.
        val isPt = i18n.language == "pt"
        val lmFor0 = if (isPt) ptContract("por", lm) else lm
        val lmFor1 = if (isPt) ptContract("de", lm) else lm
        return listOf(
            listOf(
                (i18n.opt("clue.tmpl.general.0") ?: "planned to visit a place known for {0}").replace("{0}", lmFor0),
                (i18n.opt("clue.tmpl.general.1") ?: "was headed {1}, near {0}").replace("{1}", rg).replace("{0}", lmFor1),
            ).random()
        )
    }

    private fun ptContract(prep: String, noun: String): String {
        val m = Regex("^(os|as|o|a) (.+)$").find(noun) ?: return "$prep $noun"
        val (art, rest) = m.destructured
        val contraction = when (prep) {
            "por" -> when (art) { "o" -> "pelo"; "a" -> "pela"; "os" -> "pelos"; "as" -> "pelas"; else -> prep }
            "de" -> when (art) { "o" -> "do"; "a" -> "da"; "os" -> "dos"; "as" -> "das"; else -> prep }
            else -> prep
        }
        return "$contraction $rest"
    }

    /** Venue-3 payoff odds by rank: 100% for the whole free career, then linear down to 50% at the
     *  top International grade — so late-game the flag/currency bet can whiff into a joke. */
    private fun venue3Chance(rank: Int): Double {
        if (rank < Progression.FREE_RANKS) return 1.0
        val span = (Progression.LAST_RANK - Progression.FREE_RANKS).coerceAtLeast(1)
        val t = (rank - Progression.FREE_RANKS).toDouble() / span
        return (1.0 - 0.5 * t).coerceIn(0.5, 1.0)
    }

    /** Cities in play: the free original 30, plus the paid destinations of every recognition wave
     *  unlocked at the current rank (International grades 5..14 reveal waves 0..9, famous first). */
    private fun activeCities(): List<String> {
        if (!s.expansionUnlocked) return GameData.cities
        val extra = Progression.citiesUpToWave(Masterminds.unlockedMaxWave(s.rankIndex, true))
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
        val g = if (female) "f" else "m"
        fun p(key: String, en: String) = com.acme.clara.i18n.Strings.opt("pron.$key.$g") ?: en
        return frag.replace("{S}", p("subjCap", if (female) "She" else "He"))
            .replace("{s}", p("subj", if (female) "she" else "he"))
            .replace("{p}", p("poss", if (female) "her" else "his"))
    }

    /** Rewrite a suspect-aside line's generic "They/them/their" to the culprit's singular pronoun,
     *  so a tacked-on witness quip reads as the same person describing the same crook. */
    private fun suspectPronouns(line: String): String {
        val female = s.culprit?.sex == "Female"
        return line
            .replace(Regex("\\bThey\\b"), if (female) "She" else "He")
            .replace(Regex("\\bthey\\b"), if (female) "she" else "he")
            .replace(Regex("\\bthem\\b"), if (female) "her" else "him")
            .replace(Regex("\\btheir\\b"), if (female) "her" else "his")
            .replace(Regex("\\bthemselves\\b"), if (female) "herself" else "himself")
    }

    private fun traitClue(tr: Pair<String, String>): String {
        val (cat, v) = tr
        fun t(key: String, en: String) = com.acme.clara.i18n.Strings.opt(key) ?: en
        // §19: sex is never its own clue in the original — it rides inside every trait
        // sentence's pronouns. For a bare sex trait, fall back to a jewelry-neutral remark.
        val frags = GameData.traitClueFragments["$cat:$v"]
        val frag = if (frags != null) {
            val i = frags.indices.random()
            pronouns(t("trait.$cat:$v.$i", frags[i]))
        } else pronouns(when (cat) {
            "sex" -> t("trait.fallback.sex", "{S} looked like the person you're after")
            "hair" -> "{S} had $v hair"
            else -> t("trait.fallback.other", "{S} matched your description")
        })
        // ~⅓ of DOS trait lines are the bare sentence; the rest carry a lead-in
        val lead = GameData.clueLeadIns.indices.random()
            .let { t("clue.leadin.$it", GameData.clueLeadIns[it]) }
        return if (Random.nextInt(3) == 0) "$frag." else "$lead ${frag.replaceFirstChar { it.lowercase() }}."
    }

    private fun flavourFood(c: Suspect): String {
        fun t(key: String, en: String) = com.acme.clara.i18n.Strings.opt(key) ?: en
        val f = (c.feature2 + " " + c.feature1).lowercase()
        val frag = when {
            "taco" in f || "mexican" in f -> t("food.mexican", "{S} mentioned {s} liked Mexican food")
            "seafood" in f || "shellfish" in f || "lobster" in f -> t("food.seafood", "{S} mentioned {s} liked seafood")
            "spicy" in f -> t("food.spicy", "{S} mentioned {s} liked spicy food")
            else -> t("food.none", "{S} said {s} didn't like seafood")
        }
        return pronouns(frag)
    }

    // C3: the finale is always Clara San Diego, but nothing foreshadows her. Seed her name as
    // the shadowy boss behind ordinary cases so the ~14-case arc has a villain to build toward.
    /** ~12% of the time (from case 2 on, when today's crook isn't Clara herself), a witness's
     *  funny aside is instead a hushed tease about the finale nemesis — seeding the long arc. */
    private fun shouldTeaseNemesis(st: GameState): Boolean =
        st.culprit?.name != "Clara San Diego" && st.casesSolved >= 1 && Random.nextInt(8) == 0

    private fun nemesisTease(): String {
        val teases = listOf(
            "The witness drops their voice: word is a woman named Clara San Diego runs the whole operation.",
            "\"These capers all trace back to one boss,\" the witness whispers — \"a Clara San Diego.\"",
            "Someone mutters that the real mastermind, a Clara San Diego, is still out there and untouchable.",
        )
        val i = teases.indices.random()
        return com.acme.clara.i18n.Strings.opt("nemesis.$i") ?: teases[i]
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
        // Tour: opening any venue teaches "interview"; the kind of witness arms the follow-on lesson
        // (a description → the crime computer; a trail hint → identify & fly); at the hideout the
        // search itself is the arrest lesson.
        if (s.visited.size >= 3) teach("interview")   // taught once the player has tried all three venues
        if (firstVisit && v.kind == ClueKind.TRAIT) s = s.copy(sawTraitClue = true)
        if (firstVisit && v.kind == ClueKind.DESTINATION) s = s.copy(sawTrailClue = true)
        if (s.atHideout) teach("arrest")
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

    /** The overnight roll as a pure number, for estimating a case's clock at generation time. */
    private fun rollClock(clock: Int, hours: Int): Int {
        var c = clock + hours
        val hour = (9 + c) % 24
        if (hour >= 22) c += (24 - hour) + 8 else if (hour < 8) c += 8 - hour
        return c
    }

    /** Estimate the clock an efficient run of this route burns: a couple of witness opens per city
     *  (more early, while you're still building the warrant) plus the real flight between each, with
     *  the same overnight rolls play incurs — then a short hideout search. The deadline adds slack. */
    private fun estimateEfficientClock(cities: List<String>): Int {
        var clock = 0
        cities.zipWithNext().forEachIndexed { i, (a, b) ->
            clock = rollClock(clock, if (i < 2) 8 else 6)     // investigate (warrant-building costs more early)
            clock = rollClock(clock, flightCost(a, b))         // fly to the next city
        }
        return rollClock(clock, 7)                             // search the hideout
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

    /** True if two destinations sit so close on the world map that their dots/labels overlap. */
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

    /** Flight hours between any two places (distance-scaled 2..14h; the 4h fallback is only for
     *  malformed legacy/custom data because every shipped destination has a position). */
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
        teach("trail")   // committing to a flight completes the follow-the-trail lesson
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
        // Tour: only count the computer lesson as learned once a trait a witness actually gave is in
        // the machine — so computing early with a wrong/empty guess keeps the coaching on-screen.
        if (enteredHeardTrait()) teach("computer")
        checkDeadline()
        autosave()
    }

    fun anyFilterSet(): Boolean =
        listOf(s.compSex, s.compHobby, s.compHair, s.compFeature, s.compVehicle).any { it != null }

    /** True once the computer holds at least one trait a witness has actually revealed this case. */
    private fun enteredHeardTrait(): Boolean = s.revealedTraits.any { (cat, value) ->
        when (cat) {
            "sex" -> s.compSex; "hobby" -> s.compHobby; "hair" -> s.compHair
            "feature" -> s.compFeature; else -> s.compVehicle
        } == value
    }

    // ---------- endings ----------
    private fun checkDeadline() { if (s.deadlinePassed) escaped("time") }

    /** Arrived at the hideout: play the chase animation first, then show the result. */
    private fun confront() {
        val c = s.culprit!!
        val w = s.warrantFor
        val i18n = com.acme.clara.i18n.Strings
        when {
            w == null -> {
                s = s.copy(won = false, resultLines = listOf(
                    i18n.ui("This is Interpol."),
                    GameData.CAUGHT_UP.replace("%s", c.name),
                    GameData.NO_WARRANT_ESCAPE,
                    i18n.ui("The gang has pulled off another caper and vanished!"),
                ))
            }
            w.name != c.name -> {
                s = s.copy(won = false, resultLines = listOf(
                    i18n.ui("Your trail has led you to {0}.", i18n.place(s.currentCity)),
                    GameData.FALSE_WARRANT.replace("%s", w.name),
                    GameData.FALSE_ARREST,
                    i18n.ui("Better luck on your next assignment."),
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
        autosave()
    }

    private fun win(c: Suspect) {
        val crimeCity = s.route.firstOrNull() ?: s.currentCity
        val i18n = com.acme.clara.i18n.Strings
        val crimeCityL = i18n.place(crimeCity)
        val newCases = s.casesSolved + 1
        val paid = s.expansionUnlocked
        // Case 14 is the story's inciting incident, not an ordinary capture: Clara always gets
        // away, whether or not the International track is already unlocked (see Masterminds.kt).
        val isCase14Clara = c.name == "Clara San Diego" && newCases == GameState.CAREER_CASES
        val campaignCases = newCases - GameState.CAREER_CASES
        val arc = if (paid && campaignCases > 0) Masterminds.arcForCampaignCase(campaignCases) else null
        val isFinale = arc?.final == true
        val lines = mutableListOf<String>()
        when {
            isCase14Clara -> {
                lines += GameData.GOT_AWAY
                lines += i18n.ui("She left behind proof this wasn't a lone operation — a coded warrant naming the leader of a crime family in Europe.")
            }
            isFinale -> {
                lines += GameData.CLARA_JAILED
                lines += i18n.ui("Congratulations - Interpol's most-wanted list is one name shorter tonight!")
                lines += i18n.ui("{0} was taken in the same raid, closing out the last of the five families.", c.name)
            }
            arc != null -> {
                lines += i18n.ui("{0}'s network in {1} is dismantled — {2} is in custody.",
                    i18n.label("mastermind.role", arc.role), i18n.label("mastermind.family", arc.family), c.name)
                lines += GameData.LOOT.replaceFirst("%s", c.name)
                    .replaceFirst("%s", Treasures.localized(s.treasure)).replaceFirst("%s", crimeCityL)
                if (arc.claraFlavor) lines += i18n.ui("Clara was seen fleeing the scene — she got away again.")
            }
            else -> {
                // faithful phrasing: the CRIME city's police make the arrest and get the loot back
                lines += GameData.APPREHENDED.replaceFirst("%s", crimeCityL).replaceFirst("%s", c.name)
                lines += GameData.LOOT.replaceFirst("%s", c.name)
                    .replaceFirst("%s", Treasures.localized(s.treasure)).replaceFirst("%s", crimeCityL)
            }
        }
        // R1 peak-end: sign off warm and personal, not on a cold Interpol form line.
        lines += when {
            isFinale -> i18n.ui("Take a bow, {0}. You began as a rookie — and you brought in the one who slipped past everyone else.", s.detectiveName)
            isCase14Clara -> i18n.ui("Not this time, {0}. But you're closer than anyone's ever been — and now you know her network's name.", s.detectiveName)
            else -> i18n.ui("Get some rest, {0}. Thanks to you, {1} sleeps easier tonight.", s.detectiveName, crimeCityL)
        }
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
        if (streak >= 2) lines += i18n.ui("🔥 {0}-day case streak!", streak)
        // Only the true finale (Clara's real capture, Chief Director) ends the career now — Case 14
        // never does, paid or not, since she's always an escape there (see Masterminds.kt header).
        val careerOver = isFinale
        // Free ranks retain the 1990 cadence. Every paid wave finale, including Wave 10, awards
        // its patent through the existing quiz.
        val freeThreshold = newCases in setOf(1, 5, 9, 13)
        val promote = s.rankIndex < GameData.ranks.lastIndex &&
            (freeThreshold || arc != null)
        if (promote) {
            lines += GameData.PROMOTION.replace("%s", s.detectiveName)
            lines += i18n.ui("One last puzzle stands between you and the promotion.")
        }
        // update the career record: capture the villain, tally clean / hint-free solves,
        // then unlock any newly-earned commendations from the resulting record
        // An escape at Case 14 must not mark Clara "captured" in the Most Wanted gallery — she
        // isn't, until the true finale (isFinale) actually jails her.
        val captured = when {
            isCase14Clara -> s.capturedVillains
            isFinale -> s.capturedVillains + c.name + "Clara San Diego"
            else -> s.capturedVillains + c.name
        }
        val cleanSweep = s.hadCleanCase || s.wrongFlights == 0
        val hintFree = if (s.hintsUsed == 0) s.hintFreeSolves + 1 else s.hintFreeSolves
        var next = s.copy(
            phase = Phase.RESULT, won = true, casesSolved = newCases,
            resultLines = lines, pendingPromotion = promote, careerOver = careerOver,
            rankIndex = s.rankIndex,
            capturedVillains = captured, hadCleanCase = cleanSweep, hintFreeSolves = hintFree,
            streakDays = streak, streakFreezes = freezes, lastSolveEpochDay = today,
        )
        next = next.copy(
            unlockedAchievements = next.unlockedAchievements +
                Achievements.earned(Achievements.summarise(next)),
            tutorialDone = true, tutorialActive = false,
        )
        s = next
        autosave()
    }

    // ---------- debug-only test shortcuts (menu entry gated behind BuildConfig.DEBUG — never
    // reachable in a release build; see GameMenuBar's Debug menu) ----------

    /** Skip sign-on/briefing entirely and land already at the hideout doorstep — for a debug
     *  build's cold start (a fresh install with no save), so testing the chase/result flow never
     *  needs playing through name-entry and the whole clue-gathering loop first. */
    fun devAutoStart() {
        if (profileId == null) profileId = newProfileId()
        // tutorialDone = true: skip the Rookie tour too — its spotlight otherwise swallows taps
        // outside itself, which just gets in the way of jumping straight into the Debug menu.
        s = GameState(detectiveName = "Tester", tutorialDone = true,
            expansionUnlocked = repo?.ownsExpansion() == true || s.expansionUnlocked)
        devJumpToHideoutDoorstep()
    }

    /** Jump straight to the hideout with two of its three venues already (harmlessly) visited, so
     *  opening the one remaining venue immediately catches the suspect — skips the whole
     *  clue-gathering loop this normally takes, for fast manual testing of the chase/result flow. */
    fun devJumpToHideoutDoorstep() {
        if (s.route.isEmpty() || s.culprit == null) newCase()
        val culprit = s.culprit ?: return
        s = s.copy(
            phase = Phase.CITY, overlay = null, openClue = null,
            warrantFor = culprit, computed = true,
            currentCity = s.hideout, progress = s.route.size - 1, onTrack = true,
            visited = emptySet(),
        )
        buildVenues()
        // Two of the three venues "tried" without running their real capture-odds check (which is
        // partly random on the 2nd try) — deterministic, so the one remaining venue is guaranteed
        // to catch the suspect the moment it's opened.
        s = s.copy(visited = s.venues.indices.take(2).toSet())
        autosave()
    }

    /** Jump straight to a finished, winning case on the Result screen — [promotion] also exercises
     *  the promotion-quiz flow instead of going straight to "ready for your next case?". */
    fun devJumpToResultWin(promotion: Boolean) {
        if (s.route.isEmpty() || s.culprit == null) newCase()
        val culprit = s.culprit ?: return
        s = s.copy(overlay = null, openClue = null, warrantFor = culprit, computed = true)
        win(culprit)
        s = s.copy(pendingPromotion = promotion)
        autosave()
    }

    /** The promotion quiz was answered: bump the rank only when correct (like the original). */
    fun resolvePromotion(correct: Boolean) {
        val newRank = if (correct && s.rankIndex < GameData.ranks.lastIndex) s.rankIndex + 1 else s.rankIndex
        s = s.copy(rankIndex = newRank, pendingPromotion = false)
        autosave()
    }

    /** Cases remaining until the next free promotion or explicit campaign-wave finale. */
    fun casesToNextPromotion(): Int {
        val thresholds = if (s.expansionUnlocked)
            listOf(1, 5, 9, 13) + Masterminds.waveEndCases.map { it + GameState.CAREER_CASES }
            else listOf(1, 5, 9, 13)
        val next = thresholds.firstOrNull { it > s.casesSolved } ?: return 0
        return next - s.casesSolved
    }

    fun completedCampaignArc() = Masterminds.arcForCampaignCase(s.casesSolved - GameState.CAREER_CASES)

    fun openCasePlanner() {
        s = when {
            s.expansionUnlocked -> s.copy(overlay = Overlay.CasePlanner)
            com.acme.clara.billing.BillingManager.SALES_ENABLED -> s.copy(overlay = Overlay.PurchaseOffer("Case Planner"))
            else -> s
        }
    }

    /** The mastermind arc the player is currently working toward — a caption for RankProgress
     *  ("Toward Field Inspector — 3 of 8 · the Americas, Boss"). Null before Case 14 or once the
     *  campaign is finished (rank already at Chief Director). */
    private fun escaped(reason: String) {
        val c = s.culprit!!
        // R2 near-miss: only when the loss was genuinely close — on the right trail and at (or
        // one hop from) the hideout when the clock ran out. A wrong-warrant bust isn't a
        // near-miss; that loss is handled in confront() and stays instructive.
        val nearMiss = reason == "time" && s.onTrack &&
            (s.atHideout || s.progress >= s.route.size - 1)
        val i18n = com.acme.clara.i18n.Strings
        val lines = when {
            nearMiss -> listOf(
                i18n.ui("So close."),
                i18n.ui("You reached {0} just as {1} slipped out the back — minutes too late. The trail was right; the clock beat you.",
                    i18n.place(s.hideout), c.name),
            )
            reason == "time" -> listOf(
                i18n.ui("Incoming from Interpol:"), i18n.ui("Unwelcome news..."),
                GameData.TOO_LONG.replace("%s", c.name),
            )
            else -> listOf(i18n.ui("The suspect has escaped!"))
        }
        s = s.copy(phase = Phase.RESULT, won = false, resultLines = lines)
        cue(SoundCue.OUT_OF_TIME)
    }

    fun nextCase() { menuNewCase() }
    fun toBriefingForNext() { menuNewCase() }

    // ---------- time formatting ----------
    fun clockLabel(offsetHours: Int = 0): String {
        return clockLabelAt(s.clock + offsetHours)
    }

    /** The planner's committed-arrival time, including the same 10 p.m.–8 a.m. sleep roll that
     *  [arrive] applies. A simple clock offset can otherwise promise a late-night arrival that
     *  gameplay immediately changes to 8 a.m. the next morning. */
    fun arrivalClockHours(flightHours: Int): Int = rollClock(s.clock, flightHours)
    fun arrivalClockLabel(flightHours: Int): String = clockLabelAt(arrivalClockHours(flightHours))

    private fun clockLabelAt(clockHours: Int): String {
        val total = 9 + clockHours
        val day = (total / 24).coerceIn(0, 6)
        val hour = total % 24
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val i18n = com.acme.clara.i18n.Strings
        val (h, ampm) = when {
            hour == 0 -> 12 to i18n.ui("a.m.")
            hour < 12 -> hour to i18n.ui("a.m.")
            hour == 12 -> 12 to i18n.ui("p.m.")
            else -> hour - 12 to i18n.ui("p.m.")
        }
        return "${i18n.opt("day.$day") ?: days[day]}, $h $ampm"
    }

    /** Compact time-until-deadline hint, e.g. "3d 4h left" or "18h left" when close. */
    fun deadlineLabel(offsetHours: Int = 0): String {
        val left = (s.caseDeadlineHours - s.clock - offsetHours).coerceAtLeast(0)
        val i18n = com.acme.clara.i18n.Strings
        return if (left >= 24) i18n.ui("{0}d {1}h left", left / 24, left % 24)
               else i18n.ui("{0}h left", left)
    }
}
