package com.acme.clara.game

/**
 * The welcome-back reward: after a few days away, returning to a saved career banks one free
 * hint — the carrot the re-engagement notification points at. Pure logic so it's easy to test.
 */
object WelcomeBack {
    const val THRESHOLD_DAYS = 3
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Whole days between two epoch-millis instants (never negative). */
    fun daysAway(lastPlayed: Long, now: Long): Long =
        if (now <= lastPlayed) 0 else (now - lastPlayed) / DAY_MS

    /** True when a returning player has been away long enough to earn a free hint. */
    fun grantsHint(lastPlayed: Long, now: Long, thresholdDays: Int = THRESHOLD_DAYS): Boolean =
        daysAway(lastPlayed, now) >= thresholdDays
}
