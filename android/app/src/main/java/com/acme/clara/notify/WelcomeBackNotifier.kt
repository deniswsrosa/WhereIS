package com.acme.clara.notify

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.acme.clara.game.WelcomeBack
import java.util.concurrent.TimeUnit

/**
 * Schedules the re-engagement notification: when the app is backgrounded, a one-shot job is
 * queued for a few days out. Opening the app cancels it. The notification is the pull; the
 * free hint waiting inside (see [WelcomeBack]) is the reward.
 */
object WelcomeBackNotifier {
    private const val WORK = "welcome-back-notification"

    fun schedule(context: Context) {
        val request = OneTimeWorkRequest.Builder(WelcomeBackWorker::class.java)
            .setInitialDelay(WelcomeBack.THRESHOLD_DAYS.toLong(), TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
}
