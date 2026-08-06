package com.acme.clara.ui

import android.content.Context
import android.content.Intent
import com.acme.clara.data.GameData
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ShareCard
import com.acme.clara.game.ShareResult

/**
 * Builds the spoiler-free result card from the current state and hands it to the OS share
 * sheet. No route, city, or suspect leaks — only rank, hop count, time to spare, and hints.
 */
fun shareResult(context: Context, vm: ClaraViewModel) {
    val s = vm.s
    val card = ShareCard.render(
        ShareResult(
            dateLabel = com.acme.clara.i18n.Strings.ui("Case #{0}", s.casesSolved),
            rankName = com.acme.clara.i18n.Strings.label("rank",
                GameData.ranks.getOrElse(s.rankIndex) { "Rookie" }),
            solved = s.won,
            hops = (s.route.size - 1).coerceAtLeast(0),
            wrongFlights = s.wrongFlights,
            hoursToSpare = vm.hoursLeft(),
            hintFree = s.hintsUsed == 0,
        )
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, card)
    }
    context.startActivity(
        Intent.createChooser(send, com.acme.clara.i18n.Strings.ui("Share your result")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
