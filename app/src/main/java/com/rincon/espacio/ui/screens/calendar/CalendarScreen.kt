package com.rincon.espacio.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.CalendarEvent
import com.rincon.espacio.domain.model.DayItem
import com.rincon.espacio.ui.components.CozyChip
import com.rincon.espacio.ui.components.CozySheet
import com.rincon.espacio.ui.components.CozySwitch
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.DatePickerSheet
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FieldLabel
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.IconPicker
import com.rincon.espacio.ui.components.MonthGrid
import com.rincon.espacio.ui.components.MonthHeader
import com.rincon.espacio.ui.components.PaperColorPicker
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.PrimaryButton
import com.rincon.espacio.ui.components.RinconIcon
import com.rincon.espacio.ui.components.SectionHeader
import com.rincon.espacio.ui.components.SegmentedControl
import com.rincon.espacio.ui.components.SheetTitle
import com.rincon.espacio.ui.components.TimePickerSheet
import com.rincon.espacio.ui.components.pressable
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.screens.home.DayItemRow
import com.rincon.espacio.ui.vm.CalendarMode
import com.rincon.espacio.ui.vm.CalendarViewModel
import com.rincon.espacio.ui.vm.NotesViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Calendario deliberadamente simple: día, semana o mes, y debajo lo que hay.
 *
 * Los eventos con hora se pueden reprogramar arrastrando su tirador arriba o
 * abajo (15 minutos por paso, con un toque háptico en cada salto). El gesto
 * tiene alternativa visible: tocar el evento abre su editor.
 */
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    notesViewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }

    val markers = remember(state.events, state.tasks) {
        buildMap<LocalDate, MutableList<androidx.compose.ui.graphics.Color>> {
            state.events.forEach { event ->
                getOrPut(event.date) { mutableListOf() }
                    .add(PaperColor.fromKey(event.colorKey).edge(colors.isDark))
            }
            state.tasks.forEach { task ->
                task.dueDate?.let { date ->
                    getOrPut(date) { mutableListOf() }
                        .add(PaperColor.fromKey(task.colorKey).edge(colors.isDark))
                }
            }
        }.mapValues { it.value.toList() }
    }

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
                Column(Modifier.statusBarsPadding().padding(top = Space.l)) {
                    Text("Calendario", style = Rincon.type.display, color = colors.textPrimary)
                    Spacer(Modifier.height(Space.m))
                    SegmentedControl(
                        options = CalendarMode.entries.toList(),
                        selected = state.mode,
                        label = { it.label },
                        onSelect = viewModel::setMode,
                    )
                }
            }

            item {
                PaperSurface(Modifier.fillMaxWidth(), elevation = 8.dp) {
                    Column(Modifier.padding(Space.l)) {
                        when (state.mode) {
                            CalendarMode.Month -> {
                                MonthHeader(
                                    month = state.month,
                                    onPrevious = { viewModel.shiftMonth(-1) },
                                    onNext = { viewModel.shiftMonth(1) },
                                )
                                Spacer(Modifier.height(Space.m))
                                MonthGrid(
                                    month = state.month,
                                    selected = state.selected,
                                    onSelect = viewModel::select,
                                    markers = markers,
                                )
                            }
                            CalendarMode.Week -> WeekStrip(
                                selected = state.selected,
                                markers = markers,
                                onSelect = viewModel::select,
                            )
                            CalendarMode.Day -> DayHeader(
                                date = state.selected,
                                onShift = { viewModel.select(state.selected.plusDays(it)) },
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = Dates.relative(state.selected),
                    trailing = {
                        Text(
                            Dates.longDate(state.selected),
                            style = Rincon.type.caption,
                            color = colors.textSecondary,
                        )
                    },
                )
            }

            val items = state.plan.upcoming
            if (items.isEmpty()) {
                item {
                    PaperSurface(Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = RinconIcons.Calendar,
                            title = "Este día está libre",
                            message = "Añade un evento o mueve algo aquí.",
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    when (item) {
                        is DayItem.EventItem -> EventRow(
                            event = item.event,
                            onOpen = { editing = item.event },
                            onReschedule = { newTime ->
                                viewModel.moveEvent(item.event.id, item.event.date, newTime)
                            },
                        )
                        is DayItem.TaskItem -> com.rincon.espacio.ui.components.TaskRow(
                            note = item.note,
                            subtaskProgress = item.subtaskProgress,
                            onToggle = { viewModel.toggleTask(item.note.id, !item.note.done) },
                            onOpen = { notesViewModel.openNote(item.note.id) },
                        )
                        else -> DayItemRow(item)
                    }
                }
            }
        }

        FloatingAddButton(
            onClick = {
                editing = CalendarEvent(
                    title = "",
                    date = state.selected,
                    startTime = LocalTime.of(17, 0),
                )
            },
            description = "Crear evento",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.l),
        )
    }

    EventEditorSheet(
        event = editing,
        onDismiss = { editing = null },
        onSave = { event, remind, lead ->
            viewModel.saveEvent(event, remind, lead)
            editing = null
        },
        onDelete = { id ->
            viewModel.deleteEvent(id)
            editing = null
        },
    )
}

@Composable
private fun DayHeader(date: LocalDate, onShift: (Long) -> Unit) {
    val colors = Rincon.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        com.rincon.espacio.ui.components.RoundIconButton(
            RinconIcons.ChevronLeft, "Día anterior", onClick = { onShift(-1) },
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.dayOfMonth.toString(), style = Rincon.type.numeral, color = colors.textPrimary)
            Text(Dates.weekdayShort(date), style = Rincon.type.body, color = colors.textSecondary)
        }
        com.rincon.espacio.ui.components.RoundIconButton(
            RinconIcons.ChevronRight, "Día siguiente", onClick = { onShift(1) },
        )
    }
}

@Composable
private fun WeekStrip(
    selected: LocalDate,
    markers: Map<LocalDate, List<androidx.compose.ui.graphics.Color>>,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = Rincon.colors
    val start = Dates.weekStart(selected)
    val today = Dates.today()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.rincon.espacio.ui.components.RoundIconButton(
                RinconIcons.ChevronLeft, "Semana anterior",
                onClick = { onSelect(selected.minusWeeks(1)) },
            )
            Text(
                text = "${Dates.shortDate(start)} – ${Dates.shortDate(start.plusDays(6))}",
                style = Rincon.type.cardTitle,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            com.rincon.espacio.ui.components.RoundIconButton(
                RinconIcons.ChevronRight, "Semana siguiente",
                onClick = { onSelect(selected.plusWeeks(1)) },
            )
        }
        Spacer(Modifier.height(Space.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..6).forEach { index ->
                val date = start.plusDays(index.toLong())
                val isSelected = date == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isSelected -> colors.accent
                                date == today -> colors.accentSoft
                                else -> colors.surfaceSunken
                            }
                        )
                        .pressable(hapticOnPress = false) { onSelect(date) }
                        .padding(vertical = Space.s)
                        .semantics { contentDescription = Dates.longDate(date) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        Dates.weekdayInitial(date),
                        style = Rincon.type.caption,
                        color = if (isSelected) colors.accentInk else colors.textMuted,
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        style = Rincon.type.bodyStrong,
                        color = if (isSelected) colors.accentInk else colors.textPrimary,
                    )
                    val dots = markers[date].orEmpty()
                    if (dots.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            dots.take(3).forEach {
                                com.rincon.espacio.ui.components.Dot(
                                    if (isSelected) colors.accentInk else it,
                                    size = 4.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Evento con tirador para reprogramar arrastrando. */
@Composable
private fun EventRow(
    event: CalendarEvent,
    onOpen: () -> Unit,
    onReschedule: (LocalTime) -> Unit,
) {
    val colors = Rincon.colors
    val density = LocalDensity.current
    val feedback = LocalFeedback.current
    val paper = PaperColor.fromKey(event.colorKey)
    val stepPx = with(density) { 14.dp.toPx() }
    var dragOffset by remember(event.id, event.startTime) { mutableStateOf(0f) }
    val baseTime = event.startTime
    val previewTime = remember(dragOffset, baseTime) {
        baseTime?.plusMinutes(((dragOffset / stepPx).roundToInt() * 15).toLong())
    }

    PaperSurface(Modifier.fillMaxWidth(), elevation = 5.dp) {
        Row(
            Modifier.fillMaxWidth().padding(Space.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(paper.paper(colors.isDark)),
                contentAlignment = Alignment.Center,
            ) {
                RinconIcon(
                    RinconIcons.byKey(event.iconKey),
                    null,
                    tint = paper.ink(colors.isDark),
                    size = 22.dp,
                )
            }
            Spacer(Modifier.width(Space.m))
            Column(
                Modifier
                    .weight(1f)
                    .pressable(onClick = onOpen)
            ) {
                Text(event.title, style = Rincon.type.bodyStrong, color = colors.textPrimary, maxLines = 1)
                Text(
                    text = previewTime?.let { Dates.time(it) } ?: "Todo el día",
                    style = Rincon.type.caption,
                    color = if (dragOffset != 0f) colors.accent else colors.textSecondary,
                )
            }
            if (baseTime != null) {
                Box(
                    modifier = Modifier
                        .size(Space.touch)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceSunken)
                        .pointerInput(event.id, baseTime) {
                            var lastStep = 0
                            detectVerticalDragGestures(
                                onDragStart = { feedback.pop() },
                                onDragEnd = {
                                    previewTime?.let { if (it != baseTime) onReschedule(it) }
                                    dragOffset = 0f
                                },
                                onDragCancel = { dragOffset = 0f },
                            ) { change, amount ->
                                change.consume()
                                dragOffset += amount
                                val step = (dragOffset / stepPx).roundToInt()
                                if (step != lastStep) {
                                    lastStep = step
                                    feedback.tap()
                                }
                            }
                        }
                        .semantics { contentDescription = "Arrastra para cambiar la hora" },
                    contentAlignment = Alignment.Center,
                ) {
                    RinconIcon(RinconIcons.Grip, null, tint = colors.textMuted, size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun EventEditorSheet(
    event: CalendarEvent?,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent, Boolean, Int) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (event == null) return
    var draft by remember(event.id, event.date) { mutableStateOf(event) }
    var remind by remember(event.id) { mutableStateOf(event.reminderId != null) }
    var lead by remember(event.id) { mutableStateOf(10) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val colors = Rincon.colors

    CozySheet(visible = true, onDismiss = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(max = 620.dp)
                .padding(bottom = Space.s)
        ) {
            SheetTitle(if (event.id == 0L) "Nuevo evento" else "Editar evento")
            Spacer(Modifier.height(Space.l))
            Column(Modifier.padding(horizontal = Space.xl)) {
                CozyTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    placeholder = "¿Qué es?",
                    singleLine = true,
                )
                Spacer(Modifier.height(Space.l))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    GhostButton(
                        label = Dates.shortDate(draft.date),
                        icon = RinconIcons.Calendar,
                        onClick = { showDate = true },
                        modifier = Modifier.weight(1f),
                    )
                    GhostButton(
                        label = draft.startTime?.let { Dates.time(it) } ?: "Todo el día",
                        icon = RinconIcons.Clock,
                        onClick = { showTime = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(Space.l))
                FieldLabel("Color")
            }
            PaperColorPicker(draft.colorKey, onSelect = { draft = draft.copy(colorKey = it) })
            Spacer(Modifier.height(Space.l))
            Column(Modifier.padding(start = Space.xl)) { FieldLabel("Icono") }
            IconPicker(draft.iconKey, onSelect = { draft = draft.copy(iconKey = it) })

            Column(Modifier.padding(horizontal = Space.xl)) {
                Spacer(Modifier.height(Space.l))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisarme antes", style = Rincon.type.bodyStrong, color = colors.textPrimary)
                        Text(
                            if (draft.startTime == null) "Necesita una hora" else "$lead minutos antes",
                            style = Rincon.type.caption,
                            color = colors.textSecondary,
                        )
                    }
                    CozySwitch(
                        checked = remind && draft.startTime != null,
                        onCheckedChange = { remind = it },
                    )
                }
                if (remind && draft.startTime != null) {
                    Spacer(Modifier.height(Space.m))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        listOf(0, 10, 30, 60).forEach { minutes ->
                            CozyChip(
                                label = if (minutes == 0) "A la hora" else "$minutes min",
                                selected = lead == minutes,
                                onClick = { lead = minutes },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.xl))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    if (event.id != 0L) {
                        GhostButton(
                            label = "Borrar",
                            icon = RinconIcons.Trash,
                            tint = colors.danger,
                            onClick = { onDelete(event.id) },
                        )
                    }
                    PrimaryButton(
                        label = "Guardar",
                        icon = RinconIcons.Check,
                        onClick = { if (draft.title.isNotBlank()) onSave(draft, remind, lead) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    DatePickerSheet(
        visible = showDate,
        initial = draft.date,
        onDismiss = { showDate = false },
        onPick = { date ->
            date?.let { draft = draft.copy(date = it) }
            showDate = false
        },
    )
    TimePickerSheet(
        visible = showTime,
        initial = draft.startTime,
        onDismiss = { showTime = false },
        onPick = { time ->
            draft = draft.copy(startTime = time)
            showTime = false
        },
    )
}
