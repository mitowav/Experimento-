package com.rincon.espacio.ui.screens.study

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.SubjectDetail
import com.rincon.espacio.ui.components.CozySheet
import com.rincon.espacio.ui.components.CozySwitch
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.DatePickerSheet
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FieldLabel
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.IconPicker
import com.rincon.espacio.ui.components.NoteCheckbox
import com.rincon.espacio.ui.components.PaperColorPicker
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.PrimaryButton
import com.rincon.espacio.ui.components.ProgressTrack
import com.rincon.espacio.ui.components.RinconIcon
import com.rincon.espacio.ui.components.SheetTitle
import com.rincon.espacio.ui.components.StepperRow
import com.rincon.espacio.ui.components.TimePickerSheet
import com.rincon.espacio.ui.components.pressable
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.NotesViewModel
import com.rincon.espacio.ui.vm.StudyViewModel
import java.time.temporal.ChronoUnit

/**
 * Estudio.
 *
 * Cada asignatura enseña lo mismo que se necesita saber antes de un examen:
 * cuándo es, cómo va la preparación y qué queda por hacer.
 */
@Composable
fun StudyScreen(
    viewModel: StudyViewModel,
    notesViewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editingSubject by viewModel.editingSubject.collectAsStateWithLifecycle()
    val editingExam by viewModel.editingExam.collectAsStateWithLifecycle()
    val deskState by notesViewModel.state.collectAsStateWithLifecycle()
    val draft by notesViewModel.draft.collectAsStateWithLifecycle()
    val colors = Rincon.colors

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
                    Text("Estudio", style = Rincon.type.display, color = colors.textPrimary)
                    Text(
                        if (state.minutesThisWeek > 0)
                            "${state.minutesThisWeek} minutos esta semana"
                        else "Tus asignaturas y exámenes",
                        style = Rincon.type.body,
                        color = colors.textSecondary,
                    )
                }
            }

            if (state.subjects.isEmpty()) {
                item {
                    PaperSurface(Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = RinconIcons.Book,
                            title = "Sin asignaturas",
                            message = "Crea una y añade sus exámenes y tareas.",
                            action = {
                                PrimaryButton("Nueva asignatura", viewModel::newSubject, icon = RinconIcons.Plus)
                            },
                        )
                    }
                }
            }

            items(state.subjects, key = { it.subject.id }) { detail ->
                SubjectCard(
                    detail = detail,
                    onEditSubject = { viewModel.editSubject(detail.subject) },
                    onNewExam = { viewModel.newExam(detail.subject.id) },
                    onNewTask = { notesViewModel.startNewTask(null, subjectId = detail.subject.id) },
                    onToggleTask = { id, done -> viewModel.toggleTask(id, done) },
                    onOpenTask = { notesViewModel.openNote(it) },
                    onEditExam = { viewModel.editExam(it) },
                    onReadiness = { exam, value -> viewModel.setReadiness(exam, value) },
                )
            }
        }

        FloatingAddButton(
            onClick = viewModel::newSubject,
            description = "Nueva asignatura",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.l),
        )
    }

    val subject = editingSubject
    if (subject != null) {
        CozySheet(
            visible = true,
            onDismiss = viewModel::dismissSubject,
            footer = {
                Row(
                    modifier = Modifier.padding(horizontal = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    if (subject.id != 0L) {
                        GhostButton(
                            "Borrar",
                            { viewModel.deleteSubject(subject.id) },
                            icon = RinconIcons.Trash,
                            tint = colors.danger,
                        )
                    }
                    PrimaryButton(
                        "Guardar",
                        viewModel::saveSubject,
                        Modifier.weight(1f),
                        icon = RinconIcons.Check,
                        enabled = subject.name.isNotBlank(),
                    )
                }
            },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SheetTitle(if (subject.id == 0L) "Nueva asignatura" else "Editar asignatura")
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(horizontal = Space.xl)) {
                    CozyTextField(
                        value = subject.name,
                        onValueChange = { text -> viewModel.updateSubject { it.copy(name = text) } },
                        placeholder = "Nombre (Matemáticas, Historia...)",
                        singleLine = true,
                    )
                    Spacer(Modifier.height(Space.m))
                    CozyTextField(
                        value = subject.teacher,
                        onValueChange = { text -> viewModel.updateSubject { it.copy(teacher = text) } },
                        placeholder = "Profesor o aula (opcional)",
                        singleLine = true,
                    )
                    Spacer(Modifier.height(Space.l))
                    FieldLabel("Color")
                }
                PaperColorPicker(subject.colorKey, onSelect = { key -> viewModel.updateSubject { it.copy(colorKey = key) } })
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(start = Space.xl)) { FieldLabel("Icono") }
                IconPicker(subject.iconKey, onSelect = { key -> viewModel.updateSubject { it.copy(iconKey = key) } })
                Spacer(Modifier.height(Space.s))
            }
        }
    }

    val exam = editingExam
    if (exam != null) {
        var showDate by remember(exam.id) { mutableStateOf(false) }
        var showTime by remember(exam.id) { mutableStateOf(false) }
        var remind by remember(exam.id) { mutableStateOf(exam.reminderId != null) }
        CozySheet(
            visible = true,
            onDismiss = viewModel::dismissExam,
            footer = {
                Row(
                    modifier = Modifier.padding(horizontal = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    if (exam.id != 0L) {
                        GhostButton(
                            "Borrar",
                            { viewModel.deleteExam(exam.id) },
                            icon = RinconIcons.Trash,
                            tint = colors.danger,
                        )
                    }
                    PrimaryButton(
                        "Guardar",
                        { viewModel.saveExam(remind) },
                        Modifier.weight(1f),
                        icon = RinconIcons.Check,
                    )
                }
            },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SheetTitle(if (exam.id == 0L) "Nuevo examen" else "Editar examen")
                Spacer(Modifier.height(Space.l))
                Column(Modifier.padding(horizontal = Space.xl)) {
                    CozyTextField(
                        value = exam.title,
                        onValueChange = { text -> viewModel.updateExam { it.copy(title = text) } },
                        placeholder = "Tema o título",
                        singleLine = true,
                    )
                    Spacer(Modifier.height(Space.l))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        GhostButton(
                            label = Dates.shortDate(exam.date),
                            icon = RinconIcons.Calendar,
                            onClick = { showDate = true },
                            modifier = Modifier.weight(1f),
                        )
                        GhostButton(
                            label = exam.time?.let { Dates.time(it) } ?: "Sin hora",
                            icon = RinconIcons.Clock,
                            onClick = { showTime = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(Space.l))
                    StepperRow(
                        label = "Preparación",
                        value = exam.readiness,
                        onChange = { value -> viewModel.updateExam { it.copy(readiness = value) } },
                        range = 0..100,
                        suffix = " %",
                        step = 10,
                    )
                    Spacer(Modifier.height(Space.m))
                    ProgressTrack(exam.readiness / 100f)
                    Spacer(Modifier.height(Space.l))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Avisarme una hora antes", style = Rincon.type.bodyStrong, color = colors.textPrimary)
                            Text(
                                if (exam.time == null) "Necesita una hora" else "Notificación el día del examen",
                                style = Rincon.type.caption,
                                color = colors.textSecondary,
                            )
                        }
                        CozySwitch(checked = remind && exam.time != null, onCheckedChange = { remind = it })
                    }
                    Spacer(Modifier.height(Space.s))
                }
            }
        }
        DatePickerSheet(
            visible = showDate,
            initial = exam.date,
            onDismiss = { showDate = false },
            onPick = { date ->
                date?.let { picked -> viewModel.updateExam { it.copy(date = picked) } }
                showDate = false
            },
        )
        TimePickerSheet(
            visible = showTime,
            initial = exam.time,
            onDismiss = { showTime = false },
            onPick = { time ->
                viewModel.updateExam { it.copy(time = time) }
                showTime = false
            },
        )
    }

    com.rincon.espacio.ui.screens.canvas.NoteEditorSheet(
        draft = draft,
        subjects = deskState.subjects,
        goals = deskState.goals,
        viewModel = notesViewModel,
    )
}

@Composable
private fun SubjectCard(
    detail: SubjectDetail,
    onEditSubject: () -> Unit,
    onNewExam: () -> Unit,
    onNewTask: () -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onOpenTask: (Long) -> Unit,
    onEditExam: (com.rincon.espacio.domain.model.Exam) -> Unit,
    onReadiness: (com.rincon.espacio.domain.model.Exam, Int) -> Unit,
) {
    val colors = Rincon.colors
    val paper = PaperColor.fromKey(detail.subject.colorKey)
    val accent = paper.edge(colors.isDark)

    PaperSurface(Modifier.fillMaxWidth(), elevation = 7.dp) {
        Column(Modifier.padding(Space.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(paper.paper(colors.isDark)),
                    contentAlignment = Alignment.Center,
                ) {
                    RinconIcon(
                        RinconIcons.byKey(detail.subject.iconKey),
                        null,
                        tint = paper.ink(colors.isDark),
                        size = 23.dp,
                    )
                }
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f).pressable(onClick = onEditSubject)) {
                    Text(
                        detail.subject.name.uppercase(),
                        style = Rincon.type.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                    if (detail.subject.teacher.isNotBlank()) {
                        Text(detail.subject.teacher, style = Rincon.type.caption, color = colors.textSecondary)
                    }
                }
                if (detail.minutesThisWeek > 0) {
                    Text(
                        "${detail.minutesThisWeek} min",
                        style = Rincon.type.caption,
                        color = colors.textSecondary,
                    )
                }
            }

            val exam = detail.nextExam
            if (exam != null) {
                Spacer(Modifier.height(Space.m))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(Rincon.shapes.chip)
                        .background(colors.surfaceSunken)
                        .pressable(onClick = { onEditExam(exam) })
                        .padding(Space.m)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RinconIcon(RinconIcons.Graduation, null, tint = colors.accent, size = 20.dp)
                        Spacer(Modifier.width(Space.s))
                        Column(Modifier.weight(1f)) {
                            Text(exam.title, style = Rincon.type.bodyStrong, color = colors.textPrimary, maxLines = 1)
                            Text(
                                buildString {
                                    append(Dates.longDate(exam.date))
                                    exam.time?.let { append(" · ").append(Dates.time(it)) }
                                },
                                style = Rincon.type.caption,
                                color = colors.textSecondary,
                            )
                        }
                        val days = ChronoUnit.DAYS.between(Dates.today(), exam.date)
                        Text(
                            when {
                                days == 0L -> "Hoy"
                                days == 1L -> "Mañana"
                                else -> "En $days días"
                            },
                            style = Rincon.type.label,
                            color = if (days <= 3) colors.accent else colors.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(Space.s))
                    Text("Preparación", style = Rincon.type.caption, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    ProgressTrack(exam.readiness / 100f, fillColor = accent)
                    Spacer(Modifier.height(Space.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        listOf(25, 50, 75, 100).forEach { value ->
                            com.rincon.espacio.ui.components.CozyChip(
                                label = "$value%",
                                selected = exam.readiness == value,
                                onClick = { onReadiness(exam, value) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (detail.tasks.isNotEmpty()) {
                Spacer(Modifier.height(Space.m))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tareas", style = Rincon.type.label, color = colors.textSecondary, modifier = Modifier.weight(1f))
                    Text(
                        "${(detail.taskProgress * 100).toInt()}%",
                        style = Rincon.type.label,
                        color = colors.textSecondary,
                    )
                }
                Spacer(Modifier.height(Space.xs))
                ProgressTrack(detail.taskProgress, height = 8.dp, fillColor = accent)
                Spacer(Modifier.height(Space.s))
                detail.tasks.take(5).forEach { task ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressable(hapticOnPress = false) { onOpenTask(task.id) }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NoteCheckbox(task.done, colors.textPrimary, { onToggleTask(task.id, !task.done) })
                        Spacer(Modifier.width(Space.m))
                        Text(
                            task.title.ifBlank { "Sin título" },
                            style = Rincon.type.body,
                            color = colors.textPrimary.copy(alpha = if (task.done) 0.55f else 1f),
                            textDecoration = if (task.done)
                                androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.m))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                GhostButton("Examen", onNewExam, icon = RinconIcons.Graduation, modifier = Modifier.weight(1f))
                GhostButton("Tarea", onNewTask, icon = RinconIcons.Plus, modifier = Modifier.weight(1f))
            }
        }
    }
}
