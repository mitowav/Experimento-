package com.rincon.espacio.domain.model

/**
 * Contrato de programación de avisos.
 *
 * Vive en el dominio para que los repositorios no dependan del framework de
 * Android; la implementación real (AlarmManager) queda en la capa de
 * notificaciones y en tests puede sustituirse por una falsa.
 */
interface ReminderScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Long)
    suspend fun rescheduleAll()
    /** Siguiente disparo (ms) según la regla de repetición, o null si ya terminó. */
    fun nextTrigger(reminder: Reminder, afterMillis: Long): Long?
}
