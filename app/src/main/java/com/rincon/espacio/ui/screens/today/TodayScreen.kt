package com.rincon.espacio.ui.screens.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.DayItem
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.HabitCard
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.ProgressTrack
import com.rincon.espacio.ui.components.RoundIconButton
import com.rincon.espacio.ui.components.SectionHeader
import com.rincon.espacio.ui.components.SwipeableRow
import com.rincon.espacio.ui.components.TaskRow
import com.rincon.espacio.ui.components.animatedCount
import com.rincon.espacio.ui.components.appear
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.screens.home.DayItemRow
import com.rincon.espacio.ui.vm.NotesViewModel
import com.rincon.espacio.ui.vm.TodayViewModel

/**
 * Hoy: un solo día, en orden, con lo que se puede hacer ya.
 *
 * Deslizar una tarea a la derecha la completa; a la izquierda la borra. Ambos
 * gestos tienen un equivalente visible (casilla y editor) para no depender de
 * atajos ocultos.
 */
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    notesViewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val date by viewModel.date.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val deskState by notesViewModel.state.collectAsStateWithLifecycle()
    val draft by notesViewModel.draft.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    val feedback = LocalFeedback.current
    val today = Dates.today()
    val motion = Rincon.motion

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                bottom = bottomInset + Space.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = Space.l).appear(0)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cambiar de día desliza el encabezado en la dirección
                        // del viaje: se entiende hacia dónde te has movido sin
                        // tener que leer la fecha.
                        AnimatedContent(
                            targetState = date,
                            modifier = Modifier.weight(1f),
                            transitionSpec = {
                                val forward = targetState.isAfter(initialState)
                                val enter = slideInHorizontally(motion.appear()) {
                                    if (forward) it / 3 else -it / 3
                                } + fadeIn(motion.fade())
                                val exit = slideOutHorizontally(motion.appear()) {
                                    if (forward) -it / 3 else it / 3
                                } + fadeOut(motion.quickFade())
                                enter togetherWith exit
                            },
                            label = "day",
                        ) { shown ->
                            Column {
                                Text(
                                    text = Dates.relative(shown, today),
                                    style = Rincon.type.display,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                )
                                Text(
                                    Dates.longDate(shown),
                                    style = Rincon.type.body,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(Modifier.width(Space.s))
                        RoundIconButton(RinconIcons.ChevronLeft, "Día anterior", onClick = {
                            feedback.click(); viewModel.shiftDays(-1)
                        })
                        Spacer(Modifier.width(Space.s))
                        RoundIconButton(RinconIcons.ChevronRight, "Día siguiente", onClick = {
                            feedback.click(); viewModel.shiftDays(1)
                        })
                    }
                    if (date != today) {
                        Spacer(Modifier.height(Space.s))
                        com.rincon.espacio.ui.components.GhostButton(
                            label = "Volver a hoy",
                            icon = RinconIcons.Undo,
                            onClick = { feedback.click(); viewModel.goTo(today) },
                        )
                    }
                }
            }

            item {
                PaperSurface(Modifier.fillMaxWidth().appear(1), elevation = 8.dp) {
                    Column(Modifier.padding(Space.xl)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (plan.totalCount == 0) "Sin nada anotado"
                                else "${plan.doneCount} de ${plan.totalCount} hechas",
                                style = Rincon.type.cardTitle,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${animatedCount((plan.progress * 100).toInt())}%",
                                style = Rincon.type.section,
                                color = colors.accent,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.height(Space.m))
                        ProgressTrack(plan.progress, height = 12.dp)
                    }
                }
            }

            val upcoming = plan.upcoming
            item { SectionHeader(if (upcoming.isEmpty()) "Tu día" else "Tu día · ${upcoming.size}") }

            if (upcoming.isEmpty()) {
                item {
                    PaperSurface(Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = RinconIcons.Coffee,
                            title = "No hay nada aquí todavía",
                            message = "Empieza creando una tarea con el botón +.",
                        )
                    }
                }
            } else {
                items(upcoming, key = { it.id }) { item ->
                    // Al completar o borrar, la lista se recoloca deslizándose;
                    // nada aparece ni desaparece de golpe.
                    Box(Modifier.animateItem()) {
                    when (item) {
                        is DayItem.TaskItem -> SwipeableRow(
                            onSwipeRight = { viewModel.toggleTask(item.note.id, !item.note.done) },
                            onSwipeLeft = { notesViewModel.deleteNote(item.note.id) },
                        ) {
                            TaskRow(
                                note = item.note,
                                subtaskProgress = item.subtaskProgress,
                                onToggle = { viewModel.toggleTask(item.note.id, !item.note.done) },
                                onOpen = { notesViewModel.openNote(item.note.id) },
                            )
                        }
                        is DayItem.StudyItem -> SwipeableRow(
                            onSwipeRight = {
                                viewModel.toggleStudySession(item.session.id, !item.session.completed)
                            },
                            onSwipeLeft = null,
                        ) { DayItemRow(item) }
                        else -> DayItemRow(item)
                    }
                    }
                }
            }

            val habits = plan.habits
            if (habits.isNotEmpty()) {
                item { SectionHeader("Hábitos") }
                item {
                    PaperSurface(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Space.l)) {
                            habits.forEach { habitItem ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = Space.s),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    com.rincon.espacio.ui.components.NoteCheckbox(
                                        checked = habitItem.done,
                                        ink = colors.textPrimary,
                                        onToggle = {
                                            viewModel.toggleHabit(habitItem.habit.id, date, !habitItem.done)
                                        },
                                    )
                                    Spacer(Modifier.width(Space.m))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            habitItem.habit.title,
                                            style = Rincon.type.bodyStrong,
                                            color = colors.textPrimary,
                                        )
                                        if (habitItem.streak > 1) {
                                            Text(
                                                "${habitItem.streak} días seguidos",
                                                style = Rincon.type.caption,
                                                color = colors.textSecondary,
                                            )
                                        }
                                    }
                                    com.rincon.espacio.ui.components.RinconIcon(
                                        RinconIcons.byKey(habitItem.habit.iconKey),
                                        null,
                                        tint = colors.textMuted,
                                        size = 20.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingAddButton(
            onClick = { notesViewModel.startNewTask(date) },
            description = "Crear tarea para este día",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.l),
        )
    }

    com.rincon.espacio.ui.screens.canvas.NoteEditorSheet(
        draft = draft,
        subjects = deskState.subjects,
        goals = deskState.goals,
        viewModel = notesViewModel,
    )
}
