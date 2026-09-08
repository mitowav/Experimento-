package com.rincon.espacio.domain.usecase

import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.repo.EventRepository
import com.rincon.espacio.data.repo.HabitRepository
import com.rincon.espacio.data.repo.NoteRepository
import com.rincon.espacio.data.repo.StudyRepository
import com.rincon.espacio.domain.model.DayItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Lo que hay que hacer un día concreto, ya mezclado y ordenado. */
data class DayPlan(
    val date: LocalDate,
    val items: List<DayItem> = emptyList(),
) {
    val tasks: List<DayItem.TaskItem> get() = items.filterIsInstance<DayItem.TaskItem>()
    val habits: List<DayItem.HabitItem> get() = items.filterIsInstance<DayItem.HabitItem>()

    val doneCount: Int
        get() = tasks.count { it.note.done } + habits.count { it.done }

    val totalCount: Int
        get() = tasks.size + habits.size

    val progress: Float
        get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount

    val upcoming: List<DayItem>
        get() = items.filter { it !is DayItem.HabitItem }
}

/**
 * Una sola fuente de verdad para "el día".
 *
 * Home, Hoy y el calendario muestran lo mismo desde sitios distintos; si cada
 * pantalla lo calculara a su manera acabarían discrepando. Aquí se combinan
 * tareas, eventos, exámenes, sesiones de estudio y hábitos en una lista
 * ordenada por hora.
 */
class ObserveDay(
    private val notes: NoteRepository,
    private val events: EventRepository,
    private val study: StudyRepository,
    private val habits: HabitRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate): Flow<DayPlan> = combine(
        notes.observeDesk(),
        events.observeBetween(date, date),
        study.observeExams(),
        study.observeSessionsBetween(date, date),
        combine(habits.observeHabits(), study.observeSubjects()) { h, s -> h to s },
    ) { desk, dayEvents, exams, sessions, (habitList, subjects) ->

        val subjectsById = subjects.associateBy { it.id }

        val taskItems = desk
            .filter { it.note.isTask && it.note.dueDate == date }
            .map { DayItem.TaskItem(it.note, it.progress) }

        val eventItems = dayEvents.map { DayItem.EventItem(it) }

        val examItems = exams
            .filter { it.date == date }
            .map { DayItem.ExamItem(it, subjectsById[it.subjectId]) }

        val studyItems = sessions.map { DayItem.StudyItem(it, it.subjectId?.let(subjectsById::get)) }

        val habitItems = habitList
            .filter { it.habit.appliesTo(date) }
            .map { DayItem.HabitItem(it.habit, it.isDone(date), it.streak(Dates.today())) }

        val ordered = (taskItems + eventItems + examItems + studyItems)
            .sortedWith(compareBy(nullsLast<java.time.LocalTime>()) { it.sortTime })

        DayPlan(date = date, items = ordered + habitItems)
    }
}
