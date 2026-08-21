package com.acme.clara.save

import com.acme.clara.game.GameState

/** The lightweight header shown in the "Choose a game" picker, without loading the full state. */
data class SaveMeta(
    val id: String,
    val name: String,
    val rankIndex: Int,
    val casesSolved: Int,
    val lastPlayed: Long,   // epoch millis; sorts the picker, newest first
)

/** A full saved career: its header plus the complete in-progress [GameState]. */
data class SaveData(val meta: SaveMeta, val state: GameState)

/** What the app should do on launch, decided purely from the list of existing saves. */
sealed interface LaunchOutcome {
    /** No saves — go to sign-on and start a fresh career. */
    data object SignOn : LaunchOutcome
    /** Exactly one save — continue it with no menus. */
    data class Continue(val id: String) : LaunchOutcome
    /** Two or more — show the picker (metas sorted newest first). */
    data class Choose(val metas: List<SaveMeta>) : LaunchOutcome
    /** A new-career sign-on was opened but no identity was confirmed yet. */
    data object PendingSignOn : LaunchOutcome
}

/** The launch decision: an interrupted new-career flow wins; otherwise 0 saves → sign-on,
 * 1 → continue, 2+ → choose. */
fun decideLaunch(saves: List<SaveMeta>, pendingSignOn: Boolean = false): LaunchOutcome =
    if (pendingSignOn) LaunchOutcome.PendingSignOn else when (saves.size) {
        0 -> LaunchOutcome.SignOn
        1 -> LaunchOutcome.Continue(saves.first().id)
        else -> LaunchOutcome.Choose(saves.sortedByDescending { it.lastPlayed })
    }

/** Storage for career saves. Implementations back onto disk (prod) or memory (tests). */
interface SaveRepository {
    /** All saves, newest first. */
    fun list(): List<SaveMeta>
    fun load(id: String): SaveData?
    fun save(data: SaveData)
    fun delete(id: String)

    /** Durable draft marker for a sign-on that has not been confirmed yet. It is deliberately
     * separate from the save list so the picker never shows a nameless detective. */
    fun hasPendingSignOn(): Boolean
    fun setPendingSignOn(pending: Boolean)

    /** Persist the first confirmed state, then clear the pending marker. */
    fun saveNewCareer(data: SaveData)

    /** App-wide non-consumable ownership. Unlike campaign progress, this belongs to the player,
     *  not to one career, so every existing and future profile inherits it. */
    fun ownsExpansion(): Boolean
    fun setExpansionOwned()
}

/** In-memory repository — the reference implementation used by unit tests. */
class InMemorySaveRepository : SaveRepository {
    private val store = LinkedHashMap<String, SaveData>()
    private var expansionOwned = false
    private var pendingSignOn = false

    override fun list(): List<SaveMeta> =
        store.values.map { it.meta }.sortedByDescending { it.lastPlayed }

    override fun load(id: String): SaveData? = store[id]

    override fun save(data: SaveData) { store[data.meta.id] = data }

    override fun delete(id: String) { store.remove(id) }

    override fun hasPendingSignOn(): Boolean = pendingSignOn

    override fun setPendingSignOn(pending: Boolean) { pendingSignOn = pending }

    override fun saveNewCareer(data: SaveData) {
        save(data)
        pendingSignOn = false
    }

    override fun ownsExpansion(): Boolean = expansionOwned

    override fun setExpansionOwned() { expansionOwned = true }
}
