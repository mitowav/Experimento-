package com.rincon.espacio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.PaperStyle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.util.Dates
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.ui.icons.RinconIcons
import kotlin.math.sin
import kotlin.random.Random

/** Tamaño base de una nota en el escritorio. */
val NoteWidth = 168.dp

/**
 * El papel.
 *
 * Se dibuja como un objeto físico, en capas, de abajo arriba:
 *
 *  1. degradado del papel (más claro arriba, donde "da la luz");
 *  2. grano: puntitos y fibras a muy baja opacidad, con semilla fija por nota,
 *     de modo que cada papel tiene su textura y no cambia al redibujar;
 *  3. el estilo elegido (rayas, cuadrícula, cinta de post-it, fibras de
 *     reciclado);
 *  4. una línea de luz en el borde superior y una sombra interior abajo, que es
 *     lo que hace que se lea como una lámina con grosor y no como un rectángulo;
 *  5. la esquina doblada.
 *
 * El papel reciclado además se recorta de verdad: el borde inferior se rasga
 * con `BlendMode.Clear` sobre una capa aparte, así que no es un dibujo encima
 * sino una silueta irregular real.
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
    val style = PaperStyle.fromKey(item.note.styleKey)
    // Elegir un color en el editor no debería ser un corte: el papel se tiñe.
    val bg by animateColorAsState(paper.paper(dark), Rincon.motion.fade(), label = "paperBg")
    val ink by animateColorAsState(paper.ink(dark), Rincon.motion.fade(), label = "paperInk")
    val edge by animateColorAsState(paper.edge(dark), Rincon.motion.fade(), label = "paperEdge")
    val shape = Rincon.shapes.note
    val seed = item.note.id.takeIf { it != 0L } ?: item.note.createdAt

    val doneAlpha by animateFloatAsState(
        targetValue = if (item.note.done) 0.62f else 1f,
        animationSpec = Rincon.motion.gentle(),
        label = "noteDone",
    )

    val torn = style == PaperStyle.Recycled

    Box(
        modifier = modifier
            .then(
                // Recortar de verdad exige una capa propia sobre la que borrar.
                if (torn) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                else Modifier
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to bg.lighten(if (dark) 0.10f else 0.07f),
                    0.35f to bg,
                    1f to bg.darken(if (dark) 0.06f else 0.045f),
                )
            )
            .drawBehind {
                drawGrain(ink, seed, dense = torn)
                drawStyle(style, ink, edge, seed)
                drawDepth(ink)
                if (style != PaperStyle.Sticky) drawFold(edge, bg)
            }
            .then(
                if (torn) Modifier.drawWithContent {
                    drawContent()
                    tearBottomEdge(seed)
                } else Modifier
            )
            .then(
                if (torn) Modifier else Modifier.border(1.dp, edge.copy(alpha = 0.75f), shape)
            )
            .padding(
                start = Space.m,
                end = Space.m,
                top = if (style == PaperStyle.Sticky) Space.xl else Space.m,
                bottom = if (torn) Space.xl else Space.m,
            ),
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
                        maxLines = 1,
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
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// --- Capas de dibujo del papel -------------------------------------------------

private fun DrawScope.drawGrain(ink: Color, seed: Long, dense: Boolean) {
    val random = Random(seed)
    val dots = if (dense) 90 else 55
    val dotColor = ink.copy(alpha = 0.05f)
    repeat(dots) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        drawCircle(dotColor, radius = 0.9f + random.nextFloat() * 0.8f, center = Offset(x, y))
    }
}

private fun DrawScope.drawStyle(style: PaperStyle, ink: Color, edge: Color, seed: Long) {
    when (style) {
        PaperStyle.Plain -> Unit

        PaperStyle.Lined -> {
            val gap = 22.dp.toPx()
            val color = ink.copy(alpha = 0.13f)
            var y = gap * 2f
            while (y < size.height - 4f) {
                drawLine(color, Offset(10.dp.toPx(), y), Offset(size.width - 10.dp.toPx(), y), 1f)
                y += gap
            }
        }

        PaperStyle.Grid -> {
            val gap = 18.dp.toPx()
            val color = ink.copy(alpha = 0.09f)
            var y = gap
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
                y += gap
            }
            var x = gap
            while (x < size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
                x += gap
            }
        }

        PaperStyle.Sticky -> {
            // Franja adhesiva en la parte alta, un punto más oscura.
            drawRect(
                color = ink.copy(alpha = 0.05f),
                size = Size(size.width, 30.dp.toPx()),
            )
            // Y la cinta, ligeramente girada, como pegada a mano.
            val random = Random(seed)
            val tapeW = 54.dp.toPx()
            val tapeH = 17.dp.toPx()
            val angle = -7f + random.nextFloat() * 14f
            rotate(angle, pivot = Offset(size.width / 2f, tapeH / 2f)) {
                drawRect(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(size.width / 2f - tapeW / 2f, -tapeH / 3f),
                    size = Size(tapeW, tapeH),
                )
                drawRect(
                    color = edge.copy(alpha = 0.28f),
                    topLeft = Offset(size.width / 2f - tapeW / 2f, -tapeH / 3f),
                    size = Size(tapeW, tapeH),
                    style = Stroke(width = 1f),
                )
            }
        }

        PaperStyle.Recycled -> {
            // Fibras cortas: es lo que distingue el papel reciclado a la vista.
            val random = Random(seed + 7)
            val fiber = ink.copy(alpha = 0.10f)
            repeat(26) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val len = 4f + random.nextFloat() * 9f
                val a = random.nextFloat() * 6.28f
                drawLine(
                    fiber,
                    Offset(x, y),
                    Offset(x + len * kotlin.math.cos(a), y + len * sin(a)),
                    1f,
                )
            }
        }
    }
}

/** Luz arriba y sombra abajo: el truco que da grosor a una lámina plana. */
private fun DrawScope.drawDepth(ink: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.14f),
            0.06f to Color.Transparent,
        ),
        size = Size(size.width, size.height * 0.3f),
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to ink.copy(alpha = 0.09f),
        ),
        topLeft = Offset(0f, size.height * 0.72f),
        size = Size(size.width, size.height * 0.28f),
    )
}

private fun DrawScope.drawFold(edge: Color, bg: Color) {
    val fold = 18.dp.toPx()
    val triangle = Path().apply {
        moveTo(size.width - fold, 0f)
        lineTo(size.width, fold)
        lineTo(size.width - fold, fold)
        close()
    }
    drawPath(triangle, edge.copy(alpha = 0.55f))
    // Un hilo de luz en el pliegue.
    drawLine(
        bg.lighten(0.35f).copy(alpha = 0.5f),
        Offset(size.width - fold, 0f),
        Offset(size.width - fold, fold),
        1.2f,
    )
}

/** Rasga el borde inferior borrando una silueta irregular de la capa. */
private fun DrawScope.tearBottomEdge(seed: Long) {
    val random = Random(seed + 31)
    val depth = 11.dp.toPx()
    val steps = 11
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height - depth * 0.5f)
        for (i in 1..steps) {
            val x = size.width * i / steps
            val y = size.height - depth * random.nextFloat()
            lineTo(x, y)
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, Color.Black, blendMode = BlendMode.Clear)
}

/**
 * Casilla propia.
 *
 * Al marcarla, además del muelle del check, sale un anillo que se expande y se
 * desvanece: confirma la acción en el punto exacto donde estaba el dedo.
 */
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
    val pulse = remember { Animatable(1f) }
    // El muelle se lee aquí: dentro de LaunchedEffect ya no hay composición.
    val pulseSpec = Rincon.motion.settle<Float>()
    LaunchedEffect(checked) {
        if (checked) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, pulseSpec)
        }
    }

    Box(
        modifier = modifier
            .size(30.dp)
            .drawBehind {
                val p = pulse.value
                if (p < 1f) {
                    drawCircle(
                        color = ink.copy(alpha = 0.30f * (1f - p)),
                        radius = size.minDimension * (0.55f + p * 0.75f),
                        style = Stroke(width = 2.dp.toPx() * (1f - p)),
                    )
                }
            }
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
