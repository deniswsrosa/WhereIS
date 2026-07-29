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
        val ctx = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()   // no permission — nothing to do
        }

        val manager = NotificationManagerCompat.from(ctx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Cold cases", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pending = PendingIntent.getActivity(
            ctx, 0, launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("A lead has gone cold")
            .setContentText("Come back, detective — a fresh hint is waiting.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "cold_cases"
        private const val NOTIFICATION_ID = 1001
    }
}
