package com.rincon.espacio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.ui.icons.RinconIcons
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Rejilla mensual reutilizable: la usa tanto el calendario como el selector de
 * fecha del editor de notas, para que la forma de elegir un día sea siempre la
 * misma.
 */
@Composable
fun MonthGrid(
    month: YearMonth,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = Dates.today(),
    markers: Map<LocalDate, List<Color>> = emptyMap(),
) {
    val colors = Rincon.colors
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - 1)
    val daysInMonth = month.lengthOfMonth()
    val cells = leading + daysInMonth
    val rows = (cells + 6) / 7

    Column(modifier.fillMaxWidth()) {
        // 2024-01-01 fue lunes: sirve de ancla para los nombres de los días.
        val mondayAnchor = remember { LocalDate.of(2024, 1, 1) }
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { index ->
                val label = Dates.weekdayInitial(mondayAnchor.plusDays(index.toLong()))
                Text(
                    text = label,
                    style = Rincon.type.caption,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Space.s))
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayIndex = row * 7 + column - leading + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayIndex in 1..daysInMonth) {
                            val date = month.atDay(dayIndex)
                            DayCell(
                                date = date,
                                isSelected = date == selected,
                                isToday = date == today,
                                markers = markers[date].orEmpty(),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    markers: List<Color>,
    onClick: () -> Unit,
) {
    val colors = Rincon.colors
    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> colors.accent
            isToday -> colors.accentSoft
            else -> Color.Transparent
        },
        animationSpec = Rincon.motion.fade(),
        label = "day",
    )
    val fg = when {
        isSelected -> colors.accentInk
        isToday -> colors.accent
        else -> colors.textPrimary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (isToday && !isSelected) Modifier.border(1.5.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .pressable(hapticOnPress = false, onClick = onClick)
            .padding(vertical = 6.dp)
            .semantics { contentDescription = Dates.longDate(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = Rincon.type.bodyStrong,
            color = fg,
        )
        if (markers.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                markers.take(3).forEach { color ->
                    Dot(if (isSelected) colors.accentInk.copy(alpha = 0.8f) else color, size = 5.dp)
                }
            }
        }
    }
}

@Composable
fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Dates.monthTitle(month.atDay(1)),
            style = Rincon.type.section,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        RoundIconButton(RinconIcons.ChevronLeft, "Mes anterior", onPrevious)
        Spacer(Modifier.width(Space.s))
        RoundIconButton(RinconIcons.ChevronRight, "Mes siguiente", onNext)
    }
}

@Composable
fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Rincon.colors.surface,
    tint: Color = Rincon.colors.textSecondary,
) {
    Box(
        modifier = modifier
            .size(Space.touch)
            .softShadow(4.dp, Rincon.shapes.pill)
            .clip(Rincon.shapes.pill)
            .background(background)
            .border(1.dp, Rincon.colors.outlineSoft, Rincon.shapes.pill)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RinconIcon(icon, null, tint = tint, size = 22.dp)
    }
}

/** Panel para elegir día. */
@Composable
fun DatePickerSheet(
    visible: Boolean,
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPick: (LocalDate?) -> Unit,
) {
    val start = initial ?: Dates.today()
    var month by remember(visible) { mutableStateOf(YearMonth.from(start)) }
    var chosen by remember(visible) { mutableStateOf(initial) }

    CozySheet(
        visible = visible,
        onDismiss = onDismiss,
        footer = {
            Row(
                modifier = Modifier.padding(horizontal = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                GhostButton("Cancelar", onDismiss, Modifier.weight(1f))
                PrimaryButton("Guardar", { onPick(chosen) }, Modifier.weight(1f))
            }
        },
    ) {
        Column(Modifier.padding(horizontal = Space.xl)) {
            SheetTitle("¿Qué día?", modifier = Modifier.padding(horizontal = 0.dp))
            Spacer(Modifier.height(Space.l))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                CozyChip("Hoy", chosen == Dates.today(), { chosen = Dates.today() })
                CozyChip("Mañana", chosen == Dates.today().plusDays(1), { chosen = Dates.today().plusDays(1) })
                CozyChip("Sin fecha", chosen == null, { chosen = null })
            }
            Spacer(Modifier.height(Space.l))
            MonthHeader(
                month = month,
                onPrevious = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
            )
            Spacer(Modifier.height(Space.s))
            MonthGrid(
                month = month,
                selected = chosen,
                onSelect = { chosen = it },
            )
        }
    }
}

/** Panel para elegir hora: pasos cómodos y atajos, sin ruedas diminutas. */
@Composable
fun TimePickerSheet(
    visible: Boolean,
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onPick: (LocalTime?) -> Unit,
) {
    var hour by remember(visible) { mutableStateOf(initial?.hour ?: 9) }
    var minute by remember(visible) { mutableStateOf(initial?.minute ?: 0) }

    CozySheet(
        visible = visible,
        onDismiss = onDismiss,
        footer = {
            Row(
                modifier = Modifier.padding(horizontal = Space.xl),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                GhostButton("Quitar hora", { onPick(null) }, Modifier.weight(1f))
                PrimaryButton("Guardar", { onPick(LocalTime.of(hour, minute)) }, Modifier.weight(1f))
            }
        },
    ) {
        Column(Modifier.padding(horizontal = Space.xl)) {
            SheetTitle("¿A qué hora?", modifier = Modifier.padding(horizontal = 0.dp))
            Spacer(Modifier.height(Space.l))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(Rincon.shapes.card)
                    .background(Rincon.colors.surfaceSunken)
                    .padding(vertical = Space.l),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%02d:%02d".format(hour, minute),
                    style = Rincon.type.numeral,
                    color = Rincon.colors.textPrimary,
                )
            }
            Spacer(Modifier.height(Space.l))
            StepperRow("Hora", hour, { hour = (it + 24) % 24 }, -1..24)
            Spacer(Modifier.height(Space.s))
            StepperRow("Minutos", minute, { minute = (it + 60) % 60 }, -5..60, step = 5)
            Spacer(Modifier.height(Space.l))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                listOf(8 to 0, 12 to 0, 18 to 0, 21 to 30).forEach { (h, m) ->
                    CozyChip(
                        label = "%02d:%02d".format(h, m),
                        selected = hour == h && minute == m,
                        onClick = { hour = h; minute = m },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
