package com.rincon.espacio.ui.screens.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.Goal
import com.rincon.espacio.domain.model.RepeatRule
import com.rincon.espacio.domain.model.Subject
import com.rincon.espacio.ui.components.CozyChip
import com.rincon.espacio.ui.components.CozySheet
import com.rincon.espacio.ui.components.CozySwitch
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.DatePickerSheet
import com.rincon.espacio.ui.components.FieldLabel
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.IconPicker
import com.rincon.espacio.ui.components.NoteCheckbox
import com.rincon.espacio.ui.components.PaperColorPicker
import com.rincon.espacio.ui.components.PaperStylePicker
import com.rincon.espacio.ui.components.PrimaryButton
import com.rincon.espacio.ui.components.PriorityPicker
import com.rincon.espacio.ui.components.RinconIcon
import com.rincon.espacio.ui.components.StepperRow
import com.rincon.espacio.ui.components.TimePickerSheet
import com.rincon.espacio.ui.components.pressable
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.NoteDraft
import com.rincon.espacio.ui.vm.NotesViewModel

/**
 * Editor de nota.
 *
 * Lo primero que se ve es lo único que casi siempre se necesita: escribir y
 * elegir el color. Todo lo demás —estilo, icono, convertirla en tarea, fecha,
 * aviso, pasos, asignatura— vive detrás de un solo pliegue. Una nota se crea
 * escribiendo y pulsando Guardar; el resto está ahí para el día que haga falta,
 * no ocupando la pantalla todos los días.
 */
@Composable
fun NoteEditorSheet(
    draft: NoteDraft?,
    subjects: List<Subject>,
    goals: List<Goal>,
    viewModel: NotesViewModel,
) {
    if (draft == null) return
    val colors = Rincon.colors
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newSubtask by remember { mutableStateOf("") }
    var expanded by remember(draft.isNew) { mutableStateOf(!draft.isNew) }

    val chevronTurn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Rincon.motion.gentle(),
        label = "chevron",
    )

    CozySheet(
        visible = true,
        onDismiss = viewModel::dismissEditor,
        footer = {
            Row(
                modifier = Modifier.padding(horizontal = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                if (!draft.isNew) {
                    GhostButton(
                        label = "Borrar",
                        icon = RinconIcons.Trash,
                        tint = colors.danger,
                        onClick = viewModel::deleteDraft,
                    )
                }
                PrimaryButton(
                    label = "Guardar",
                    icon = RinconIcons.Check,
                    onClick = viewModel::saveDraft,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {

            Column(Modifier.padding(horizontal = Space.xl)) {
                CozyTextField(
                    value = draft.note.text,
                    onValueChange = { text -> viewModel.editNote { it.copy(text = text) } },
                    placeholder = "¿Qué quieres recordar?",
                    minLines = 3,
                    textStyle = Rincon.type.note,
                    autoFocus = draft.isNew,
                )
            }

            Spacer(Modifier.height(Space.l))
            PaperColorPicker(
                selected = draft.note.colorKey,
                onSelect = { key -> viewModel.editNote { it.copy(colorKey = key) } },
            )

            Spacer(Modifier.height(Space.l))

            // El pliegue. Un solo control, y con estado visible.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable { expanded = !expanded }
                    .padding(horizontal = Space.xl, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "Menos opciones" else "Más opciones",
                    style = Rincon.type.label,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                RinconIcon(
                    RinconIcons.ChevronDown,
                    null,
                    tint = colors.textSecondary,
                    size = 20.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronTurn },
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(Rincon.motion.sizeSpring()) + fadeIn(Rincon.motion.fade()),
                exit = shrinkVertically(Rincon.motion.sizeSpring()) + fadeOut(Rincon.motion.quickFade()),
            ) {
                Column {
                    Column(Modifier.padding(start = Space.xl)) { FieldLabel("Papel") }
                    PaperStylePicker(
                        selectedStyle = draft.note.styleKey,
                        paperColorKey = draft.note.colorKey,
                        onSelect = { key -> viewModel.editNote { it.copy(styleKey = key) } },
                    )

                    Spacer(Modifier.height(Space.l))
                    Column(Modifier.padding(start = Space.xl)) { FieldLabel("Icono") }
                    IconPicker(
                        selected = draft.note.iconKey,
                        onSelect = { key -> viewModel.editNote { it.copy(iconKey = key) } },
                    )

                    Spacer(Modifier.height(Space.l))
                    Column(Modifier.padding(horizontal = Space.xl)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Convertir en tarea",
                                style = Rincon.type.bodyStrong,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            CozySwitch(
                                checked = draft.note.isTask,
                                onCheckedChange = { value -> viewModel.editNote { it.copy(isTask = value) } },
                            )
                        }

                        AnimatedVisibility(
                            visible = draft.note.isTask,
                            enter = expandVertically(Rincon.motion.sizeSpring()) + fadeIn(Rincon.motion.fade()),
                            exit = shrinkVertically(Rincon.motion.sizeSpring()) + fadeOut(Rincon.motion.quickFade()),
                        ) {
                            Column {
                                Spacer(Modifier.height(Space.m))
                                PriorityPicker(
                                    selected = draft.note.priority,
                                    onSelect = { value -> viewModel.editNote { it.copy(priority = value) } },
                                )
                            }
                        }

                        Spacer(Modifier.height(Space.l))
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                            GhostButton(
                                label = draft.note.dueDate?.let { Dates.shortDate(it) } ?: "Sin fecha",
                                onClick = { showDatePicker = true },
                                icon = RinconIcons.Calendar,
                                modifier = Modifier.weight(1f),
                            )
                            GhostButton(
                                label = draft.note.dueTime?.let { Dates.time(it) } ?: "Sin hora",
                                onClick = { showTimePicker = true },
                                icon = RinconIcons.Clock,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AnimatedVisibility(
                            visible = draft.note.dueDate != null && draft.note.dueTime != null,
                            enter = expandVertically(Rincon.motion.sizeSpring()) + fadeIn(Rincon.motion.fade()),
                            exit = shrinkVertically(Rincon.motion.sizeSpring()) + fadeOut(Rincon.motion.quickFade()),
                        ) {
                            Column {
                                Spacer(Modifier.height(Space.l))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RinconIcon(RinconIcons.Bell, null, tint = colors.textSecondary, size = 20.dp)
                                    Spacer(Modifier.width(Space.m))
                                    Text(
                                        "Avisarme",
                                        style = Rincon.type.bodyStrong,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    CozySwitch(
                                        checked = draft.reminderEnabled,
                                        onCheckedChange = { value ->
                                            viewModel.editDraft { it.copy(reminderEnabled = value) }
                                        },
                                    )
                                }

                                AnimatedVisibility(
                                    visible = draft.reminderEnabled,
                                    enter = expandVertically(Rincon.motion.sizeSpring()) + fadeIn(Rincon.motion.fade()),
                                    exit = shrinkVertically(Rincon.motion.sizeSpring()) + fadeOut(Rincon.motion.quickFade()),
                                ) {
                                    Column {
                                        Spacer(Modifier.height(Space.m))
                                        StepperRow(
                                            label = "Antes",
                                            value = draft.leadMinutes,
                                            onChange = { value -> viewModel.editDraft { it.copy(leadMinutes = value) } },
                                            range = 0..120,
                                            suffix = " min",
                                            step = 5,
                                        )
                                        Spacer(Modifier.height(Space.m))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                                        ) {
                                            listOf(RepeatRule.Once, RepeatRule.Daily, RepeatRule.Weekly).forEach { rule ->
                                                CozyChip(
                                                    label = rule.label,
                                                    selected = draft.repeat == rule,
                                                    onClick = { viewModel.editDraft { it.copy(repeat = rule) } },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(Space.l))
                        FieldLabel("Pasos")
                        draft.subtasks.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NoteCheckbox(
                                    checked = subtask.done,
                                    ink = colors.textPrimary,
                                    onToggle = { viewModel.toggleDraftSubtask(index) },
                                )
                                Spacer(Modifier.width(Space.m))
                                Text(
                                    subtask.text,
                                    style = Rincon.type.body,
                                    color = colors.textPrimary.copy(alpha = if (subtask.done) 0.55f else 1f),
                                    textDecoration = if (subtask.done) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    Modifier
                                        .pressable(hapticOnPress = false) { viewModel.removeDraftSubtask(index) }
                                        .padding(Space.s)
                                ) {
                                    RinconIcon(RinconIcons.Close, "Quitar paso", tint = colors.textMuted, size = 18.dp)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                CozyTextField(
                                    value = newSubtask,
                                    onValueChange = { newSubtask = it },
                                    placeholder = "Añadir un paso...",
                                    singleLine = true,
                                    imeAction = ImeAction.Done,
                                )
                            }
                            Spacer(Modifier.width(Space.s))
                            GhostButton(
                                label = "Añadir",
                                icon = RinconIcons.Plus,
                                onClick = {
                                    viewModel.addSubtask(newSubtask)
                                    newSubtask = ""
                                },
                            )
                        }

                        if (subjects.isNotEmpty() || goals.isNotEmpty()) {
                            Spacer(Modifier.height(Space.l))
                            FieldLabel("Relacionar con")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(Space.s),
                            ) {
                                CozyChip(
                                    label = "Nada",
                                    selected = draft.note.subjectId == null && draft.note.goalId == null,
                                    onClick = { viewModel.editNote { it.copy(subjectId = null, goalId = null) } },
                                )
                                subjects.forEach { subject ->
                                    CozyChip(
                                        label = subject.name,
                                        selected = draft.note.subjectId == subject.id,
                                        icon = RinconIcons.byKey(subject.iconKey),
                                        onClick = {
                                            viewModel.editNote { it.copy(subjectId = subject.id, goalId = null) }
                                        },
                                    )
                                }
                                goals.forEach { goal ->
                                    CozyChip(
                                        label = goal.title,
                                        selected = draft.note.goalId == goal.id,
                                        icon = RinconIcons.byKey(goal.iconKey),
                                        onClick = {
                                            viewModel.editNote { it.copy(goalId = goal.id, subjectId = null) }
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Space.s))
                    }
                }
            }
        }
    }

    DatePickerSheet(
        visible = showDatePicker,
        initial = draft.note.dueDate,
        onDismiss = { showDatePicker = false },
        onPick = { date ->
            viewModel.editNote { it.copy(dueDate = date) }
            if (date != null && draft.note.dueTime != null) {
                viewModel.editDraft { it.copy(reminderEnabled = true) }
            }
            showDatePicker = false
        },
    )

    TimePickerSheet(
        visible = showTimePicker,
        initial = draft.note.dueTime,
        onDismiss = { showTimePicker = false },
        onPick = { time ->
            viewModel.editNote { it.copy(dueTime = time) }
            // Poner hora es, casi siempre, pedir que te avisen.
            if (time != null) {
                viewModel.editDraft { it.copy(reminderEnabled = true) }
                if (draft.note.dueDate == null) {
                    viewModel.editNote { it.copy(dueDate = Dates.today()) }
                }
            }
            showTimePicker = false
        },
    )
}
