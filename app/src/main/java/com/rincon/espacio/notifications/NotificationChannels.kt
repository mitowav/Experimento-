package com.rincon.espacio.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.rincon.espacio.R

/**
 * Tono de aviso.
 *
 * Los cuatro están sintetizados y empaquetados con la app: nada de sonidos
 * genéricos del sistema. Van de lo más discreto (Madera) a lo que de verdad
 * te despierta (Amanecer), porque no todos los avisos merecen la misma
 * insistencia.
 */
enum class ReminderTone(val label: String, val description: String, val rawRes: Int) {
    Campana("Campana", "Cálida y con cola larga", R.raw.tone_campana),
    Gota("Gota", "Corta y redonda", R.raw.tone_gota),
    Amanecer("Amanecer", "Tres notas que suben", R.raw.tone_amanecer),
    Madera("Madera", "Seca y discreta", R.raw.tone_madera);

    companion object {
        fun fromKey(key: String?): ReminderTone = entries.firstOrNull { it.name == key } ?: Campana
    }
}

/**
 * Canales de notificación.
 *
 * En Android 8+ el sonido y la vibración son propiedades del canal, no de la
 * notificación: por eso hay un canal por tono, más uno sólo con vibración y
 * otro silencioso. Es la única forma de que elegir tono funcione de verdad y,
 * a la vez, el sistema siga siendo el dueño final de esos ajustes.
 */
object NotificationChannels {

    const val VIBRATE_ONLY = "reminders_vibrate"
    const val QUIET = "reminders_quiet"

    private fun toneChannelId(tone: ReminderTone) = "reminders_tone_${tone.name.lowercase()}"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = ReminderTone.entries.map { tone ->
            NotificationChannel(
                toneChannelId(tone),
                "Recordatorios · ${tone.label}",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Avisos con el tono ${tone.label}."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 60, 90, 60)
                setSound(
                    Uri.parse("android.resource://${context.packageName}/${tone.rawRes}"),
                    audio,
                )
                setShowBadge(true)
            }
        }.toMutableList()

        channels += NotificationChannel(
            VIBRATE_ONLY,
            "Recordatorios · sólo vibración",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Los mismos avisos, sin sonido."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 60, 90, 60)
            setSound(null, null)
            setShowBadge(true)
        }

        channels += NotificationChannel(
            QUIET,
            "Recordatorios · silenciosos",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos que aparecen sin sonido ni vibración."
            enableVibration(false)
            setSound(null, null)
            setShowBadge(true)
        }

        manager.createNotificationChannels(channels)

        // Canales de versiones anteriores: se retiran para no dejar ajustes
        // huérfanos en la pantalla de notificaciones del sistema.
        runCatching { manager.deleteNotificationChannel("reminders_full") }
    }

    fun channelFor(sound: Boolean, vibrate: Boolean, tone: ReminderTone): String = when {
        sound -> toneChannelId(tone)
        vibrate -> VIBRATE_ONLY
        else -> QUIET
    }
}
