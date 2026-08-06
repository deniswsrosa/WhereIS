package com.acme.clara.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.acme.clara.R

/** Posts the "come back, a hint is waiting" notification. Skips quietly if notifications are off. */
class WelcomeBackWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        post(applicationContext)
        return Result.success()
    }

    companion object {
        const val CHANNEL = "cold_cases"
        const val NOTIFICATION_ID = 1001
        const val TITLE = "A lead has gone cold"
        const val TEXT = "Come back, detective — a fresh hint is waiting."

        /** Build and post the notification. Returns false (a no-op) when notifications aren't permitted.
         *  Extracted so it can be unit-tested without a device. */
        fun post(ctx: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false   // no permission — nothing to do
            }
            // The worker can fire without the Activity ever running this process — load the
            // player's chosen language before rendering the notification text.
            com.acme.clara.i18n.Strings.ensureInit(ctx)

            val manager = NotificationManagerCompat.from(ctx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL, com.acme.clara.i18n.Strings.ui("Cold cases"),
                        NotificationManager.IMPORTANCE_DEFAULT)
                )
            }

            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            val pending = PendingIntent.getActivity(
                ctx, 0, launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(com.acme.clara.i18n.Strings.ui(TITLE))
                .setContentText(com.acme.clara.i18n.Strings.ui(TEXT))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
            return true
        }
    }
}
