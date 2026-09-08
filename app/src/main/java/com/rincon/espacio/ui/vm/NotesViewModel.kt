package com.rincon.espacio.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.data.prefs.DeskView
import com.rincon.espacio.data.prefs.PreferencesRepository
import com.rincon.espacio.data.repo.GoalRepository
import com.rincon.espacio.data.repo.NoteRepository
import com.rincon.espacio.data.repo.ReminderRepository
import com.rincon.espacio.data.repo.StudyRepository
import com.rincon.espacio.domain.model.Goal
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.domain.model.Reminder
import com.rincon.espacio.domain.model.ReminderTarget
import com.rincon.espacio.domain.model.RepeatRule
import com.rincon.espacio.domain.model.Subject
import com.rincon.espacio.domain.model.Subtask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/** Lo que el editor de notas está manipulando ahora mismo. */
data class NoteDraft(
    val note: Note,
    val subtasks: List<Subtask> = emptyList(),
    val reminderEnabled: Boolean = false,
    val leadMinutes: Int = 0,
    val repeat: RepeatRule = RepeatRule.Once,
    val reminderSound: Boolean = true,
    val reminderVibrate: Boolean = true,
    val isNew: Boolean = true,
)

data class DeskUiState(
    val notes: List<NoteWithSubtasks> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val goals: List<Goal> = emptyList(),
)

/**
 * Estado del escritorio y del editor de notas.
 *
 * Guarda la colocación física por separado del contenido: al arrastrar sólo se
 * escribe posición/rotación/capa, que es lo que ocurre decenas de veces por
 * sesión.
 */
class NotesViewModel(
    private val notes: NoteRepository,
    private val reminders: ReminderRepository,
    private val preferences: PreferencesRepository,
    study: StudyRepository,
    goals: GoalRepository,
) : ViewModel() {

    /** Vista guardada del tablero, para volver justo donde lo dejaste. */
    val deskView: StateFlow<DeskView> = preferences.deskView
        .stateIn(viewModelScope, SharingStarted.Eagerly, DeskView())

    fun rememberDeskView(scale: Float, offsetX: Float, offsetY: Float) {
        viewModelScope.launch { preferences.saveDeskView(scale, offsetX, offsetY) }
    }

    val state: StateFlow<DeskUiState> = combine(
        notes.observeDesk(),
        study.observeSubjects(),
        goals.observeGoals(),
    ) { desk, subjects, goalList ->
        DeskUiState(desk, subjects, goalList.map { it.goal })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeskUiState())

    private val _draft = MutableStateFlow<NoteDraft?>(null)
    val draft: StateFlow<NoteDraft?> = _draft.asStateFlow()

    /**
     * Última nota borrada.
     *
     * Romper una nota es un gesto satisfactorio y por eso mismo es fácil
     * hacerlo sin querer. Se guarda entera —con sus pasos— unos segundos, para
     * que deshacer sea posible y no haya que arrepentirse de una animación
     * bonita.
     */
    private val _recentlyDeleted = MutableStateFlow<NoteWithSubtasks?>(null)
    val recentlyDeleted: StateFlow<NoteWithSubtasks?> = _recentlyDeleted.asStateFlow()

    fun dismissUndo() { _recentlyDeleted.value = null }

    fun undoDelete() {
        val snapshot = _recentlyDeleted.value ?: return
        _recentlyDeleted.value = null
        viewModelScope.launch {
            // Vuelve al mismo sitio del tablero, con su color y su inclinación.
            val id = notes.create(snapshot.note.copy(id = 0L))
            if (snapshot.subtasks.isNotEmpty()) {
                notes.replaceSubtasks(id, snapshot.subtasks)
            }
        }
    }

    private fun rememberForUndo(id: Long) {
        _recentlyDeleted.value = state.value.notes.firstOrNull { it.note.id == id }
    }

/**
     * Nota nueva.
     *
     * Nace en el punto del tablero que se está mirando, no en una esquina fija:
     * si te has ido a una zona del tablero a organizar algo, la nota aparece
     * ahí, contigo.
     */
    fun startNewNote(boardX: Float = 40f, boardY: Float = 40f) {
        val resting = Random.nextDouble(-3.0, 3.0).toFloat()
        _draft.value = NoteDraft(
            note = Note(
                x = boardX.coerceAtLeast(0f),
                y = boardY.coerceAtLeast(0f),
                rotation = resting,
                colorKey = defaultPalette.random(),
                styleKey = defaultStyles.random(),
            ),
            isNew = true,
        )
    }

    fun startNewTask(date: LocalDate?, subjectId: Long? = null, goalId: Long? = null) {
        _draft.value = NoteDraft(
            note = Note(
                isTask = true,
                dueDate = date,
                subjectId = subjectId,
                goalId = goalId,
                iconKey = if (subjectId != null) "book" else "check",
                colorKey = if (subjectId != null) "Sky" else "Sage",
                rotation = Random.nextDouble(-2.5, 2.5).toFloat(),
                x = Random.nextDouble(30.0, 320.0).toFloat(),
                y = Random.nextDouble(30.0, 420.0).toFloat(),
            ),
            isNew = true,
        )
    }

    fun openNote(id: Long) {
        viewModelScope.launch {
            val current = state.value.notes.firstOrNull { it.note.id == id }
                ?: notes.byId(id)?.let { NoteWithSubtasks(it) }
                ?: return@launch
            val reminder = current.note.reminderId?.let { reminders.byId(it) }
            _draft.value = NoteDraft(
                note = current.note,
                subtasks = current.subtasks,
                reminderEnabled = reminder?.enabled == true,
                leadMinutes = reminder?.leadMinutes ?: 0,
                repeat = reminder?.repeat ?: RepeatRule.Once,
                reminderSound = reminder?.sound ?: true,
                reminderVibrate = reminder?.vibrate ?: true,
                isNew = false,
            )
        }
    }

    fun dismissEditor() { _draft.value = null }

    fun editNote(block: (Note) -> Note) {
        _draft.update { current -> current?.copy(note = block(current.note)) }
    }

    fun editDraft(block: (NoteDraft) -> NoteDraft) {
        _draft.update { current -> current?.let(block) }
    }

    fun addSubtask(text: String) {
        if (text.isBlank()) return
        _draft.update { current ->
            current?.copy(
                subtasks = current.subtasks + Subtask(
                    noteId = current.note.id,
                    text = text.trim(),
                    position = current.subtasks.size,
                )
            )
        }
    }

    fun toggleDraftSubtask(index: Int) {
        _draft.update { current ->
            current ?: return@update null
            val list = current.subtasks.toMutableList()
            list.getOrNull(index)?.let { list[index] = it.copy(done = !it.done) }
            current.copy(subtasks = list)
        }
    }

    fun removeDraftSubtask(index: Int) {
        _draft.update { current ->
            current ?: return@update null
            current.copy(subtasks = current.subtasks.filterIndexed { i, _ -> i != index })
        }
    }

    /**
     * Guarda la nota y mantiene coherente su recordatorio: si hay fecha, hora y
     * aviso activo se crea o actualiza; si falta cualquiera de esas cosas, se
     * elimina. Así "poner fecha y hora" basta para tener un recordatorio, que es
     * justo lo que se espera al escribir una nota con hora.
     */
    fun saveDraft() {
        val current = _draft.value ?: return
        viewModelScope.launch {
            val id = if (current.isNew) {
                notes.create(current.note)
            } else {
                notes.update(current.note)
                current.note.id
            }

            notes.replaceSubtasks(id, current.subtasks)

            val date = current.note.dueDate
            val time = current.note.dueTime
            val wants = current.reminderEnabled && date != null && time != null

            if (wants) {
                val triggerAt = Dates.millisOf(date!!, time!!) - current.leadMinutes * 60_000L
                val reminder = Reminder(
                    id = current.note.reminderId ?: 0L,
                    targetType = ReminderTarget.Note,
                    targetId = id,
                    title = current.note.title.ifBlank { "Recordatorio" },
                    body = buildString {
                        append(Dates.time(time))
                        if (current.leadMinutes > 0) append(" · aviso ${current.leadMinutes} min antes")
                    },
                    iconKey = current.note.iconKey,
                    triggerAtMillis = triggerAt,
                    // El aviso sigue el ritmo de la tarea: si ella vuelve
                    // cada semana, él también.
                    repeat = if (current.note.repeat != RepeatRule.Once) {
                        current.note.repeat
                    } else {
                        current.repeat
                    },
                    leadMinutes = current.leadMinutes,
                    enabled = true,
                    sound = current.reminderSound,
                    vibrate = current.reminderVibrate,
                )
                val reminderId = reminders.save(reminder)
                notes.update(current.note.copy(id = id, reminderId = reminderId))
            } else if (current.note.reminderId != null) {
                reminders.delete(current.note.reminderId!!)
                notes.update(current.note.copy(id = id, reminderId = null))
            }

            _draft.value = null
        }
    }

    fun deleteDraft() {
        val current = _draft.value ?: return
        _draft.value = null
        if (current.isNew) return
        deleteNote(current.note.id)
    }

    fun deleteNote(id: Long) {
        rememberForUndo(id)
        viewModelScope.launch {
            notes.delete(id)
            delay(UNDO_WINDOW_MS)
            if (_recentlyDeleted.value?.note?.id == id) _recentlyDeleted.value = null
        }
    }

    fun toggleDone(id: Long, done: Boolean) {
        viewModelScope.launch { notes.setDone(id, done) }
    }

    fun savePlacement(id: Long, x: Float, y: Float, rotation: Float, z: Int) {
        viewModelScope.launch { notes.savePlacement(id, x, y, rotation, z) }
    }

    fun quickAdd(text: String, date: LocalDate?, time: LocalTime?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            notes.create(
                Note(
                    text = text.trim(),
                    isTask = true,
                    dueDate = date,
                    dueTime = time,
                    iconKey = "check",
                    colorKey = defaultPalette.random(),
                    priority = Priority.Normal,
                    rotation = Random.nextDouble(-2.5, 2.5).toFloat(),
                    x = Random.nextDouble(30.0, 340.0).toFloat(),
                    y = Random.nextDouble(30.0, 500.0).toFloat(),
                )
            )
        }
    }

    private companion object {
        const val UNDO_WINDOW_MS = 6_000L
        val defaultPalette = listOf(
            "Butter", "Sky", "Sage", "Rose", "Lavender", "Peach", "Cream", "Mint", "Coral", "Sand",
        )
        // Variar el estilo hace que el escritorio parezca un cajón de papeles
        // de verdad y no un paquete de folios idénticos.
        val defaultStyles = listOf("Plain", "Plain", "Lined", "Grid", "Sticky", "Recycled")
    }
}
