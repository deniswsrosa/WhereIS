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
        val i18n = com.acme.clara.i18n.Strings
        return if (from == state.currentCity) {
            i18n.ui("Your case opens in {0}.", i18n.place(from))
        } else {
            i18n.ui("Previously… you trailed the thief from {0} to {1}.",
                i18n.place(from), i18n.place(state.currentCity))
        }
    }
}
