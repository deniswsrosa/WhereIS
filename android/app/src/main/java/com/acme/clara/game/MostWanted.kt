package com.acme.clara.game

import com.acme.clara.data.GameData

/** One slot on the Most-Wanted board. [captured] gates the reveal — locked entries hide the name. */
data class WantedEntry(val name: String, val captured: Boolean)

/**
 * The Most-Wanted gallery. It mirrors the fixed villain roster; each mugshot/dossier
 * unlocks the first time that villain is jailed, so the board fills in as you play.
 */
object MostWanted {
    /** The whole board in roster order, each flagged by whether the player has captured them. */
    fun gallery(captured: Set<String>): List<WantedEntry> =
        GameData.suspects.map { WantedEntry(it.name, it.name in captured) }

    fun capturedCount(captured: Set<String>): Int = gallery(captured).count { it.captured }

    fun total(): Int = GameData.suspects.size

    fun allCaught(captured: Set<String>): Boolean = capturedCount(captured) >= total()
}
