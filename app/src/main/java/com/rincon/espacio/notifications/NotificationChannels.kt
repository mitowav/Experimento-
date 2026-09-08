package com.rincon.espacio.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.provider.Settings

/**
 * Tres canales en lugar de uno: en Android 8+ el sonido y la vibración son
 * propiedades del canal, no de la notificación. Separarlos permite respetar la
 * elección "sonido/vibración" de cada recordatorio y, a la vez, deja que el
 * sistema siga siendo el dueño final de esos ajustes.
 */
object NotificationChannels {

    const val FULL = "reminders_full"
    const val VIBRATE_ONLY = "reminders_vibrate"
    const val QUIET = "reminders_quiet"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val full = NotificationChannel(FULL, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos de notas, tareas, estudio y hábitos."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 60, 90, 60)
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audio)
            setShowBadge(true)
        }

        val vibrateOnly = NotificationChannel(VIBRATE_ONLY, "Recordatorios (sólo vibración)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Los mismos avisos, sin sonido."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 60, 90, 60)
            setSound(null, null)
            setShowBadge(true)
        }

        val quiet = NotificationChannel(QUIET, "Recordatorios silenciosos", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisos que aparecen sin sonido ni vibración."
            enableVibration(false)
            setSound(null, null)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(full, vibrateOnly, quiet))
    }

    fun channelFor(sound: Boolean, vibrate: Boolean): String = when {
        sound -> FULL
        vibrate -> VIBRATE_ONLY
        else -> QUIET
    }
}
