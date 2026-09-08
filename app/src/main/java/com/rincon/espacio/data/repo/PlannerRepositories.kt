package com.rincon.espacio.data.repo

import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.local.dao.EventDao
import com.rincon.espacio.data.local.dao.GoalDao
import com.rincon.espacio.data.local.dao.HabitDao
import com.rincon.espacio.data.local.dao.StudyDao
import com.rincon.espacio.data.local.entity.HabitCheckEntity
import com.rincon.espacio.domain.model.CalendarEvent
import com.rincon.espacio.domain.model.Exam
import com.rincon.espacio.domain.model.Goal
import com.rincon.espacio.domain.model.GoalStep
import com.rincon.espacio.domain.model.GoalWithSteps
import com.rincon.espacio.domain.model.Habit
import com.rincon.espacio.domain.model.HabitWithChecks
import com.rincon.espacio.domain.model.ReminderTarget
import com.rincon.espacio.domain.model.StudySession
import com.rincon.espacio.domain.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class GoalRepository(private val dao: GoalDao) {

    fun observeGoals(): Flow<List<GoalWithSteps>> =
        combine(dao.observeGoals(), dao.observeSteps()) { goals, steps ->
            val grouped = steps.groupBy { it.goalId }
            goals.map { g ->
                GoalWithSteps(g.toDomain(), grouped[g.id].orEmpty().map { it.toDomain() })
            }
        }

    suspend fun save(goal: Goal): Long =
        dao.insert(goal.copy(createdAt = if (goal.createdAt == 0L) System.currentTimeMillis() else goal.createdAt).toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun addStep(goalId: Long, title: String, position: Int) {
        dao.insertStep(GoalStep(goalId = goalId, title = title, position = position).toEntity())
    }

    suspend fun setStepDone(stepId: Long, done: Boolean) = dao.setStepDone(stepId, done)

    suspend fun deleteStep(stepId: Long) = dao.deleteStep(stepId)

    suspend fun updateStep(step: GoalStep) = dao.updateStep(step.toEntity())
}

class HabitRepository(
    private val dao: HabitDao,
    private val reminders: ReminderRepository,
) {
    /** Se observa una ventana de un año: suficiente para rachas y suave en memoria. */
    fun observeHabits(from: LocalDate = LocalDate.now().minusDays(370)): Flow<List<HabitWithChecks>> =
        combine(dao.observeHabits(), dao.observeChecksSince(Dates.epochDay(from))) { habits, checks ->
            val grouped = checks.groupBy { it.habitId }
            habits.map { h ->
                HabitWithChecks(
                    habit = h.toDomain(),
                    checkedDates = grouped[h.id].orEmpty().map { Dates.fromEpochDay(it.date) }.toSet(),
                )
            }
        }

    suspend fun save(habit: Habit): Long =
        dao.insert(habit.copy(createdAt = if (habit.createdAt == 0L) System.currentTimeMillis() else habit.createdAt).toEntity())

    suspend fun delete(id: Long) {
        reminders.deleteForTarget(ReminderTarget.Habit, id)
        dao.deleteById(id)
    }

    suspend fun setChecked(habitId: Long, date: LocalDate, checked: Boolean) {
        if (checked) dao.check(HabitCheckEntity(habitId = habitId, date = Dates.epochDay(date)))
        else dao.uncheck(habitId, Dates.epochDay(date))
    }
}

class StudyRepository(
    private val dao: StudyDao,
    private val reminders: ReminderRepository,
) {
    fun observeSubjects(): Flow<List<Subject>> =
        dao.observeSubjects().map { list -> list.map { it.toDomain() } }

    fun observeExams(): Flow<List<Exam>> =
        dao.observeExams().map { list -> list.map { it.toDomain() } }

    fun observeSessionsBetween(from: LocalDate, to: LocalDate): Flow<List<StudySession>> =
        dao.observeSessionsBetween(Dates.epochDay(from), Dates.epochDay(to))
            .map { list -> list.map { it.toDomain() } }

    fun observeRecentSessions(): Flow<List<StudySession>> =
        dao.observeRecentSessions().map { list -> list.map { it.toDomain() } }

    suspend fun saveSubject(subject: Subject): Long = dao.insertSubject(subject.toEntity())

    suspend fun deleteSubject(id: Long) = dao.deleteSubject(id)

    suspend fun saveExam(exam: Exam): Long = dao.insertExam(exam.toEntity())

    suspend fun deleteExam(id: Long) {
        reminders.deleteForTarget(ReminderTarget.Exam, id)
        dao.deleteExam(id)
    }

    suspend fun examById(id: Long): Exam? = dao.examById(id)?.toDomain()

    suspend fun saveSession(session: StudySession): Long = dao.insertSession(session.toEntity())

    suspend fun deleteSession(id: Long) {
        reminders.deleteForTarget(ReminderTarget.StudySession, id)
        dao.deleteSession(id)
    }

    suspend fun setSessionCompleted(id: Long, completed: Boolean) {
        val current = dao.sessionById(id) ?: return
        dao.updateSession(current.copy(completed = completed))
    }
}

class EventRepository(
    private val dao: EventDao,
    private val reminders: ReminderRepository,
) {
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<CalendarEvent>> =
        dao.observeBetween(Dates.epochDay(from), Dates.epochDay(to))
            .map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<CalendarEvent>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(event: CalendarEvent): Long = dao.insert(event.toEntity())

    suspend fun byId(id: Long): CalendarEvent? = dao.byId(id)?.toDomain()

    suspend fun move(id: Long, date: LocalDate, start: java.time.LocalTime?) {
        val current = dao.byId(id)?.toDomain() ?: return
        val duration = current.startTime?.let { s -> current.endTime?.let { e -> java.time.Duration.between(s, e) } }
        val newEnd = if (start != null && duration != null) start.plus(duration) else current.endTime
        dao.update(current.copy(date = date, startTime = start, endTime = newEnd).toEntity())
    }

    suspend fun delete(id: Long) {
        reminders.deleteForTarget(ReminderTarget.Event, id)
        dao.deleteById(id)
    }
}
