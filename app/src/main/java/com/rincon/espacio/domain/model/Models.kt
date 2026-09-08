package com.rincon.espacio.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Modelos de dominio: Kotlin puro, sin Room ni Compose.
 *
 * Decisión de diseño central: **la nota es el átomo**. Una nota puede quedarse
 * como papel libre en el escritorio o ganar fecha, hora, prioridad, subtareas,
 * recordatorio y vínculo con un objetivo o una asignatura. Así, "convertir una
 * nota en tarea" no crea otro objeto: sólo enciende `isTask`. Es lo que hace
 * que el escritorio y el sistema de productividad sean la misma cosa.
 */

enum class Priority(val label: String) {
    Low("Tranquila"), Normal("Normal"), High("Importante");

    companion object {
        fun fromKey(key: String?): Priority = entries.firstOrNull { it.name == key } ?: Normal
    }
}

data class Note(
    val id: Long = 0L,
    val text: String = "",
    val colorKey: String = "Butter",
    val iconKey: String = "note",
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0,
    val isTask: Boolean = false,
    val done: Boolean = false,
    val pinned: Boolean = false,
    val priority: Priority = Priority.Normal,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val goalId: Long? = null,
    val subjectId: Long? = null,
    val reminderId: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val archived: Boolean = false,
) {
    val title: String
        get() = text.lineSequence().firstOrNull()?.trim().orEmpty()

    val hasSchedule: Boolean get() = dueDate != null
}

data class Subtask(
    val id: Long = 0L,
    val noteId: Long,
    val text: String,
    val done: Boolean = false,
    val position: Int = 0,
)

/** Una nota con sus subtareas ya resueltas. */
data class NoteWithSubtasks(
    val note: Note,
    val subtasks: List<Subtask> = emptyList(),
) {
    val progress: Float
        get() = when {
            subtasks.isEmpty() -> if (note.done) 1f else 0f
            else -> subtasks.count { it.done }.toFloat() / subtasks.size
        }
}

data class Goal(
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val colorKey: String = "Sage",
    val iconKey: String = "target",
    val targetDate: LocalDate? = null,
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val position: Int = 0,
)

data class GoalStep(
    val id: Long = 0L,
    val goalId: Long,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0,
)

data class GoalWithSteps(
    val goal: Goal,
    val steps: List<GoalStep> = emptyList(),
) {
    val progress: Float
        get() = if (steps.isEmpty()) 0f else steps.count { it.done }.toFloat() / steps.size
    val isComplete: Boolean get() = steps.isNotEmpty() && steps.all { it.done }
}

enum class HabitCadence(val label: String) {
    Daily("Cada día"),
    Weekdays("De lunes a viernes"),
    TimesPerWeek("Veces por semana");

    companion object {
        fun fromKey(key: String?): HabitCadence = entries.firstOrNull { it.name == key } ?: Daily
    }
}

data class Habit(
    val id: Long = 0L,
    val title: String,
    val colorKey: String = "Sky",
    val iconKey: String = "leaf",
    val cadence: HabitCadence = HabitCadence.Daily,
    val timesPerWeek: Int = 3,
    val reminderTime: LocalTime? = null,
    val reminderId: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val position: Int = 0,
) {
    fun appliesTo(date: LocalDate): Boolean = when (cadence) {
        HabitCadence.Daily -> true
        HabitCadence.Weekdays -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        HabitCadence.TimesPerWeek -> true
    }
}

data class HabitCheck(
    val id: Long = 0L,
    val habitId: Long,
    val date: LocalDate,
)

data class HabitWithChecks(
    val habit: Habit,
    val checkedDates: Set<LocalDate> = emptySet(),
) {
    fun isDone(date: LocalDate): Boolean = date in checkedDates

    /** Días consecutivos cumplidos contando hacia atrás desde [today]. */
    fun streak(today: LocalDate): Int {
        var count = 0
        var cursor = today
        // Si hoy todavía no está marcado no rompemos la racha: empezamos ayer.
        if (!isDone(cursor)) cursor = cursor.minusDays(1)
        while (true) {
            if (habit.appliesTo(cursor)) {
                if (isDone(cursor)) count++ else break
            }
            cursor = cursor.minusDays(1)
            if (count > 3650) break
        }
        return count
    }

    fun weekProgress(weekStart: LocalDate): Pair<Int, Int> {
        val days = (0..6).map { weekStart.plusDays(it.toLong()) }
        val applicable = days.filter { habit.appliesTo(it) }
        val goal = when (habit.cadence) {
            HabitCadence.TimesPerWeek -> habit.timesPerWeek.coerceIn(1, 7)
            else -> applicable.size
        }
        return days.count { isDone(it) } to goal
    }
}

data class Subject(
    val id: Long = 0L,
    val name: String,
    val colorKey: String = "Sky",
    val iconKey: String = "book",
    val teacher: String = "",
    val archived: Boolean = false,
    val position: Int = 0,
)

data class Exam(
    val id: Long = 0L,
    val subjectId: Long,
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val readiness: Int = 0,
    val reminderId: Long? = null,
)

data class StudySession(
    val id: Long = 0L,
    val subjectId: Long?,
    val date: LocalDate,
    val startTime: LocalTime,
    val minutes: Int,
    val note: String = "",
    val completed: Boolean = false,
    val reminderId: Long? = null,
)

data class SubjectDetail(
    val subject: Subject,
    val exams: List<Exam> = emptyList(),
    val tasks: List<Note> = emptyList(),
    val minutesThisWeek: Int = 0,
) {
    val taskProgress: Float
        get() = if (tasks.isEmpty()) 0f else tasks.count { it.done }.toFloat() / tasks.size
    val nextExam: Exam? get() = exams.minByOrNull { it.date }
}

data class CalendarEvent(
    val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val colorKey: String = "Lavender",
    val iconKey: String = "calendar",
    val note: String = "",
    val reminderId: Long? = null,
)

enum class ReminderTarget { Note, Exam, Habit, Event, StudySession, Goal }

enum class RepeatRule(val label: String) {
    Once("Una vez"),
    Daily("Cada día"),
    Weekdays("De lunes a viernes"),
    Weekly("Cada semana"),
    Monthly("Cada mes");

    companion object {
        fun fromKey(key: String?): RepeatRule = entries.firstOrNull { it.name == key } ?: Once
    }
}

data class Reminder(
    val id: Long = 0L,
    val targetType: ReminderTarget,
    val targetId: Long,
    val title: String,
    val body: String = "",
    val iconKey: String = "bell",
    val triggerAtMillis: Long,
    val repeat: RepeatRule = RepeatRule.Once,
    val leadMinutes: Int = 0,
    val enabled: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
)

/** Elemento unificado que pinta la pantalla "Hoy" y el calendario. */
sealed interface DayItem {
    val sortTime: LocalTime?
    val id: String

    data class TaskItem(val note: Note, val subtaskProgress: Float) : DayItem {
        override val sortTime: LocalTime? get() = note.dueTime
        override val id: String get() = "note-${note.id}"
    }

    data class EventItem(val event: CalendarEvent) : DayItem {
        override val sortTime: LocalTime? get() = event.startTime
        override val id: String get() = "event-${event.id}"
    }

    data class ExamItem(val exam: Exam, val subject: Subject?) : DayItem {
        override val sortTime: LocalTime? get() = exam.time
        override val id: String get() = "exam-${exam.id}"
    }

    data class StudyItem(val session: StudySession, val subject: Subject?) : DayItem {
        override val sortTime: LocalTime get() = session.startTime
        override val id: String get() = "study-${session.id}"
    }

    data class HabitItem(val habit: Habit, val done: Boolean, val streak: Int) : DayItem {
        override val sortTime: LocalTime? get() = habit.reminderTime
        override val id: String get() = "habit-${habit.id}"
    }
}
