package com.acme.clara

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.notify.Reminders
import com.acme.clara.notify.WelcomeBackWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The reminder is on by default, and the notification actually posts (verified via Robolectric,
 *  so the "needs a device" part is covered up to the OS boundary). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NotifyTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun remindersAreOnByDefaultAndCanBeTurnedOff() {
        assertTrue("on by default", Reminders.enabled(ctx))
        Reminders.setEnabled(ctx, false)
        assertFalse(Reminders.enabled(ctx))
        Reminders.setEnabled(ctx, true)
        assertTrue(Reminders.enabled(ctx))
    }

    @Test fun theWelcomeBackNotificationPostsWithTheRightWords() {
        val posted = WelcomeBackWorker.post(ctx)
        assertTrue("notification was posted", posted)

        val manager = ctx.getSystemService(NotificationManager::class.java)
        val shown = shadowOf(manager).getNotification(null, WelcomeBackWorker.NOTIFICATION_ID)
        assertEquals(1, shadowOf(manager).size())
        assertEquals(
            WelcomeBackWorker.TITLE,
            shown.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString(),
        )
    }

    @Test @Config(sdk = [33])
    fun postingIsSkippedWhenNotificationsArentPermitted() {
        // Robolectric denies POST_NOTIFICATIONS by default on API 33+, so posting no-ops.
        assertFalse(WelcomeBackWorker.post(ctx))
    }
}
