package com.acme.clara.game

/**
 * The diegetic Rookie tutorial: a guided first case that teaches one mechanic at a time. The
 * steps advance as the player performs the taught action (see [ClaraViewModel]); the coach-mark
 * shows [message] for the current step, and the player can SKIP at any point.
 */
object Tutorial {
    const val STEPS = 7

    fun message(step: Int): String? = when (step) {
        0 -> "Interpol needs you. Read the case, then tap BEGIN."
        1 -> "Tap INVESTIGATE and open a venue to question a witness."
        2 -> "That witness pointed to the next city. Open Acme ▸ World Database to identify the place."
        3 -> "Tap DEPART and fly toward that place. A wrong flight burns hours."
        4 -> "Witnesses here describe the thief. Enter a trait in the crime computer."
        5 -> "When one suspect matches, run the computer to issue the warrant."
        6 -> "Now follow the leads to the hideout and search the venues to make the arrest."
        else -> null
    }
}
