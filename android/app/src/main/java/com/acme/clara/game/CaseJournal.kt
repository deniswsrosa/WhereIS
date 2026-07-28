package com.acme.clara.game

/**
 * Derives the case-log views from the running [GameState.journal]: the leads and traits
 * you've gathered, plus a one-line "Previously…" recap so a case survives a break away.
 * It surfaces the facts you collected — never conclusions.
 */
object CaseJournal {

    fun leads(state: GameState): List<JournalEntry> =
        state.journal.filter { it.kind == ClueKind.DESTINATION }

    fun traits(state: GameState): List<JournalEntry> =
        state.journal.filter { it.kind == ClueKind.TRAIT }

    /** A short recap for the resume/return screen; null before a case is properly underway. */
    fun recap(state: GameState): String? {
        val from = state.route.firstOrNull() ?: return null
        if (state.currentCity.isBlank()) return null
        return if (from == state.currentCity) {
            "Your case opens in $from."
        } else {
            "Previously… you trailed the thief from $from to ${state.currentCity}."
        }
    }
}
