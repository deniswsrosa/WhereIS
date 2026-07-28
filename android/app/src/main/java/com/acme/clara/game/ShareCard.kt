package com.acme.clara.game

/** The facts a result card brags about — deliberately holds no city, route, or suspect name. */
data class ShareResult(
    val dateLabel: String,
    val rankName: String,
    val solved: Boolean,
    val hops: Int,
    val wrongFlights: Int,
    val hoursToSpare: Int,
    val hintFree: Boolean,
)

/**
 * Renders a spoiler-free, screenshot-ready result card in the Wordle mould: abstract
 * glyphs and stats only, so it's brag-worthy without leaking the answer or the route.
 */
object ShareCard {
    fun render(r: ShareResult): String {
        val planes = "✈".repeat(r.hops.coerceIn(0, 12))
        val outcome = if (r.solved) "solved" else "escaped"
        val hints = if (r.hintFree) "🔎 no hints" else "🔎 used a hint"
        return buildString {
            appendLine("WhereIS · ${r.dateLabel}")
            appendLine("🎖 ${r.rankName} — $outcome")
            appendLine("$planes ${r.hops} hops · ${r.wrongFlights} wrong")
            append("⏱ ${r.hoursToSpare}h to spare · $hints")
        }
    }
}
