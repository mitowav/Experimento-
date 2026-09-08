package com.rincon.espacio.data.repo

import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.local.dao.NoteDao
import com.rincon.espacio.data.local.entity.SubtaskEntity
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.ReminderTarget
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

    suspend fun setDone(id: Long, done: Boolean) {
        dao.setDone(id, done, System.currentTimeMillis())
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
