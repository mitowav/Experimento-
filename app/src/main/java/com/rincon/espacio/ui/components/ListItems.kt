package com.rincon.espacio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.GoalWithSteps
import com.rincon.espacio.domain.model.HabitWithChecks
import com.rincon.espacio.domain.model.Note
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.ui.icons.RinconIcons
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fila deslizable.
 *
 * Deslizar a la derecha completa, a la izquierda borra. El fondo se colorea de
 * forma progresiva y el icono crece conforme se acerca el umbral, para que el
 * gesto se entienda antes de soltarlo: nunca hay una acción invisible.
 */
@Composable
fun SwipeableRow(
    onSwipeRight: (() -> Unit)?,
    onSwipeLeft: (() -> Unit)?,
    modifier: Modifier = Modifier,
    rightIcon: ImageVector = RinconIcons.Check,
    leftIcon: ImageVector = RinconIcons.Trash,
    rightColor: Color = Rincon.colors.success,
    leftColor: Color = Rincon.colors.danger,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val motion = Rincon.motion
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val threshold = with(density) { 96.dp.toPx() }
    val maxDrag = with(density) { 150.dp.toPx() }

    val progress = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)
    val revealingRight = offsetX.value > 0

    Box(modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(Rincon.shapes.card)
                .background(
                    (if (revealingRight) rightColor else leftColor).copy(alpha = 0.16f + 0.5f * progress)
                )
                .padding(horizontal = Space.xl),
            contentAlignment = if (revealingRight) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            if (progress > 0.02f) {
                RinconIcon(
                    icon = if (revealingRight) rightIcon else leftIcon,
                    contentDescription = null,
                    tint = if (revealingRight) rightColor else leftColor,
                    size = (20 + 10 * progress).dp,
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(onSwipeRight, onSwipeLeft) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val value = offsetX.value
                            when {
                                value > threshold && onSwipeRight != null -> {
                                    feedback.complete()
                                    scope.launch { offsetX.animateTo(0f, motion.settle()) }
                                    onSwipeRight()
                                }
                                value < -threshold && onSwipeLeft != null -> {
                                    feedback.warn()
                                    scope.launch { offsetX.animateTo(0f, motion.settle()) }
                                    onSwipeLeft()
                                }
                                else -> scope.launch { offsetX.animateTo(0f, motion.gentle()) }
                            }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, motion.gentle()) } },
                    ) { change, dragAmount ->
                        change.consume()
                        val next = (offsetX.value + dragAmount).coerceIn(
                            if (onSwipeLeft != null) -maxDrag else 0f,
                            if (onSwipeRight != null) maxDrag else 0f,
                        )
                        scope.launch { offsetX.snapTo(next) }
                    }
                }
        ) {
            content()
        }
    }
}

/** Tarea del día: casilla grande, texto claro y metadatos discretos. */
@Composable
fun TaskRow(
    note: Note,
    subtaskProgress: Float?,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
) {
    val colors = Rincon.colors
    val paper = PaperColor.fromKey(note.colorKey)
    val accent = paper.edge(colors.isDark)
    val fade by animateFloatAsState(
        targetValue = if (note.done) 0.55f else 1f,
        animationSpec = Rincon.motion.gentle(),
        label = "taskFade",
    )

    PaperSurface(modifier = modifier.fillMaxWidth(), elevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(onClick = onOpen)
                .padding(Space.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
            Spacer(Modifier.width(Space.m))
            NoteCheckbox(checked = note.done, ink = colors.textPrimary, onToggle = onToggle)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RinconIcon(
                        RinconIcons.byKey(note.iconKey),
                        null,
                        tint = colors.textSecondary.copy(alpha = fade),
                        size = 17.dp,
                    )
                    Spacer(Modifier.width(Space.xs))
                    Text(
                        text = note.title.ifBlank { "Sin título" },
                        style = Rincon.type.bodyStrong,
                        color = colors.textPrimary.copy(alpha = fade),
                        textDecoration = if (note.done) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val meta = buildList {
                    note.dueTime?.let { add(Dates.time(it)) }
                    note.dueDate?.let { add(Dates.relative(it)) }
                    if (note.priority == Priority.High) add("Importante")
                    trailingLabel?.let { add(it) }
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        meta.joinToString(" · "),
                        style = Rincon.type.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (subtaskProgress != null && subtaskProgress > 0f) {
                    Spacer(Modifier.height(Space.s))
                    ProgressTrack(subtaskProgress, height = 5.dp, fillColor = accent)
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    item: GoalWithSteps,
    onOpen: () -> Unit,
    onToggleStep: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val paper = PaperColor.fromKey(item.goal.colorKey)
    val accent = paper.edge(colors.isDark)

    PaperSurface(modifier = modifier.fillMaxWidth(), elevation = 6.dp) {
        Column(Modifier.fillMaxWidth().pressable(onClick = onOpen).padding(Space.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(paper.paper(colors.isDark)),
                    contentAlignment = Alignment.Center,
                ) {
                    RinconIcon(
                        RinconIcons.byKey(item.goal.iconKey),
                        null,
                        tint = paper.ink(colors.isDark),
                        size = 22.dp,
                    )
                }
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f)) {
                    Text(item.goal.title, style = Rincon.type.cardTitle, color = colors.textPrimary, maxLines = 2)
                    val steps = item.steps
                    Text(
                        text = when {
                            steps.isEmpty() -> "Añade pequeños pasos"
                            else -> "${steps.count { it.done }} de ${steps.size} pasos"
                        },
                        style = Rincon.type.caption,
                        color = colors.textSecondary,
                    )
                }
                item.goal.targetDate?.let {
                    Text(Dates.relative(it), style = Rincon.type.caption, color = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(Space.m))
            ProgressTrack(item.progress, fillColor = accent)
            if (item.steps.isNotEmpty()) {
                Spacer(Modifier.height(Space.m))
                item.steps.take(3).forEach { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(hapticOnPress = false) { onToggleStep(step.id, !step.done) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NoteCheckbox(step.done, colors.textPrimary, { onToggleStep(step.id, !step.done) })
                        Spacer(Modifier.width(Space.m))
                        Text(
                            step.title,
                            style = Rincon.type.body,
                            color = colors.textPrimary.copy(alpha = if (step.done) 0.55f else 1f),
                            textDecoration = if (step.done) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (item.steps.size > 3) {
                    Text(
                        "y ${item.steps.size - 3} más",
                        style = Rincon.type.caption,
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}

/** Hábito: semana visible de un vistazo, racha discreta. */
@Composable
fun HabitCard(
    item: HabitWithChecks,
    weekStart: LocalDate,
    today: LocalDate,
    onToggle: (LocalDate, Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val paper = PaperColor.fromKey(item.habit.colorKey)
    val accent = paper.edge(colors.isDark)
    val streak = item.streak(today)

    PaperSurface(modifier = modifier.fillMaxWidth(), elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().padding(Space.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(paper.paper(colors.isDark)),
                    contentAlignment = Alignment.Center,
                ) {
                    RinconIcon(
                        RinconIcons.byKey(item.habit.iconKey),
                        null,
                        tint = paper.ink(colors.isDark),
                        size = 20.dp,
                    )
                }
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f).pressable(onClick = onOpen)) {
                    Text(item.habit.title, style = Rincon.type.cardTitle, color = colors.textPrimary, maxLines = 1)
                    Text(
                        item.habit.cadence.label,
                        style = Rincon.type.caption,
                        color = colors.textSecondary,
                    )
                }
                if (streak > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RinconIcon(RinconIcons.Flame, "Racha", tint = colors.warning, size = 18.dp)
                        Spacer(Modifier.width(2.dp))
                        Text("$streak", style = Rincon.type.label, color = colors.warning)
                    }
                }
            }
            Spacer(Modifier.height(Space.m))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (0..6).forEach { index ->
                    val date = weekStart.plusDays(index.toLong())
                    val done = item.isDone(date)
                    val enabled = !date.isAfter(today) && item.habit.appliesTo(date)
                    val scale by animateFloatAsState(
                        targetValue = if (done) 1f else 0.92f,
                        animationSpec = Rincon.motion.playful(),
                        label = "habitDay",
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            Dates.weekdayInitial(date),
                            style = Rincon.type.caption,
                            color = colors.textMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    when {
                                        done -> accent
                                        !enabled -> colors.surfaceSunken.copy(alpha = 0.5f)
                                        else -> colors.surfaceSunken
                                    }
                                )
                                .then(
                                    if (date == today) Modifier.androidxBorder(colors.accent) else Modifier
                                )
                                .pressable(enabled = enabled, hapticOnPress = false) {
                                    onToggle(date, !done)
                                }
                                .semantics {
                                    contentDescription =
                                        "${Dates.weekdayShort(date)} ${if (done) "hecho" else "pendiente"}"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) {
                                RinconIcon(
                                    RinconIcons.Check,
                                    null,
                                    tint = paper.ink(colors.isDark),
                                    size = 18.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.androidxBorder(color: Color): Modifier =
    this.border(1.5.dp, color.copy(alpha = 0.55f), RoundedCornerShape(13.dp))
