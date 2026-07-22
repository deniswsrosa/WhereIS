package com.acme.carmen.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.acme.carmen.data.CityMeta
import com.acme.carmen.data.GameData
import com.acme.carmen.data.Suspect
import kotlin.random.Random

enum class Phase { TITLE, SIGN_ON, BRIEFING, CITY, TRAVEL, CRIME, RESULT }

enum class ClueKind { DESTINATION, TRAIT, DANGER, NONE }

/** Menu-bar overlays. */
sealed interface Overlay {
    data object About : Overlay
    data object Roster : Overlay
    data object HallOfFame : Overlay
    data object Dossiers : Overlay
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
    val phase: Phase = Phase.TITLE,
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
    // result
    val won: Boolean = false,
    val resultLines: List<String> = emptyList(),
    // ui
    val overlay: Overlay? = null,
    val soundOn: Boolean = true,
) {
    val revealedTraits: List<Pair<String, String>> get() = revealOrder.take(revealedCount)
    val deadlinePassed: Boolean get() = clock > DEADLINE_HOURS
    val hideout: String get() = route.lastOrNull() ?: ""
    val atHideout: Boolean get() = currentCity == hideout && onTrack
    companion object { const val DEADLINE_HOURS = 152 } // Mon 9am -> Sun 5pm
}

class CarmenViewModel : ViewModel() {
    var s by mutableStateOf(GameState())
        private set

    // ---------- flow ----------
    fun start() { s = s.copy(phase = Phase.SIGN_ON) }

    fun signOn(name: String) {
        val nm = name.trim().ifBlank { "Gumshoe" }
        s = GameState(phase = Phase.BRIEFING, detectiveName = nm)
        newCase()
    }

    fun beginInvestigation() { s = s.copy(phase = Phase.CITY) }

    // ---------- navigation ----------
    fun gotoCity() { s = s.copy(phase = Phase.CITY) }
    fun gotoTravel() { s = s.copy(phase = Phase.TRAVEL) }
    fun gotoCrime() { autoFillFromClues(); s = s.copy(phase = Phase.CRIME) }

    // ---------- menu bar ----------
    fun openOverlay(o: Overlay) { s = s.copy(overlay = o) }
    fun dismissOverlay() { s = s.copy(overlay = null) }
    fun menuNewCase() { s = s.copy(overlay = null, phase = Phase.BRIEFING); newCase() }
    fun menuQuitToTitle() { s = GameState() }
    fun toggleSound() {
        val on = !s.soundOn
        s = s.copy(soundOn = on, overlay = Overlay.Info("SOUND",
            listOf("Sound is now ${if (on) "ON" else "OFF"}.", "",
                "(Digitized & MIDI audio are", "not yet wired up in this remake.)")))
    }
    fun showJoystick() {
        s = s.copy(overlay = Overlay.Info("JOYSTICK",
            listOf("Joystick improperly centered", "or not present.")))
    }

    // ---------- case generation ----------
    private fun newCase() {
        val carmen = GameData.suspects.first { it.name == "Carmen Sandiego" }
        val pool = GameData.suspects.filter { it.name != "Carmen Sandiego" }
        val culprit = if (s.rankIndex >= 4) carmen else pool.random()

        val order = discriminatingOrder(culprit)
        val base = 3 + s.rankIndex          // route grows with rank
        val routeLen = maxOf(base, order.size + 1).coerceAtMost(6)

        val cities = GameData.cities.shuffled().take(routeLen)
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
        )
        buildVenues()
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
        val occs = GameData.occupations.shuffled()
        val list = mutableListOf<Venue>()
        val onTrack = st.currentCity == st.route.getOrNull(st.progress) && st.onTrack

        if (!onTrack) {
            places.forEachIndexed { i, p ->
                list.add(Venue(p, occs[i], ClueKind.NONE, GameData.noInformation.random()))
            }
        } else {
            val isHideout = st.currentCity == st.hideout
            val nextCity = st.route.getOrNull(st.progress + 1)
            var slot = 0
            places.forEachIndexed { i, p ->
                val occ = occs[i]
                if (!isHideout && slot == 0 && nextCity != null) {
                    slot++
                    list.add(Venue(p, occ, ClueKind.DESTINATION, destinationClue(nextCity)))
                } else {
                    // trait clue if any left, else danger/flavour
                    val idx = st.revealedCount + list.count { it.kind == ClueKind.TRAIT }
                    val tr = st.revealOrder.getOrNull(idx)
                    if (tr != null) {
                        list.add(Venue(p, occ, ClueKind.TRAIT, traitClue(tr), tr))
                    } else {
                        val txt = if (isHideout) GameData.dangerMessages.random()
                        else "${GameData.clueLeadIns.random()} ${flavourFood(st.culprit!!)}."
                        list.add(Venue(p, occ, ClueKind.DANGER, txt))
                    }
                }
            }
        }
        s = s.copy(venues = list, visited = emptySet(), openClue = null)
    }

    private fun destinationClue(next: String): String {
        val info = CityMeta.of(next)
        val lead = GameData.clueLeadIns.random()
        val she = if (s.culprit?.sex == "Female") "she" else "he"
        return "$lead $she was headed for a city in ${info.region}, known for ${info.landmark}."
    }

    private fun traitClue(tr: Pair<String, String>): String {
        val lead = GameData.clueLeadIns.random()
        val (cat, v) = tr
        val she = if (s.culprit?.sex == "Female") "She" else "He"
        val frag = when (cat) {
            "sex" -> "the suspect was ${if (v == "female") "a woman" else "a man"}"
            "hobby" -> when (v) {
                "tennis" -> "${she.lowercase()} enjoyed playing tennis"
                "mt. climbing" -> "${she.lowercase()} was a mountain climber"
                "croquet" -> "${she.lowercase()} played croquet"
                else -> "${she.lowercase()} liked $v"
            }
            "hair" -> "the suspect had $v hair"
            "feature" -> when (v) {
                "ring" -> "the suspect wore a large ring"
                "tattoo" -> "I noticed a tattoo on the suspect"
                "jewelry" -> "the suspect wore fancy jewelry"
                "scar" -> "the suspect had a noticeable scar"
                else -> "the suspect $v"
            }
            "vehicle" -> when (v) {
                "convertible" -> "the suspect arrived in a convertible"
                "limousine" -> "the suspect was driving a limo"
                "motorcycle" -> "the suspect arrived on a motorcycle"
                "race car" -> "the suspect sped off in a race car"
                else -> "the suspect had a $v"
            }
            else -> "the suspect looked suspicious"
        }
        return "$lead $frag."
    }

    private fun flavourFood(c: Suspect): String {
        val f = (c.feature2 + " " + c.feature1).lowercase()
        return when {
            "taco" in f || "mexican" in f -> "the suspect asked where to find good Mexican food"
            "seafood" in f || "shellfish" in f || "lobster" in f -> "the suspect ordered a lot of seafood"
            "spicy" in f -> "the suspect loved spicy food"
            else -> "the suspect was acting suspicious"
        }
    }

    // ---------- player actions in a city ----------
    fun openVenue(index: Int) {
        val v = s.venues.getOrNull(index) ?: return
        if (index !in s.visited) {
            var st = s
            if (v.kind == ClueKind.TRAIT) st = st.copy(revealedCount = st.revealedCount + 1)
            st = st.copy(clock = st.clock + 1, visited = st.visited + index)
            s = st
        }
        s = s.copy(openClue = v)
        checkDeadline()
    }

    fun closeClue() { s = s.copy(openClue = null) }

    // ---------- travel ----------
    fun travelOptions(): List<String> {
        val correct = s.route.getOrNull(s.progress + 1) ?: return emptyList()
        val decoys = GameData.cities
            .filter { it != correct && it !in s.route.take(s.progress + 1) }
            .shuffled().take(3)
        return (decoys + correct).shuffled()
    }

    fun travelTo(city: String) {
        val correct = s.route.getOrNull(s.progress + 1)
        val cost = Random.nextInt(10, 25)
        var st = s.copy(clock = s.clock + cost)
        if (city == correct) {
            st = st.copy(progress = st.progress + 1, currentCity = city, onTrack = true)
        } else {
            st = st.copy(currentCity = city, onTrack = false)
        }
        s = st
        if (s.deadlinePassed) { escaped("time"); return }
        // arrival at hideout triggers the confrontation
        if (s.atHideout) { confront(); return }
        s = s.copy(phase = Phase.CITY)
        buildVenues()
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

    fun compute() { s = s.copy(computed = true) }

    fun anyFilterSet(): Boolean =
        listOf(s.compSex, s.compHobby, s.compHair, s.compFeature, s.compVehicle).any { it != null }

    fun issueWarrant(su: Suspect) { s = s.copy(warrantFor = su, phase = Phase.CITY) }

    /** Pre-fill the computer with what witnesses have revealed. */
    fun autoFillFromClues() {
        var st = s
        s.revealedTraits.forEach { (cat, v) ->
            st = when (cat) {
                "sex" -> st.copy(compSex = v); "hobby" -> st.copy(compHobby = v)
                "hair" -> st.copy(compHair = v); "feature" -> st.copy(compFeature = v)
                else -> st.copy(compVehicle = v)
            }
        }
        s = st.copy(computed = true)
    }

    // ---------- endings ----------
    private fun checkDeadline() { if (s.deadlinePassed) escaped("time") }

    private fun confront() {
        val c = s.culprit!!
        val w = s.warrantFor
        when {
            w == null -> {
                s = s.copy(phase = Phase.RESULT, won = false, resultLines = listOf(
                    "Interpol here.",
                    "You have caught up with ${c.name}.",
                    GameData.NO_WARRANT.let { "However, without a warrant we cannot make a legal arrest!" },
                    "It looks like the gang has gotten away with another caper!",
                ))
            }
            w.name != c.name -> {
                s = s.copy(phase = Phase.RESULT, won = false, resultLines = listOf(
                    "You have trailed the suspect to ${s.currentCity}.",
                    "Unfortunately, you have a warrant for ${w.name}.",
                    "Be careful, we could all be sued for false arrest!",
                    "We hope you do better on your next case.",
                ))
            }
            else -> win(c)
        }
    }

    private fun win(c: Suspect) {
        val lines = mutableListOf<String>()
        if (c.name == "Carmen Sandiego") {
            lines += "You have successfully arrested the ring-leader, Carmen Sandiego, and sent her to jail for good!"
            lines += "Congratulations, your name will go into the Interpol Hall of Fame!"
        } else {
            lines += "Thanks to your help, the ${s.currentCity} police have apprehended ${c.name}."
            lines += "${c.name} had the loot, ${s.treasure}, which will be returned to its grateful owners."
        }
        lines += "We here at Interpol thank you for your good work on this case."
        val newCases = s.casesSolved + 1
        var newRank = s.rankIndex
        if (newCases % 2 == 0 && newRank < GameData.ranks.lastIndex) {
            newRank++
            lines += "Good job, ${s.detectiveName}, you have earned a promotion."
            lines += "Your new rank is: ${GameData.ranks[newRank]}."
        }
        s = s.copy(phase = Phase.RESULT, won = true, casesSolved = newCases, rankIndex = newRank, resultLines = lines)
    }

    private fun escaped(reason: String) {
        val c = s.culprit!!
        val lines = if (reason == "time") listOf(
            "Message from Interpol:", "Bad news...",
            "We've just received word that ${c.name} slipped through your fingers because your investigation took too long!",
        ) else listOf("The suspect got away!")
        s = s.copy(phase = Phase.RESULT, won = false, resultLines = lines)
    }

    fun nextCase() { newCase() }
    fun toBriefingForNext() { s = s.copy(phase = Phase.BRIEFING); newCase() }

    // ---------- time formatting ----------
    fun clockLabel(): String {
        val total = 9 + s.clock
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
}
