package com.rincon.espacio.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.core.util.Phrases
import com.rincon.espacio.domain.model.DayItem
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.GoalCard
import com.rincon.espacio.ui.components.PaperNote
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.ProgressTrack
import com.rincon.espacio.ui.components.RinconIcon
import com.rincon.espacio.ui.components.SectionHeader
import com.rincon.espacio.ui.components.TaskRow
import com.rincon.espacio.ui.components.animatedCount
import com.rincon.espacio.ui.components.appear
import com.rincon.espacio.ui.components.pressable
import com.rincon.espacio.ui.components.softShadow
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.HomeViewModel
import com.rincon.espacio.ui.vm.NotesViewModel

/**
 * Inicio.
 *
 * El objetivo no es enseñar todo lo pendiente, sino que en dos segundos se sepa
 * qué toca hoy y cómo va la cosa. Por eso el progreso va arriba, las tareas se
 * limitan a las importantes y lo demás son accesos, no listas.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    notesViewModel: NotesViewModel,
    onOpenDesk: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    var searching by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Space.screen,
            end = Space.screen,
            bottom = bottomInset + Space.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = Space.l).appear(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(state.greeting)
                            if (state.displayName.isNotBlank()) append(", ${state.displayName}")
                        },
                        style = Rincon.type.display,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(Space.s))
                    RinconIcon(
                        RinconIcons.byKey(state.greetingIcon),
                        null,
                        tint = colors.warning,
                        size = 30.dp,
                    )
                }
                Spacer(Modifier.height(Space.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Dates.longDate(state.date),
                        style = Rincon.type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    com.rincon.espacio.ui.components.RoundIconButton(
                        icon = RinconIcons.Sparkle,
                        description = "Buscar en tus notas",
                        onClick = { searching = true },
                    )
                }
                Spacer(Modifier.height(Space.m))
                // Una línea, cambia cada día, y no pide nada a cambio.
                Text(
                    text = Phrases.forDate(state.date),
                    style = Rincon.type.bodyStrong,
                    color = colors.accent,
                    modifier = Modifier.appear(1),
                )
            }
        }

        item {
            DayProgressCard(
                done = state.plan.doneCount,
                total = state.plan.totalCount,
                onClick = onOpenToday,
                modifier = Modifier.appear(2),
            )
        }

        if (state.overdue.isNotEmpty()) {
            item {
                PaperSurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accentSoft,
                    elevation = 4.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().pressable(onClick = onOpenToday).padding(Space.l),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RinconIcon(RinconIcons.Clock, null, tint = colors.accent, size = 22.dp)
                        Spacer(Modifier.width(Space.m))
                        Text(
                            "${state.overdue.size} ${if (state.overdue.size == 1) "cosa se quedó atrás" else "cosas se quedaron atrás"}",
                            style = Rincon.type.bodyStrong,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        RinconIcon(RinconIcons.ChevronRight, null, tint = colors.textSecondary, size = 20.dp)
                    }
                }
            }
        }

        item {
            // Ancho fijo y fila deslizante: repartir a partes iguales rompía
            // las palabras largas con la escala de texto del sistema alta.
            LazyRow(
                modifier = Modifier.appear(3),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                item { QuickAccess(RinconIcons.Target, "Objetivos", onClick = onOpenGoals) }
                item { QuickAccess(RinconIcons.Leaf, "Hábitos", onClick = onOpenHabits) }
                item { QuickAccess(RinconIcons.Book, "Estudio", onClick = onOpenStudy) }
                item { QuickAccess(RinconIcons.Calendar, "Agenda", onClick = onOpenCalendar) }
            }
        }

        item {
            SectionHeader(
                title = "Tu día",
                trailing = {
                    GhostButton("Ver todo", onOpenToday, icon = RinconIcons.ChevronRight)
                },
            )
        }

        val upcoming = state.plan.upcoming.take(4)
        if (upcoming.isEmpty()) {
            item {
                PaperSurface(Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = RinconIcons.Sparkle,
                        title = "Hoy está despejado",
                        message = "Disfrútalo, o añade algo pequeño.",
                    )
                }
            }
        } else {
            items(upcoming, key = { it.id }) { item ->
                when (item) {
                    is DayItem.TaskItem -> TaskRow(
                        note = item.note,
                        subtaskProgress = item.subtaskProgress,
                        onToggle = { notesViewModel.toggleDone(item.note.id, !item.note.done) },
                        onOpen = { notesViewModel.openNote(item.note.id) },
                    )
                    else -> DayItemRow(item)
                }
            }
        }

        if (state.recentNotes.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Notas recientes",
                    trailing = { GhostButton("Escritorio", onOpenDesk, icon = RinconIcons.Note) },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    items(state.recentNotes, key = { it.note.id }) { note ->
                        Box(
                            Modifier
                                .width(164.dp)
                                .softShadow(6.dp, Rincon.shapes.note)
                                .pressable { notesViewModel.openNote(note.note.id) }
                        ) {
                            PaperNote(item = note, compact = true)
                        }
                    }
                }
            }
        }

        if (state.goals.isNotEmpty()) {
            item { SectionHeader("Tus objetivos") }
            items(state.goals, key = { it.goal.id }) { goal ->
                GoalCard(item = goal, onOpen = onOpenGoals, onToggleStep = { _, _ -> onOpenGoals() })
            }
        }

        val habits = state.plan.habits
        if (habits.isNotEmpty()) {
            item { SectionHeader("Hábitos de hoy") }
            item {
                PaperSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Space.l)) {
                        habits.forEach { habit ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressable(onClick = onOpenHabits)
                                    .padding(vertical = Space.s),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RinconIcon(
                                    RinconIcons.byKey(habit.habit.iconKey),
                                    null,
                                    tint = if (habit.done) colors.success else colors.textSecondary,
                                    size = 22.dp,
                                )
                                Spacer(Modifier.width(Space.m))
                                Text(
                                    habit.habit.title,
                                    style = Rincon.type.body,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (habit.done) {
                                    RinconIcon(RinconIcons.Check, "Hecho", tint = colors.success, size = 20.dp)
                                } else {
                                    Text("Pendiente", style = Rincon.type.caption, color = colors.textMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    HomeSearch(
        visible = searching,
        notesViewModel = notesViewModel,
        onDismiss = { searching = false },
    )
}

@Composable
private fun HomeSearch(
    visible: Boolean,
    notesViewModel: NotesViewModel,
    onDismiss: () -> Unit,
) = SearchSheet(visible = visible, notesViewModel = notesViewModel, onDismiss = onDismiss)

@Composable
private fun DayProgressCard(
    done: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val progress = if (total == 0) 0f else done.toFloat() / total
    PaperSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = 10.dp,
    ) {
        Column(Modifier.fillMaxWidth().pressable(onClick = onClick).padding(Space.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hoy", style = Rincon.type.section, color = colors.textPrimary)
                    Text(
                        text = when {
                            total == 0 -> "Nada pendiente por ahora"
                            done == total -> "Todo hecho. Qué gusto."
                            else -> "$done de $total cosas hechas"
                        },
                        style = Rincon.type.body,
                        color = colors.textSecondary,
                    )
                }
                // El número sube contando, no salta: el progreso se ve avanzar.
                Text(
                    "${animatedCount((progress * 100).toInt())}%",
                    style = Rincon.type.numeral,
                    color = colors.accent,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(Space.l))
            ProgressTrack(progress, height = 12.dp)
        }
    }
}

@Composable
private fun QuickAccess(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = Rincon.colors
    PaperSurface(modifier = modifier.width(96.dp), elevation = 4.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .pressable(onClick = onClick)
                .padding(horizontal = Space.s, vertical = Space.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceSunken),
                contentAlignment = Alignment.Center,
            ) {
                RinconIcon(icon, null, tint = colors.accent, size = 22.dp)
            }
            Spacer(Modifier.height(Space.xs))
            Text(
                label,
                style = Rincon.type.navLabel,
                color = colors.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Fila genérica para eventos, exámenes y sesiones de estudio. */
@Composable
fun DayItemRow(item: DayItem, modifier: Modifier = Modifier) {
    val colors = Rincon.colors
    val (icon, title, subtitle) = when (item) {
        is DayItem.EventItem -> Triple(
            RinconIcons.byKey(item.event.iconKey),
            item.event.title,
            item.event.startTime?.let { Dates.time(it) } ?: "Todo el día",
        )
        is DayItem.ExamItem -> Triple(
            RinconIcons.Graduation,
            item.exam.title,
            listOfNotNull(item.subject?.name, item.exam.time?.let { Dates.time(it) }).joinToString(" · "),
        )
        is DayItem.StudyItem -> Triple(
            RinconIcons.Book,
            item.subject?.name ?: "Sesión de estudio",
            "${Dates.time(item.session.startTime)} · ${item.session.minutes} min",
        )
        is DayItem.TaskItem -> Triple(
            RinconIcons.byKey(item.note.iconKey),
            item.note.title,
            item.note.dueTime?.let { Dates.time(it) } ?: "",
        )
        is DayItem.HabitItem -> Triple(
            RinconIcons.byKey(item.habit.iconKey),
            item.habit.title,
            item.habit.cadence.label,
        )
    }

    PaperSurface(modifier = modifier.fillMaxWidth(), elevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(Space.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceSunken),
                contentAlignment = Alignment.Center,
            ) {
                RinconIcon(icon, null, tint = colors.textSecondary, size = 21.dp)
            }
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = Rincon.type.bodyStrong, color = colors.textPrimary, maxLines = 1)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = Rincon.type.caption, color = colors.textSecondary, maxLines = 1)
                }
            }
        }
    }
}
