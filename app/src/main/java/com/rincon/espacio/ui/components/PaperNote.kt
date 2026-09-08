package com.rincon.espacio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.ui.icons.RinconIcons

/** Tamaño base de una nota en el escritorio. */
val NoteWidth = 164.dp

/**
 * El papel.
 *
 * Se dibuja como un objeto físico: color de papel, canto ligeramente más
 * oscuro (como el grosor del papel), una esquina doblada y un brillo muy sutil
 * en la parte superior. Nada de esto se anima por su cuenta: el movimiento lo
 * aporta el gesto.
 */
@Composable
fun PaperNote(
    item: NoteWithSubtasks,
    modifier: Modifier = Modifier,
    onToggleDone: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val colors = Rincon.colors
    val dark = colors.isDark
    val paper = PaperColor.fromKey(item.note.colorKey)
    val bg = paper.paper(dark)
    val ink = paper.ink(dark)
    val edge = paper.edge(dark)
    val shape = Rincon.shapes.note

    val doneAlpha by animateFloatAsState(
        targetValue = if (item.note.done) 0.62f else 1f,
        animationSpec = Rincon.motion.gentle(),
        label = "noteDone",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        bg.lighten(if (dark) 0.06f else 0.05f),
                        bg,
                        bg.darken(0.03f),
                    )
                )
            )
            .border(1.dp, edge.copy(alpha = 0.75f), shape)
            .drawBehind {
                // Esquina doblada: el detalle que hace que se lea como papel.
                val fold = 18.dp.toPx()
                val path = Path().apply {
                    moveTo(size.width - fold, 0f)
                    lineTo(size.width, fold)
                    lineTo(size.width - fold, fold)
                    close()
                }
                drawPath(path, edge.copy(alpha = 0.55f))
            }
            .padding(Space.m),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ink.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    RinconIcon(
                        icon = RinconIcons.byKey(item.note.iconKey),
                        contentDescription = RinconIcons.labelFor(item.note.iconKey),
                        tint = ink.copy(alpha = 0.85f),
                        size = 17.dp,
                    )
                }
                Spacer(Modifier.width(Space.s))
                if (item.note.priority == Priority.High) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ink.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(Space.xs))
                    Text(
                        "Importante",
                        style = Rincon.type.caption,
                        color = ink.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (item.note.isTask && onToggleDone != null) {
                    NoteCheckbox(
                        checked = item.note.done,
                        ink = ink,
                        onToggle = onToggleDone,
                    )
                }
            }

            Spacer(Modifier.height(Space.s))

            Text(
                text = item.note.text.ifBlank { "Nota sin texto" },
                style = Rincon.type.note,
                color = ink.copy(alpha = doneAlpha),
                fontStyle = if (item.note.text.isBlank()) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (item.note.done) TextDecoration.LineThrough else null,
                maxLines = if (compact) 3 else 7,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.subtasks.isNotEmpty()) {
                Spacer(Modifier.height(Space.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressTrack(
                        progress = item.progress,
                        modifier = Modifier.weight(1f),
                        height = 6.dp,
                        trackColor = ink.copy(alpha = 0.14f),
                        fillColor = ink.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.width(Space.s))
                    Text(
                        "${item.subtasks.count { it.done }}/${item.subtasks.size}",
                        style = Rincon.type.caption,
                        color = ink.copy(alpha = 0.7f),
                    )
                }
            }

            val note = item.note
            if (note.dueDate != null) {
                Spacer(Modifier.height(Space.s))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    RinconIcon(
                        icon = if (note.reminderId != null) RinconIcons.Bell else RinconIcons.Calendar,
                        contentDescription = if (note.reminderId != null) "Con recordatorio" else "Con fecha",
                        tint = ink.copy(alpha = 0.7f),
                        size = 15.dp,
                    )
                    Text(
                        text = buildString {
                            append(Dates.relative(note.dueDate))
                            note.dueTime?.let { append(" · ").append(Dates.time(it)) }
                        },
                        style = Rincon.type.caption,
                        color = ink.copy(alpha = 0.75f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Casilla propia: el check se dibuja con un muelle, no aparece de golpe. */
@Composable
fun NoteCheckbox(
    checked: Boolean,
    ink: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(9.dp)
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = Rincon.motion.playful(),
        label = "check",
    )
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(shape)
            .background(ink.copy(alpha = 0.08f + 0.22f * fill))
            .border(1.5.dp, ink.copy(alpha = 0.35f + 0.35f * fill), shape)
            .pressable(hapticOnPress = false, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(Rincon.motion.playful()) + fadeIn(Rincon.motion.quickFade()),
            exit = scaleOut(Rincon.motion.snappy()) + fadeOut(Rincon.motion.quickFade()),
        ) {
            RinconIcon(RinconIcons.Check, null, tint = ink, size = 18.dp)
        }
    }
}

internal fun Color.lighten(amount: Float): Color = Color(
    red = (red + (1f - red) * amount).coerceIn(0f, 1f),
    green = (green + (1f - green) * amount).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
    alpha = alpha,
)

internal fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)

internal fun offsetOf(x: Float, y: Float) = Offset(x, y)
