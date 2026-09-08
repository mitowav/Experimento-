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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.ui.components.CozySheet
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.DatePickerSheet
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FieldLabel
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.GoalCard
import com.rincon.espacio.ui.components.IconPicker
import com.rincon.espacio.ui.components.PaperColorPicker
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.PrimaryButton
import com.rincon.espacio.ui.components.ProgressTrack
import com.rincon.espacio.ui.components.SheetTitle
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.GoalsViewModel

/**
 * Objetivos.
 *
 * Sin niveles, monedas ni experiencia: el único premio es ver la barra avanzar
 * y que quede menos. Un objetivo se divide en pasos pequeños y cada paso marcado
 * mueve el progreso con el mismo muelle que el resto de la app.
 */
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val goals by viewModel.state.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    val feedback = LocalFeedback.current
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var newStep by remember { mutableStateOf("") }

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
                    Text("Objetivos", style = Rincon.type.display, color = colors.textPrimary)
                    Text(
                        "Cosas grandes, en trozos pequeños.",
                        style = Rincon.type.body,
                        color = colors.textSecondary,
                    )
                }
            }

            if (goals.isEmpty()) {
                item {
                    PaperSurface(Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = RinconIcons.Target,
                            title = "Aún no hay objetivos",
                            message = "Empieza por uno. Con dos o tres pasos basta.",
                            action = {
                                PrimaryButton("Crear objetivo", viewModel::startNew, icon = RinconIcons.Plus)
                            },
                        )
                    }
                }
            }

            items(goals, key = { it.goal.id }) { item ->
                Column {
                    GoalCard(
                        item = item,
                        onOpen = {
                            expandedId = if (expandedId == item.goal.id) null else item.goal.id
                        },
                        onToggleStep = { stepId, done ->
                            viewModel.toggleStep(stepId, done)
                            if (done && item.steps.count { it.done } + 1 == item.steps.size) {
                                feedback.goalReached()
                            } else {
                                feedback.complete()
                            }
                        },
                    )
                    if (expandedId == item.goal.id) {
                        Spacer(Modifier.height(Space.s))
                        PaperSurface(Modifier.fillMaxWidth(), elevation = 3.dp) {
                            Column(Modifier.padding(Space.l)) {
                                FieldLabel("Añadir un paso")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        CozyTextField(
                                            value = newStep,
                                            onValueChange = { newStep = it },
                                            placeholder = "Un paso pequeño...",
                                            singleLine = true,
                                            imeAction = ImeAction.Done,
                                        )
                                    }
                                    Spacer(Modifier.width(Space.s))
                                    GhostButton("Añadir", {
                                        viewModel.addStep(item.goal.id, newStep, item.steps.size)
                                        newStep = ""
                                    }, icon = RinconIcons.Plus)
                                }
                                Spacer(Modifier.height(Space.m))
                                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                                    GhostButton("Editar", { viewModel.edit(item.goal) }, icon = RinconIcons.Pencil)
                                    GhostButton(
                                        "Borrar",
                                        { viewModel.delete(item.goal.id) },
                                        icon = RinconIcons.Trash,
                                        tint = colors.danger,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (goals.isNotEmpty()) {
                item {
                    val total = goals.sumOf { it.steps.size }
                    val done = goals.sumOf { g -> g.steps.count { it.done } }
                    PaperSurface(Modifier.fillMaxWidth(), elevation = 6.dp) {
                        Column(Modifier.padding(Space.xl)) {
                            Text("En conjunto", style = Rincon.type.cardTitle, color = colors.textPrimary)
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                if (total == 0) "Añade pasos para ver tu avance"
                                else "$done de $total pasos completados",
                                style = Rincon.type.body,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(Space.m))
                            ProgressTrack(if (total == 0) 0f else done.toFloat() / total, height = 12.dp)
                        }
                    }
                }
            }
        }

        FloatingAddButton(
            onClick = viewModel::startNew,
            description = "Crear objetivo",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.l),
        )
    }

    val goal = editing
    if (goal != null) {
        var showDate by remember(goal.id) { mutableStateOf(false) }
        CozySheet(visible = true, onDismiss = viewModel::dismiss) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 580.dp)
            ) {
                SheetTitle(if (goal.id == 0L) "Nuevo objetivo" else "Editar objetivo")
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(horizontal = Space.xl)) {
                    CozyTextField(
                        value = goal.title,
                        onValueChange = { text -> viewModel.update { it.copy(title = text) } },
                        placeholder = "¿Qué quieres conseguir?",
                        singleLine = true,
                    )
                    Spacer(Modifier.height(Space.m))
                    CozyTextField(
                        value = goal.note,
                        onValueChange = { text -> viewModel.update { it.copy(note = text) } },
                        placeholder = "Por qué te importa (opcional)",
                        minLines = 2,
                    )
                    Spacer(Modifier.height(Space.l))
                    GhostButton(
                        label = goal.targetDate?.let { "Para ${Dates.shortDate(it)}" } ?: "Sin fecha límite",
                        icon = RinconIcons.Calendar,
                        onClick = { showDate = true },
                    )
                    Spacer(Modifier.height(Space.l))
                    FieldLabel("Color")
                }
                PaperColorPicker(goal.colorKey) { key -> viewModel.update { it.copy(colorKey = key) } }
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(start = Space.xl)) { FieldLabel("Icono") }
                IconPicker(goal.iconKey) { key -> viewModel.update { it.copy(iconKey = key) } }
                Spacer(Modifier.height(Space.xl))
                Column(Modifier.padding(horizontal = Space.xl)) {
                    PrimaryButton(
                        label = "Guardar",
                        icon = RinconIcons.Check,
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = goal.title.isNotBlank(),
                    )
                }
            }
        }
        DatePickerSheet(
            visible = showDate,
            initial = goal.targetDate,
            onDismiss = { showDate = false },
            onPick = { date ->
                viewModel.update { it.copy(targetDate = date) }
                showDate = false
            },
        )
    }
}
