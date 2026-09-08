package com.rincon.espacio.data.repo

import com.rincon.espacio.data.local.dao.ReminderDao
import com.rincon.espacio.domain.model.Reminder
import com.rincon.espacio.domain.model.ReminderScheduler
import com.rincon.espacio.domain.model.ReminderTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Única puerta de entrada a los recordatorios: guardar en Room y programar en
 * el sistema siempre ocurren juntos, de modo que la base de datos y las alarmas
 * no puedan quedar desincronizadas.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: ReminderScheduler,
) {
    fun observeAll(): Flow<List<Reminder>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun byId(id: Long): Reminder? = dao.byId(id)?.toDomain()

    suspend fun save(reminder: Reminder): Long {
        val id = dao.insert(reminder.toEntity())
        val stored = reminder.copy(id = if (reminder.id == 0L) id else reminder.id)
        if (stored.enabled) scheduler.schedule(stored) else scheduler.cancel(stored.id)
        return stored.id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val current = dao.byId(id)?.toDomain() ?: return
        val updated = current.copy(enabled = enabled)
        dao.update(updated.toEntity())
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.deleteById(id)
    }

    suspend fun deleteForTarget(target: ReminderTarget, targetId: Long) {
        dao.allEnabled()
            .filter { it.targetType == target.name && it.targetId == targetId }
            .forEach { scheduler.cancel(it.id) }
        dao.deleteForTarget(target.name, targetId)
    }

    /** Avanza un recordatorio repetitivo tras dispararse, o lo apaga si era único. */
    suspend fun advanceAfterFire(id: Long, firedAtMillis: Long) {
        val reminder = dao.byId(id)?.toDomain() ?: return
        val next = scheduler.nextTrigger(reminder, firedAtMillis)
        if (next == null) {
            dao.update(reminder.copy(enabled = false).toEntity())
        } else {
            val updated = reminder.copy(triggerAtMillis = next)
            dao.update(updated.toEntity())
            scheduler.schedule(updated)
        }
    }

    suspend fun rescheduleAll() = scheduler.rescheduleAll()
}
