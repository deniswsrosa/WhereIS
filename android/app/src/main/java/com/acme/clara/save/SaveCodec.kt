package com.acme.clara.save

import com.acme.clara.data.GameData
import com.acme.clara.data.Suspect
import com.acme.clara.game.ClueKind
import com.acme.clara.game.GameState
import com.acme.clara.game.JournalEntry
import com.acme.clara.game.Phase
import com.acme.clara.game.Venue

/**
 * Serialises a career ([SaveMeta] + [GameState]) to and from a JSON string via [Json].
 *
 * The whole state is captured — including the in-flight case (route, clock, venues,
 * warrant, computer entries) — so a reopen restores the exact moment, which is what makes
 * the save anti-scum: a wrong flight can't be reloaded away. Transient UI fields (overlay,
 * open clue, flight animation, sighting interstitial) are deliberately dropped.
 */
object SaveCodec {
    const val VERSION = 1

    fun encode(meta: SaveMeta, state: GameState): String =
        Json.encode(
            linkedMapOf(
                "v" to VERSION,
                "meta" to metaMap(meta),
                "state" to stateMap(state),
            )
        )

    fun decode(text: String): SaveData? = try {
        val root = Json.decode(text) as? Map<*, *> ?: return null
        val meta = readMeta(root["meta"] as? Map<*, *> ?: return null) ?: return null
        val state = readState(root["state"] as? Map<*, *> ?: return null)
        SaveData(meta, state)
    } catch (e: Exception) {
        null
    }

    // ---------- meta ----------
    private fun metaMap(m: SaveMeta) = linkedMapOf<String, Any?>(
        "id" to m.id, "name" to m.name, "rankIndex" to m.rankIndex,
        "casesSolved" to m.casesSolved, "lastPlayed" to m.lastPlayed,
    )

    private fun readMeta(m: Map<*, *>): SaveMeta? {
        val id = m["id"] as? String ?: return null
        return SaveMeta(
            id = id,
            name = str(m["name"]),
            rankIndex = int(m["rankIndex"]),
            casesSolved = int(m["casesSolved"]),
            lastPlayed = long(m["lastPlayed"]),
        )
    }

    // ---------- state ----------
    private fun stateMap(s: GameState): Map<String, Any?> = linkedMapOf(
        "phase" to s.phase.name,
        "detectiveName" to s.detectiveName,
        "rankIndex" to s.rankIndex,
        "casesSolved" to s.casesSolved,
        "culprit" to s.culprit?.name,
        "treasure" to s.treasure,
        "route" to s.route,
        "progress" to s.progress,
        "currentCity" to s.currentCity,
        "clock" to s.clock,
        "onTrack" to s.onTrack,
        "revealOrder" to s.revealOrder.map { listOf(it.first, it.second) },
        "revealedCount" to s.revealedCount,
        "venues" to s.venues.map { venueMap(it) },
        "visited" to s.visited.toList(),
        "compSex" to s.compSex, "compHobby" to s.compHobby, "compHair" to s.compHair,
        "compFeature" to s.compFeature, "compVehicle" to s.compVehicle,
        "warrantFor" to s.warrantFor?.name,
        "computed" to s.computed,
        "departOptions" to s.departOptions,
        "careerOver" to s.careerOver,
        "selectedTool" to s.selectedTool,
        "won" to s.won,
        "resultLines" to s.resultLines,
        "pendingPromotion" to s.pendingPromotion,
        "soundOn" to s.soundOn,
        "hapticsOn" to s.hapticsOn,
        "captionsOn" to s.captionsOn,
        "wrongFlights" to s.wrongFlights,
        "hintsUsed" to s.hintsUsed,
        "journal" to s.journal.map { journalMap(it) },
        "capturedVillains" to s.capturedVillains.toList(),
        "unlockedAchievements" to s.unlockedAchievements.toList(),
        "hintFreeSolves" to s.hintFreeSolves,
        "hadCleanCase" to s.hadCleanCase,
        "expansionUnlocked" to s.expansionUnlocked,
        "freeHints" to s.freeHints,
        "tutorialDone" to s.tutorialDone,
        "tutorialStep" to s.tutorialStep,
    )

    private fun readState(m: Map<*, *>): GameState {
        val phase = runCatching { Phase.valueOf(str(m["phase"])) }.getOrDefault(Phase.CITY)
        // Never resume into a transient animation/front-of-house phase — clamp to CITY.
        val safePhase = when (phase) {
            Phase.CITY, Phase.CRIME, Phase.BRIEFING, Phase.RESULT -> phase
            else -> Phase.CITY
        }
        return GameState(
            phase = safePhase,
            detectiveName = str(m["detectiveName"]),
            rankIndex = int(m["rankIndex"]),
            casesSolved = int(m["casesSolved"]),
            culprit = suspect(m["culprit"]),
            treasure = str(m["treasure"]),
            route = strList(m["route"]),
            progress = int(m["progress"]),
            currentCity = str(m["currentCity"]),
            clock = int(m["clock"]),
            onTrack = bool(m["onTrack"], true),
            revealOrder = pairList(m["revealOrder"]),
            revealedCount = int(m["revealedCount"]),
            venues = venueList(m["venues"]),
            visited = intList(m["visited"]).toSet(),
            compSex = nstr(m["compSex"]), compHobby = nstr(m["compHobby"]), compHair = nstr(m["compHair"]),
            compFeature = nstr(m["compFeature"]), compVehicle = nstr(m["compVehicle"]),
            warrantFor = suspect(m["warrantFor"]),
            computed = bool(m["computed"], false),
            departOptions = strList(m["departOptions"]),
            careerOver = bool(m["careerOver"], false),
            selectedTool = int(m["selectedTool"], 2),
            won = bool(m["won"], false),
            resultLines = strList(m["resultLines"]),
            pendingPromotion = bool(m["pendingPromotion"], false),
            soundOn = bool(m["soundOn"], true),
            hapticsOn = bool(m["hapticsOn"], true),
            captionsOn = bool(m["captionsOn"], false),
            wrongFlights = int(m["wrongFlights"]),
            hintsUsed = int(m["hintsUsed"]),
            journal = journalList(m["journal"]),
            capturedVillains = strList(m["capturedVillains"]).toSet(),
            unlockedAchievements = strList(m["unlockedAchievements"]).toSet(),
            expansionUnlocked = bool(m["expansionUnlocked"], false),
            hintFreeSolves = int(m["hintFreeSolves"]),
            hadCleanCase = bool(m["hadCleanCase"], false),
            freeHints = int(m["freeHints"]),
            tutorialDone = bool(m["tutorialDone"], false),
            tutorialStep = int(m["tutorialStep"], -1),
        )
    }

    private fun venueMap(v: Venue) = linkedMapOf<String, Any?>(
        "place" to v.place, "occupation" to v.occupation, "kind" to v.kind.name,
        "text" to v.text, "trait" to v.trait?.let { listOf(it.first, it.second) },
    )

    private fun venueList(v: Any?): List<Venue> = (v as? List<*>).orEmpty().mapNotNull { e ->
        val mm = e as? Map<*, *> ?: return@mapNotNull null
        Venue(
            place = str(mm["place"]), occupation = str(mm["occupation"]),
            kind = runCatching { ClueKind.valueOf(str(mm["kind"])) }.getOrDefault(ClueKind.NONE),
            text = str(mm["text"]),
            trait = (mm["trait"] as? List<*>)?.takeIf { it.size == 2 }
                ?.let { (it[0] as String) to (it[1] as String) },
        )
    }

    private fun journalMap(j: JournalEntry) = linkedMapOf<String, Any?>(
        "kind" to j.kind.name, "text" to j.text, "city" to j.city,
    )

    private fun journalList(v: Any?): List<JournalEntry> = (v as? List<*>).orEmpty().mapNotNull { e ->
        val mm = e as? Map<*, *> ?: return@mapNotNull null
        JournalEntry(
            kind = runCatching { ClueKind.valueOf(str(mm["kind"])) }.getOrDefault(ClueKind.NONE),
            text = str(mm["text"]), city = str(mm["city"]),
        )
    }

    // ---------- primitives ----------
    private fun suspect(v: Any?): Suspect? =
        (v as? String)?.let { name -> GameData.suspects.firstOrNull { it.name == name } }

    private fun int(v: Any?, def: Int = 0): Int = (v as? Long)?.toInt() ?: (v as? Int) ?: def
    private fun long(v: Any?, def: Long = 0L): Long = (v as? Long) ?: (v as? Int)?.toLong() ?: def
    private fun bool(v: Any?, def: Boolean): Boolean = (v as? Boolean) ?: def
    private fun str(v: Any?): String = v as? String ?: ""
    private fun nstr(v: Any?): String? = v as? String
    private fun strList(v: Any?): List<String> = (v as? List<*>).orEmpty().filterIsInstance<String>()
    private fun intList(v: Any?): List<Int> =
        (v as? List<*>).orEmpty().mapNotNull { (it as? Long)?.toInt() ?: (it as? Int) }

    private fun pairList(v: Any?): List<Pair<String, String>> = (v as? List<*>).orEmpty().mapNotNull { e ->
        (e as? List<*>)?.takeIf { it.size == 2 }?.let { (it[0] as String) to (it[1] as String) }
    }
}
