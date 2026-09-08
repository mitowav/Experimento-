package com.rincon.espacio.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rincon.espacio.MainActivity
import com.rincon.espacio.R
import com.rincon.espacio.di.AppContainer
import com.rincon.espacio.di.launchSafely
import com.rincon.espacio.domain.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Recibe el disparo de una alarma, muestra el aviso y programa la siguiente. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pending = goAsync()
        val container = AppContainer.get(context)
        container.applicationScope.launchSafely(pending) {
            val reminder = withContext(Dispatchers.IO) { container.reminderRepository.byId(reminderId) }
            if (reminder != null && reminder.enabled) {
                val tone = withContext(Dispatchers.IO) {
                    ReminderTone.fromKey(container.preferences.settings.first().reminderTone.name)
                }
                notify(context, reminder, tone)
                withContext(Dispatchers.IO) {
                    container.reminderRepository.advanceAfterFire(reminderId, System.currentTimeMillis())
                }
            }
        }
    }

    private fun notify(context: Context, reminder: Reminder, tone: ReminderTone) {
        NotificationChannels.ensure(context)

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
        if (!granted) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_REMINDER_TARGET_TYPE, reminder.targetType.name)
            putExtra(MainActivity.EXTRA_REMINDER_TARGET_ID, reminder.targetId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannels.channelFor(reminder.sound, reminder.vibrate, tone),
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.body.ifBlank { "Es el momento." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.body.ifBlank { "Es el momento." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.rincon.espacio.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
