package com.rincon.espacio.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.rincon.espacio.data.local.dao.ReminderDao
import com.rincon.espacio.data.repo.toDomain
import com.rincon.espacio.domain.model.Reminder
import com.rincon.espacio.domain.model.ReminderScheduler
import java.time.ZoneId

/**
 * Programación de avisos con AlarmManager.
 *
 * Android moderno restringe las alarmas exactas: desde Android 12 hacen falta
 * permisos y desde Android 14 `SCHEDULE_EXACT_ALARM` no se concede sola. Por eso
 * la estrategia es honesta en lugar de frágil:
 *
 *  - si el sistema nos permite alarmas exactas, usamos
 *    `setExactAndAllowWhileIdle` (llega puntual incluso en Doze);
 *  - si no, degradamos a `setWindow` con una ventana de 5 minutos, que sigue
 *    funcionando sin permisos especiales;
 *  - las repeticiones no se delegan a `setRepeating` (inexacto y frágil): tras
 *    cada disparo se calcula y se programa la siguiente ocurrencia.
 *
 * Además, todas las alarmas se reprograman tras reiniciar el teléfono, cambiar
 * la hora o actualizar la app (ver [BootReceiver]).
 */
class AlarmReminderScheduler(
    private val context: Context,
    private val reminderDao: ReminderDao,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ReminderScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun pendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            data = android.net.Uri.parse("rincon://reminder/$reminderId")
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun schedule(reminder: Reminder) {
        val manager = alarmManager ?: return
        if (!reminder.enabled) return
        val now = System.currentTimeMillis()
        val target = if (reminder.triggerAtMillis > now) {
            reminder.triggerAtMillis
        } else {
            nextTrigger(reminder, now) ?: return
        }
        val pi = pendingIntent(reminder.id)
        runCatching {
            if (canScheduleExact()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi)
            } else {
                manager.setWindow(AlarmManager.RTC_WAKEUP, target, WINDOW_MS, pi)
            }
        }.onFailure { Log.w(TAG, "No se pudo programar el aviso ${reminder.id}", it) }
    }

    override fun cancel(reminderId: Long) {
        alarmManager?.cancel(pendingIntent(reminderId))
    }

    override suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        reminderDao.allEnabled().forEach { entity ->
            val reminder = entity.toDomain()
            val next = if (reminder.triggerAtMillis > now) {
                reminder.triggerAtMillis
            } else {
                nextTrigger(reminder, now)
            }
            if (next == null) {
                reminderDao.update(entity.copy(enabled = false))
            } else {
                if (next != reminder.triggerAtMillis) {
                    reminderDao.update(entity.copy(triggerAtMillis = next))
                }
                schedule(reminder.copy(triggerAtMillis = next))
            }
        }
    }

    override fun nextTrigger(reminder: Reminder, afterMillis: Long): Long? =
        RepeatMath.next(reminder.triggerAtMillis, reminder.repeat, afterMillis, zone)

    private companion object {
        const val TAG = "RinconAlarms"
        const val WINDOW_MS = 5 * 60 * 1000L
    }
}
