package com.rincon.espacio.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.prefs.PreferencesRepository
import com.rincon.espacio.data.repo.EventRepository
import com.rincon.espacio.data.repo.GoalRepository
import com.rincon.espacio.data.repo.HabitRepository
import com.rincon.espacio.data.repo.NoteRepository
import com.rincon.espacio.data.repo.ReminderRepository
import com.rincon.espacio.data.repo.StudyRepository
import com.rincon.espacio.domain.model.CalendarEvent
import com.rincon.espacio.domain.model.Exam
import com.rincon.espacio.domain.model.Goal
import com.rincon.espacio.domain.model.GoalWithSteps
import com.rincon.espacio.domain.model.Habit
import com.rincon.espacio.domain.model.HabitWithChecks
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.Reminder
import com.rincon.espacio.domain.model.ReminderTarget
import com.rincon.espacio.domain.model.RepeatRule
import com.rincon.espacio.domain.model.StudySession
import com.rincon.espacio.domain.model.Subject
import com.rincon.espacio.domain.model.SubjectDetail
import com.rincon.espacio.domain.usecase.DayPlan
import com.rincon.espacio.domain.usecase.ObserveDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Pantalla "Hoy": un día concreto, navegable hacia delante y hacia atrás. */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    observeDay: ObserveDay,
    private val notes: NoteRepository,
    private val habits: HabitRepository,
    private val study: StudyRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(Dates.today())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    val plan: StateFlow<DayPlan> = _date
        .flatMapLatest { observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayPlan(Dates.today()))

    fun goTo(date: LocalDate) { _date.value = date }
    fun shiftDays(delta: Long) { _date.value = _date.value.plusDays(delta) }

    fun toggleTask(id: Long, done: Boolean) = viewModelScope.launch { notes.setDone(id, done) }

    fun toggleHabit(habitId: Long, date: LocalDate, checked: Boolean) =
        viewModelScope.launch { habits.setChecked(habitId, date, checked) }

    fun toggleStudySession(id: Long, completed: Boolean) =
        viewModelScope.launch { study.setSessionCompleted(id, completed) }
}

data class HomeUiState(
    val greeting: String = "",
    val greetingIcon: String = "sun",
    val displayName: String = "",
    val date: LocalDate = Dates.today(),
    val plan: DayPlan = DayPlan(Dates.today()),
    val recentNotes: List<NoteWithSubtasks> = emptyList(),
    val goals: List<GoalWithSteps> = emptyList(),
    val overdue: List<Note> = emptyList(),
)

/** Pantalla de inicio: el resumen amable del día. */
class HomeViewModel(
    observeDay: ObserveDay,
    notes: NoteRepository,
    goals: GoalRepository,
    preferences: PreferencesRepository,
) : ViewModel() {

    private val today = Dates.today()

    val state: StateFlow<HomeUiState> = combine(
        observeDay(today),
        notes.observeDesk(),
        goals.observeGoals(),
        notes.observeOpenUpTo(today.minusDays(1)),
        preferences.settings.map { it.displayName },
    ) { plan, desk, goalList, overdue, name ->
        HomeUiState(
            greeting = Dates.greeting(),
            greetingIcon = Dates.greetingIcon(),
            displayName = name,
            date = today,
            plan = plan,
            recentNotes = desk.sortedByDescending { it.note.updatedAt }.take(6),
            goals = goalList.take(3),
            overdue = overdue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

enum class CalendarMode(val label: String) { Day("Día"), Week("Semana"), Month("Mes") }

data class CalendarUiState(
    val mode: CalendarMode = CalendarMode.Month,
    val selected: LocalDate = Dates.today(),
    val month: YearMonth = YearMonth.from(Dates.today()),
    val events: List<CalendarEvent> = emptyList(),
    val tasks: List<Note> = emptyList(),
    val plan: DayPlan = DayPlan(Dates.today()),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    observeDay: ObserveDay,
    private val notes: NoteRepository,
    private val events: EventRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {

    private val _mode = MutableStateFlow(CalendarMode.Month)
    private val _selected = MutableStateFlow(Dates.today())
    private val _month = MutableStateFlow(YearMonth.from(Dates.today()))

    private val monthRange = _month.map { month ->
        month.atDay(1).minusDays(7) to month.atEndOfMonth().plusDays(7)
    }

    val state: StateFlow<CalendarUiState> = combine(
        _mode,
        _selected,
        _month,
        monthRange.flatMapLatest { (from, to) ->
            combine(events.observeBetween(from, to), notes.observeBetween(from, to)) { e, n -> e to n }
        },
        _selected.flatMapLatest { observeDay(it) },
    ) { mode, selected, month, (monthEvents, monthTasks), plan ->
        CalendarUiState(mode, selected, month, monthEvents, monthTasks, plan)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun setMode(mode: CalendarMode) { _mode.value = mode }

    fun select(date: LocalDate) {
        _selected.value = date
        _month.value = YearMonth.from(date)
    }

    fun shiftMonth(delta: Long) { _month.value = _month.value.plusMonths(delta) }

    fun saveEvent(event: CalendarEvent, remind: Boolean, leadMinutes: Int) {
        viewModelScope.launch {
            val id = events.save(event)
            if (remind && event.startTime != null) {
                val trigger = Dates.millisOf(event.date, event.startTime) - leadMinutes * 60_000L
                val reminderId = reminders.save(
                    Reminder(
                        id = event.reminderId ?: 0L,
                        targetType = ReminderTarget.Event,
                        targetId = id,
                        title = event.title,
                        body = "Empieza a las ${Dates.time(event.startTime)}",
                        iconKey = event.iconKey,
                        triggerAtMillis = trigger,
                        repeat = RepeatRule.Once,
                        leadMinutes = leadMinutes,
                    )
                )
                events.save(event.copy(id = id, reminderId = reminderId))
            } else if (event.reminderId != null) {
                reminders.delete(event.reminderId)
                events.save(event.copy(id = id, reminderId = null))
            }
        }
    }

    /** Mover un evento arrastrándolo en el calendario. */
    fun moveEvent(id: Long, date: LocalDate, start: LocalTime?) {
        viewModelScope.launch { events.move(id, date, start) }
    }

    fun deleteEvent(id: Long) = viewModelScope.launch { events.delete(id) }

    fun toggleTask(id: Long, done: Boolean) = viewModelScope.launch { notes.setDone(id, done) }
}

class GoalsViewModel(private val goals: GoalRepository) : ViewModel() {

    val state: StateFlow<List<GoalWithSteps>> = goals.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editing = MutableStateFlow<Goal?>(null)
    val editing: StateFlow<Goal?> = _editing.asStateFlow()

    fun startNew() { _editing.value = Goal(title = "") }
    fun edit(goal: Goal) { _editing.value = goal }
    fun update(block: (Goal) -> Goal) { _editing.value = _editing.value?.let(block) }
    fun dismiss() { _editing.value = null }

    fun save() {
        val goal = _editing.value ?: return
        if (goal.title.isBlank()) return
        viewModelScope.launch {
            goals.save(goal)
            _editing.value = null
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        goals.delete(id)
        _editing.value = null
    }

    fun addStep(goalId: Long, title: String, position: Int) = viewModelScope.launch {
        if (title.isNotBlank()) goals.addStep(goalId, title.trim(), position)
    }

    fun toggleStep(stepId: Long, done: Boolean) = viewModelScope.launch { goals.setStepDone(stepId, done) }

    fun deleteStep(stepId: Long) = viewModelScope.launch { goals.deleteStep(stepId) }
}

class HabitsViewModel(
    private val habits: HabitRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {

    val state: StateFlow<List<HabitWithChecks>> = habits.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editing = MutableStateFlow<Habit?>(null)
    val editing: StateFlow<Habit?> = _editing.asStateFlow()

    fun startNew() { _editing.value = Habit(title = "") }
    fun edit(habit: Habit) { _editing.value = habit }
    fun update(block: (Habit) -> Habit) { _editing.value = _editing.value?.let(block) }
    fun dismiss() { _editing.value = null }

    fun toggle(habitId: Long, date: LocalDate, checked: Boolean) =
        viewModelScope.launch { habits.setChecked(habitId, date, checked) }

    fun save() {
        val habit = _editing.value ?: return
        if (habit.title.isBlank()) return
        viewModelScope.launch {
            val id = habits.save(habit)
            val time = habit.reminderTime
            if (time != null) {
                val trigger = Dates.millisOf(Dates.today(), time).let {
                    if (it < System.currentTimeMillis()) Dates.millisOf(Dates.today().plusDays(1), time) else it
                }
                val reminderId = reminders.save(
                    Reminder(
                        id = habit.reminderId ?: 0L,
                        targetType = ReminderTarget.Habit,
                        targetId = id,
                        title = habit.title,
                        body = "Un pequeño paso de hoy.",
                        iconKey = habit.iconKey,
                        triggerAtMillis = trigger,
                        repeat = RepeatRule.Daily,
                    )
                )
                habits.save(habit.copy(id = id, reminderId = reminderId))
            } else if (habit.reminderId != null) {
                reminders.delete(habit.reminderId)
                habits.save(habit.copy(id = id, reminderId = null))
            }
            _editing.value = null
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        habits.delete(id)
        _editing.value = null
    }
}

data class StudyUiState(
    val subjects: List<SubjectDetail> = emptyList(),
    val upcomingExams: List<Exam> = emptyList(),
    val minutesThisWeek: Int = 0,
)

class StudyViewModel(
    private val study: StudyRepository,
    private val notes: NoteRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {

    private val weekStart = Dates.weekStart(Dates.today())

    val state: StateFlow<StudyUiState> = combine(
        study.observeSubjects(),
        study.observeExams(),
        study.observeSessionsBetween(weekStart, weekStart.plusDays(6)),
        notes.observeDesk(),
    ) { subjects, exams, sessions, desk ->
        val details = subjects.map { subject ->
            SubjectDetail(
                subject = subject,
                exams = exams.filter { it.subjectId == subject.id && !it.date.isBefore(Dates.today()) },
                tasks = desk.map { it.note }.filter { it.subjectId == subject.id && it.isTask },
                minutesThisWeek = sessions.filter { it.subjectId == subject.id }.sumOf { it.minutes },
            )
        }
        StudyUiState(
            subjects = details,
            upcomingExams = exams.filter { !it.date.isBefore(Dates.today()) }.sortedBy { it.date },
            minutesThisWeek = sessions.sumOf { it.minutes },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyUiState())

    private val _editingSubject = MutableStateFlow<Subject?>(null)
    val editingSubject: StateFlow<Subject?> = _editingSubject.asStateFlow()

    private val _editingExam = MutableStateFlow<Exam?>(null)
    val editingExam: StateFlow<Exam?> = _editingExam.asStateFlow()

    fun newSubject() { _editingSubject.value = Subject(name = "") }
    fun editSubject(subject: Subject) { _editingSubject.value = subject }
    fun updateSubject(block: (Subject) -> Subject) { _editingSubject.value = _editingSubject.value?.let(block) }
    fun dismissSubject() { _editingSubject.value = null }

    fun saveSubject() {
        val subject = _editingSubject.value ?: return
        if (subject.name.isBlank()) return
        viewModelScope.launch {
            study.saveSubject(subject)
            _editingSubject.value = null
        }
    }

    fun deleteSubject(id: Long) = viewModelScope.launch {
        study.deleteSubject(id)
        _editingSubject.value = null
    }

    fun newExam(subjectId: Long) {
        _editingExam.value = Exam(subjectId = subjectId, title = "Examen", date = Dates.today().plusDays(7))
    }

    fun editExam(exam: Exam) { _editingExam.value = exam }
    fun updateExam(block: (Exam) -> Exam) { _editingExam.value = _editingExam.value?.let(block) }
    fun dismissExam() { _editingExam.value = null }

    fun saveExam(remind: Boolean) {
        val exam = _editingExam.value ?: return
        viewModelScope.launch {
            val id = study.saveExam(exam)
            val time = exam.time
            if (remind && time != null) {
                val reminderId = reminders.save(
                    Reminder(
                        id = exam.reminderId ?: 0L,
                        targetType = ReminderTarget.Exam,
                        targetId = id,
                        title = exam.title,
                        body = "Es hoy, a las ${Dates.time(time)}.",
                        iconKey = "graduation",
                        triggerAtMillis = Dates.millisOf(exam.date, time) - 60 * 60_000L,
                        repeat = RepeatRule.Once,
                        leadMinutes = 60,
                    )
                )
                study.saveExam(exam.copy(id = id, reminderId = reminderId))
            }
            _editingExam.value = null
        }
    }

    fun deleteExam(id: Long) = viewModelScope.launch {
        study.deleteExam(id)
        _editingExam.value = null
    }

    fun setReadiness(exam: Exam, readiness: Int) = viewModelScope.launch {
        study.saveExam(exam.copy(readiness = readiness.coerceIn(0, 100)))
    }

    fun addSession(subjectId: Long?, date: LocalDate, start: LocalTime, minutes: Int) =
        viewModelScope.launch {
            study.saveSession(
                StudySession(subjectId = subjectId, date = date, startTime = start, minutes = minutes)
            )
        }

    fun toggleTask(id: Long, done: Boolean) = viewModelScope.launch { notes.setDone(id, done) }
}
