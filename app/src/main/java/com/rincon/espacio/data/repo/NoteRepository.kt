package com.rincon.espacio.data.repo

import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.local.dao.NoteDao
import com.rincon.espacio.data.local.entity.SubtaskEntity
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.ReminderTarget
import com.rincon.espacio.domain.model.RepeatRule
import com.rincon.espacio.domain.model.Subtask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class NoteRepository(
    private val dao: NoteDao,
    private val reminders: ReminderRepository,
) {

    /** Todas las notas del escritorio, con sus subtareas, ordenadas por capa. */
    fun observeDesk(): Flow<List<NoteWithSubtasks>> =
        combine(dao.observeAll(), dao.observeAllSubtasks()) { notes, subtasks ->
            val grouped = subtasks.groupBy { it.noteId }
            notes.map { entity ->
                NoteWithSubtasks(
                    note = entity.toDomain(),
                    subtasks = grouped[entity.id].orEmpty().map { it.toDomain() },
                )
            }
        }

    fun observeForDate(date: LocalDate): Flow<List<Note>> =
        dao.observeForDate(Dates.epochDay(date)).map { list -> list.map { it.toDomain() } }

    fun observeOpenUpTo(date: LocalDate): Flow<List<Note>> =
        dao.observeOpenUpTo(Dates.epochDay(date)).map { list -> list.map { it.toDomain() } }

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Note>> =
        dao.observeBetween(Dates.epochDay(from), Dates.epochDay(to)).map { list -> list.map { it.toDomain() } }

    fun observeForSubject(subjectId: Long): Flow<List<Note>> =
        dao.observeForSubject(subjectId).map { list -> list.map { it.toDomain() } }

    suspend fun byId(id: Long): Note? = dao.byId(id)?.toDomain()

    suspend fun nextZ(): Int = dao.maxZ() + 1

    suspend fun create(note: Note): Long {
        val now = System.currentTimeMillis()
        val z = if (note.zIndex == 0) nextZ() else note.zIndex
        return dao.insert(note.copy(createdAt = now, updatedAt = now, zIndex = z).toEntity())
    }

    suspend fun update(note: Note) {
        dao.update(note.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    /**
     * Persistencia del gesto de arrastre. Se guarda sólo la colocación para no
     * reescribir texto ni recordatorios en cada `onDragEnd`.
     */
    suspend fun savePlacement(id: Long, x: Float, y: Float, rotation: Float, zIndex: Int) {
        dao.updatePlacement(id, x, y, rotation, zIndex, System.currentTimeMillis())
    }

    /**
     * Marcar como hecha.
     *
     * Una tarea que se repite no se queda tachada: salta a su siguiente fecha
     * y vuelve pendiente, arrastrando consigo su aviso. Es lo que uno espera de
     * "beber agua" o "sacar la basura" — que reaparezca, no que desaparezca.
     *
     * Devuelve `true` si la tarea ha viajado en vez de completarse.
     */
    suspend fun setDone(id: Long, done: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val current = dao.byId(id)?.toDomain()

        if (done && current != null && current.repeat != RepeatRule.Once) {
            val from = current.dueDate ?: Dates.today()
            val next = current.repeat.nextDate(from)
            if (next != null) {
                dao.update(current.copy(dueDate = next, done = false, updatedAt = now).toEntity())
                moveReminderTo(current, next)
                return true
            }
        }

        dao.setDone(id, done, now)
        return false
    }

    /** El aviso viaja con la tarea: si no, sonaría por una fecha que ya pasó. */
    private suspend fun moveReminderTo(note: Note, date: java.time.LocalDate) {
        val reminderId = note.reminderId ?: return
        val time = note.dueTime ?: return
        val reminder = reminders.byId(reminderId) ?: return
        reminders.save(
            reminder.copy(
                triggerAtMillis = Dates.millisOf(date, time) - reminder.leadMinutes * 60_000L,
                enabled = true,
            )
        )
    }

    suspend fun delete(id: Long) {
        reminders.deleteForTarget(ReminderTarget.Note, id)
        dao.deleteById(id)
    }

    suspend fun replaceSubtasks(noteId: Long, texts: List<Subtask>) {
        dao.replaceSubtasks(
            noteId,
            texts.map { SubtaskEntity(0L, noteId, it.text, it.done, it.position) },
        )
    }

    suspend fun setSubtaskDone(subtask: Subtask, done: Boolean) {
        dao.updateSubtask(subtask.copy(done = done).toEntity())
    }
}
