package com.rincon.espacio.ui.screens.goals

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.HabitCadence
import com.rincon.espacio.ui.components.CozySheet
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FieldLabel
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.HabitCard
import com.rincon.espacio.ui.components.IconPicker
import com.rincon.espacio.ui.components.PaperColorPicker
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.PrimaryButton
import com.rincon.espacio.ui.components.SegmentedControl
import com.rincon.espacio.ui.components.SheetTitle
import com.rincon.espacio.ui.components.StepperRow
import com.rincon.espacio.ui.components.TimePickerSheet
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.HabitsViewModel

/**
 * Hábitos.
 *
 * La semana se ve entera, sin abrir nada. La racha existe, pero es un detalle
 * pequeño en una esquina: si se rompe un día no debería doler.
 */
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val habits by viewModel.state.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    val feedback = LocalFeedback.current
    val today = Dates.today()
    val weekStart = Dates.weekStart(today)

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
                Column(Modifier.statusBarsPadding().padding(top = Space.l, bottom = Space.s)) {
                    Text("Hábitos", style = Rincon.type.display, color = colors.textPrimary)
                    Text(
                        "Semana del ${Dates.shortDate(weekStart)}",
                        style = Rincon.type.body,
                        color = colors.textSecondary,
                    )
                }
            }

            if (habits.isEmpty()) {
                item {
                    PaperSurface(Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = RinconIcons.Leaf,
                            title = "Sin hábitos todavía",
                            message = "Beber agua, leer diez minutos, salir a andar...",
                            action = {
                                PrimaryButton("Crear hábito", viewModel::startNew, icon = RinconIcons.Plus)
                            },
                        )
                    }
                }
            }

            items(habits, key = { it.habit.id }) { item ->
                HabitCard(
                    item = item,
                    weekStart = weekStart,
                    today = today,
                    onToggle = { date, checked ->
                        viewModel.toggle(item.habit.id, date, checked)
                        if (checked) feedback.complete()
                    },
                    onOpen = { viewModel.edit(item.habit) },
                )
            }
        }

        FloatingAddButton(
            onClick = viewModel::startNew,
            description = "Crear hábito",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.l),
        )
    }

    val habit = editing
    if (habit != null) {
        var showTime by remember(habit.id) { mutableStateOf(false) }
        CozySheet(
            visible = true,
            onDismiss = viewModel::dismiss,
            footer = {
                Row(
                    modifier = Modifier.padding(horizontal = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    if (habit.id != 0L) {
                        GhostButton(
                            "Borrar",
                            { viewModel.delete(habit.id) },
                            icon = RinconIcons.Trash,
                            tint = colors.danger,
                        )
                    }
                    PrimaryButton(
                        label = "Guardar",
                        icon = RinconIcons.Check,
                        onClick = viewModel::save,
                        modifier = Modifier.weight(1f),
                        enabled = habit.title.isNotBlank(),
                    )
                }
            },
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                SheetTitle(if (habit.id == 0L) "Nuevo hábito" else "Editar hábito")
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(horizontal = Space.xl)) {
                    CozyTextField(
                        value = habit.title,
                        onValueChange = { text -> viewModel.update { it.copy(title = text) } },
                        placeholder = "¿Qué quieres hacer a menudo?",
                        singleLine = true,
                    )
                    Spacer(Modifier.height(Space.l))
                    FieldLabel("Cada cuánto")
                    SegmentedControl(
                        options = HabitCadence.entries.toList(),
                        selected = habit.cadence,
                        label = { it.label },
                        onSelect = { cadence -> viewModel.update { it.copy(cadence = cadence) } },
                    )
                    if (habit.cadence == HabitCadence.TimesPerWeek) {
                        Spacer(Modifier.height(Space.m))
                        StepperRow(
                            label = "Veces por semana",
                            value = habit.timesPerWeek,
                            onChange = { value -> viewModel.update { it.copy(timesPerWeek = value) } },
                            range = 1..7,
                        )
                    }
                    Spacer(Modifier.height(Space.l))
                    GhostButton(
                        label = habit.reminderTime?.let { "Aviso a las ${Dates.time(it)}" } ?: "Sin recordatorio",
                        icon = RinconIcons.Bell,
                        onClick = { showTime = true },
                    )
                    Spacer(Modifier.height(Space.l))
                    FieldLabel("Color")
                }
                PaperColorPicker(habit.colorKey, onSelect = { key -> viewModel.update { it.copy(colorKey = key) } })
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(start = Space.xl)) { FieldLabel("Icono") }
                IconPicker(habit.iconKey, onSelect = { key -> viewModel.update { it.copy(iconKey = key) } })
                Spacer(Modifier.height(Space.s))
            }
        }
        TimePickerSheet(
            visible = showTime,
            initial = habit.reminderTime,
            onDismiss = { showTime = false },
            onPick = { time ->
                viewModel.update { it.copy(reminderTime = time) }
                showTime = false
            },
        )
    }
}
