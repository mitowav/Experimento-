package com.rincon.espacio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rincon.espacio.data.local.entity.EventEntity
import com.rincon.espacio.data.local.entity.ExamEntity
import com.rincon.espacio.data.local.entity.GoalEntity
import com.rincon.espacio.data.local.entity.GoalStepEntity
import com.rincon.espacio.data.local.entity.HabitCheckEntity
import com.rincon.espacio.data.local.entity.HabitEntity
import com.rincon.espacio.data.local.entity.NoteEntity
import com.rincon.espacio.data.local.entity.ReminderEntity
import com.rincon.espacio.data.local.entity.StudySessionEntity
import com.rincon.espacio.data.local.entity.SubjectEntity
import com.rincon.espacio.data.local.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    suspend fun allNotes(): List<NoteEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun allSubtasks(): List<SubtaskEntity>

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY zIndex ASC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE archived = 0 AND isTask = 1 AND dueDate = :epochDay ORDER BY dueTime IS NULL, dueTime ASC")
    fun observeForDate(epochDay: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE archived = 0 AND isTask = 1 AND done = 0 AND (dueDate IS NULL OR dueDate <= :epochDay) ORDER BY dueDate IS NULL, dueDate ASC, dueTime IS NULL, dueTime ASC")
    fun observeOpenUpTo(epochDay: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE archived = 0 AND subjectId = :subjectId ORDER BY done ASC, dueDate IS NULL, dueDate ASC")
    fun observeForSubject(subjectId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE archived = 0 AND dueDate BETWEEN :from AND :to")
    fun observeBetween(from: Long, to: Long): Flow<List<NoteEntity>>

    @Query("SELECT COALESCE(MAX(zIndex), 0) FROM notes")
    suspend fun maxZ(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET x = :x, y = :y, rotation = :rotation, zIndex = :z, updatedAt = :now WHERE id = :id")
    suspend fun updatePlacement(id: Long, x: Float, y: Float, rotation: Float, z: Int, now: Long)

    @Query("UPDATE notes SET done = :done, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM subtasks WHERE noteId = :noteId ORDER BY position ASC")
    fun observeSubtasks(noteId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks ORDER BY position ASC")
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE noteId = :noteId")
    suspend fun deleteSubtasksOf(noteId: Long)

    @Transaction
    suspend fun replaceSubtasks(noteId: Long, items: List<SubtaskEntity>) {
        deleteSubtasksOf(noteId)
        items.forEachIndexed { index, item ->
            insertSubtask(item.copy(id = 0L, noteId = noteId, position = index))
        }
    }
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    suspend fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal_steps")
    suspend fun allSteps(): List<GoalStepEntity>

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY position ASC, createdAt ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal_steps ORDER BY position ASC")
    fun observeSteps(): Flow<List<GoalStepEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun byId(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: GoalStepEntity): Long

    @Update
    suspend fun updateStep(step: GoalStepEntity)

    @Query("DELETE FROM goal_steps WHERE id = :id")
    suspend fun deleteStep(id: Long)

    @Query("UPDATE goal_steps SET done = :done WHERE id = :id")
    suspend fun setStepDone(id: Long, done: Boolean)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    suspend fun allHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_checks")
    suspend fun allChecks(): List<HabitCheckEntity>

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY position ASC, createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_checks WHERE date >= :from")
    fun observeChecksSince(from: Long): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun check(entry: HabitCheckEntity): Long

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun uncheck(habitId: Long, date: Long)
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM subjects")
    suspend fun allSubjects(): List<SubjectEntity>

    @Query("SELECT * FROM exams")
    suspend fun allExams(): List<ExamEntity>

    @Query("SELECT * FROM study_sessions")
    suspend fun allSessions(): List<StudySessionEntity>

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("DELETE FROM study_sessions")
    suspend fun clearSessions()

    @Query("SELECT * FROM subjects WHERE archived = 0 ORDER BY position ASC, name ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun observeExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM study_sessions WHERE date BETWEEN :from AND :to ORDER BY date ASC, startTime ASC")
    fun observeSessionsBetween(from: Long, to: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY date DESC LIMIT 200")
    fun observeRecentSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun subjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: Long)

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun examById(id: Long): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): StudySessionEntity?
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    suspend fun allEvents(): List<EventEntity>

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    @Query("SELECT * FROM events WHERE date BETWEEN :from AND :to ORDER BY startTime IS NULL, startTime ASC")
    fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun byId(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    suspend fun allReminders(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun allEnabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminders WHERE targetType = :type AND targetId = :targetId")
    suspend fun deleteForTarget(type: String, targetId: Long)
}
