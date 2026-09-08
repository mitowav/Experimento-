package com.rincon.espacio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.ui.icons.RinconIcons
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Estado físico de una nota mientras vive en el escritorio.
 *
 * Se guarda fuera de la composición (en un mapa recordado) para que la nota no
 * "salte" cuando la lista se recompone por otros motivos.
 */
private class NoteMotion(start: Offset, restingRotation: Float) {
    /**
     * Posición dibujada. Es un estado plano y no un `Animatable` a propósito:
     * durante el arrastre se escribe directamente desde el manejador del gesto,
     * sin pasar por una corrutina, para que el papel no vaya ni un fotograma
     * por detrás del dedo.
     */
    var value by mutableStateOf(start)

    /** Sólo se usa para el vuelo tras soltar (inercia + asentamiento). */
    private val flight = Animatable(start, Offset.VectorConverter)

    val lift = Animatable(0f)
    val tilt = Animatable(restingRotation)
    var dragging by mutableStateOf(false)
    var resting = restingRotation
    var height: Float = 0f

    suspend fun settleTo(
        target: Offset,
        spec: androidx.compose.animation.core.AnimationSpec<Offset>,
        initialVelocity: Offset,
    ) {
        flight.snapTo(value)
        flight.animateTo(target, spec, initialVelocity) {
            this@NoteMotion.value = this.value
        }
    }

    suspend fun stopFlight() {
        flight.stop()
    }
}

private const val MaxTiltDegrees = 7f
private const val LiftScale = 0.055f      // cuánto crece la nota al agarrarla
private const val InertiaSeconds = 0.055f // cuánto continúa tras soltar
private const val VelocityHandover = 0.35f

/**
 * El escritorio.
 *
 * Las notas no son una lista: son objetos con posición propia. El sistema de
 * coordenadas guarda `x` como fracción del ancho útil y `y` en dp absolutos, de
 * forma que al cambiar de móvil (o al rotar) el escritorio se reparte
 * proporcionalmente en horizontal y conserva el orden vertical.
 *
 * Física del arrastre:
 *  - la nota sigue el dedo sin retardo (se escribe directamente en el Animatable);
 *  - se eleva: escala +5,5 %, sombra más abierta y capa superior;
 *  - se inclina según la dirección del movimiento, con un tope de 7°;
 *  - al soltar continúa con la inercia medida y se asienta con un muelle;
 *  - si el punto de reposo cae cerca de una rejilla o del canto de otra nota,
 *    se acerca suavemente (imán con tolerancia, nunca una cuadrícula rígida).
 */
@Composable
fun NoteCanvas(
    items: List<NoteWithSubtasks>,
    onOpen: (Long) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onPlacementChange: (id: Long, x: Float, y: Float, rotation: Float, z: Int) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val motion = Rincon.motion
    val feedback = LocalFeedback.current
    val colors = Rincon.colors
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val states = remember { mutableStateMapOf<Long, NoteMotion>() }
    var dragTargetId by remember { mutableStateOf<Long?>(null) }
    var overTrash by remember { mutableStateOf(false) }
    var topZ by remember { mutableStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val boardWidthPx = with(density) { maxWidth.toPx() }
        val noteWidthPx = with(density) { NoteWidth.toPx() }
        val usableWidth = (boardWidthPx - noteWidthPx - with(density) { (Space.screen * 2).toPx() })
            .coerceAtLeast(1f)
        val leftInset = with(density) { Space.screen.toPx() }
        // Rejilla ancha (48dp) frente a una tolerancia estrecha (11dp): la
        // mayor parte del escritorio es espacio libre, y sólo cuando la nota
        // cae realmente cerca de una guía se deja llevar.
        val gridPx = with(density) { 48.dp.toPx() }
        val snapTolerance = with(density) { 11.dp.toPx() }
        val stackGapPx = with(density) { 12.dp.toPx() }
        val trashBandPx = with(density) { 108.dp.toPx() }

        fun toPixels(xFraction: Float, yDp: Float): Offset = Offset(
            x = leftInset + xFraction.coerceIn(0f, 1f) * usableWidth,
            y = with(density) { yDp.dp.toPx() },
        )

        fun toStored(pos: Offset): Pair<Float, Float> {
            val fraction = ((pos.x - leftInset) / usableWidth).coerceIn(0f, 1f)
            val yDp = with(density) { pos.y.toDp().value }
            return fraction to yDp
        }

        // Sincroniza el estado físico con los datos, sin tocar la nota que se
        // está arrastrando en este momento.
        LaunchedEffect(items, usableWidth) {
            items.forEach { item ->
                val note = item.note
                val target = toPixels(note.x, note.y)
                val existing = states[note.id]
                if (existing == null) {
                    states[note.id] = NoteMotion(target, note.rotation)
                } else if (!existing.dragging) {
                    existing.resting = note.rotation
                    if ((existing.value - target).getDistance() > 1.5f) {
                        existing.value = target
                    }
                }
            }
            val ids = items.map { it.note.id }.toSet()
            states.keys.filter { it !in ids }.forEach { states.remove(it) }
            topZ = items.maxOfOrNull { it.note.zIndex } ?: 0
        }

        val lowestY = items.maxOfOrNull { it.note.y } ?: 0f
        val boardHeight = maxOf(maxHeight, (lowestY + 340f).dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardHeight)
            ) {
                items.sortedBy { it.note.zIndex }.forEach { item ->
                    val note = item.note
                    val state = states[note.id] ?: return@forEach
                    val liftValue = state.lift.value

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    state.value.x.roundToInt(),
                                    state.value.y.roundToInt(),
                                )
                            }
                            .width(NoteWidth)
                            .onSizeChanged { state.height = it.height.toFloat() }
                            .graphicsLayer {
                                val scale = 1f + liftValue * LiftScale * motion.physicality
                                scaleX = scale
                                scaleY = scale
                                rotationZ = state.tilt.value
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                            }
                            .softShadow(
                                elevation = (5 + liftValue * 16).dp,
                                shape = Rincon.shapes.note,
                                ambientAlpha = 0.10f + liftValue * 0.06f,
                                spotAlpha = 0.16f + liftValue * 0.16f,
                            )
                            .pointerInput(note.id) {
                                var tracker = VelocityTracker()
                                var accumulated = Offset.Zero
                                detectDragGestures(
                                    onDragStart = {
                                        tracker = VelocityTracker()
                                        accumulated = Offset.Zero
                                        state.dragging = true
                                        dragTargetId = note.id
                                        scope.launch { state.stopFlight() }
                                        topZ += 1
                                        onPlacementChange(note.id, note.x, note.y, note.rotation, topZ)
                                        feedback.paper()
                                        scope.launch { state.lift.animateTo(1f, motion.snappy()) }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulated += dragAmount
                                        tracker.addPosition(change.uptimeMillis, accumulated)
                                        val next = state.value + dragAmount
                                        state.value = next
                                        val lean = (dragAmount.x * 1.15f)
                                            .coerceIn(-MaxTiltDegrees, MaxTiltDegrees) * motion.physicality
                                        scope.launch {
                                            state.tilt.animateTo(state.resting + lean, motion.snappy())
                                        }
                                        val viewportY = next.y - scrollState.value.toFloat()
                                        val nowOverTrash = viewportY + state.height > viewportHeightPx - trashBandPx
                                        if (nowOverTrash != overTrash) {
                                            overTrash = nowOverTrash
                                            if (nowOverTrash) feedback.warn()
                                        }
                                    },
                                    onDragEnd = {
                                        val velocity = tracker.calculateVelocity()
                                        state.dragging = false
                                        dragTargetId = null
                                        val wasOverTrash = overTrash
                                        overTrash = false

                                        if (wasOverTrash) {
                                            feedback.warn()
                                            scope.launch { state.lift.animateTo(0f, motion.gentle()) }
                                            onDelete(note.id)
                                        } else {
                                        val current = state.value
                                        val predicted = current + Offset(
                                            velocity.x * InertiaSeconds,
                                            velocity.y * InertiaSeconds,
                                        )
                                        val others = items
                                            .filter { it.note.id != note.id }
                                            .mapNotNull { other ->
                                                states[other.note.id]?.let { it.value to it.height }
                                            }
                                        val settled = settlePosition(
                                            candidate = predicted,
                                            noteWidth = noteWidthPx,
                                            noteHeight = state.height,
                                            minX = leftInset,
                                            maxX = leftInset + usableWidth,
                                            grid = gridPx,
                                            gap = stackGapPx,
                                            tolerance = snapTolerance,
                                            others = others,
                                        )
                                        val (storedX, storedY) = toStored(settled)
                                        feedback.settle()
                                        scope.launch {
                                            state.lift.animateTo(0f, motion.settle())
                                        }
                                        scope.launch {
                                            state.tilt.animateTo(state.resting, motion.settle())
                                        }
                                        scope.launch {
                                            state.settleTo(
                                                target = settled,
                                                spec = motion.settle(),
                                                initialVelocity = Offset(
                                                    velocity.x * VelocityHandover,
                                                    velocity.y * VelocityHandover,
                                                ),
                                            )
                                        }
                                        onPlacementChange(note.id, storedX, storedY, note.rotation, topZ)
                                        }
                                    },
                                    onDragCancel = {
                                        state.dragging = false
                                        dragTargetId = null
                                        overTrash = false
                                        scope.launch { state.lift.animateTo(0f, motion.settle()) }
                                        scope.launch { state.tilt.animateTo(state.resting, motion.settle()) }
                                    },
                                )
                            }
                            .pointerInput(note.id) {
                                detectTapGestures(
                                    onTap = {
                                        feedback.tap()
                                        onOpen(note.id)
                                    },
                                    onLongPress = {
                                        feedback.pop()
                                        onOpen(note.id)
                                    },
                                )
                            }
                            .semantics {
                                contentDescription = buildString {
                                    append("Nota: ")
                                    append(note.text.ifBlank { "sin texto" })
                                    if (note.isTask) append(if (note.done) ". Completada" else ". Pendiente")
                                }
                            },
                    ) {
                        PaperNote(
                            item = item,
                            onToggleDone = if (note.isTask) {
                                { onToggleDone(note.id, !note.done) }
                            } else null,
                        )
                    }
                }

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) { emptyContent() }
                }
            }
        }

        // Papelera: sólo existe mientras se arrastra, y se abre al acercarse.
        AnimatedVisibility(
            visible = dragTargetId != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Space.xl),
            enter = scaleIn(motion.gentle(), initialScale = 0.8f) + fadeIn(motion.fade()),
            exit = scaleOut(motion.snappy(), targetScale = 0.85f) + fadeOut(motion.quickFade()),
        ) {
            val bg = if (overTrash) colors.danger else colors.surface
            Box(
                modifier = Modifier
                    .softShadow(if (overTrash) 16.dp else 8.dp, Rincon.shapes.pill)
                    .clip(Rincon.shapes.pill)
                    .background(bg)
                    .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
                    .padding(horizontal = Space.xl, vertical = Space.m),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Space.s),
                ) {
                    RinconIcon(
                        RinconIcons.Trash,
                        null,
                        tint = if (overTrash) colors.accentInk else colors.textSecondary,
                        size = 22.dp,
                    )
                    Text(
                        if (overTrash) "Suelta para tirar" else "Arrastra aquí para tirar",
                        style = Rincon.type.label,
                        color = if (overTrash) colors.accentInk else colors.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Imán suave.
 *
 * Primero se limita la nota al tablero; después, si el punto de reposo cae
 * dentro de la tolerancia respecto al canto de otra nota (alinear o apilar) o a
 * una línea de la rejilla, se ajusta. La rejilla es deliberadamente ancha en
 * relación con la tolerancia: fuera de ese margen la nota se queda exactamente
 * donde la persona la ha dejado. El escritorio es suyo, no de la cuadrícula.
 */
internal fun settlePosition(
    candidate: Offset,
    noteWidth: Float,
    noteHeight: Float,
    minX: Float,
    maxX: Float,
    grid: Float,
    gap: Float,
    tolerance: Float,
    others: List<Pair<Offset, Float>>,
): Offset {
    var x = candidate.x.coerceIn(minX, maxX)
    var y = candidate.y.coerceAtLeast(0f)

    fun magnet(value: Float, anchor: Float): Float? =
        if (abs(value - anchor) <= tolerance) anchor else null

    // Cantos de otras notas: alinear izquierdas, o apilar justo debajo.
    var snappedX = false
    var snappedY = false
    for ((pos, height) in others) {
        if (!snappedX) magnet(x, pos.x)?.let { x = it; snappedX = true }
        if (!snappedY) {
            magnet(y, pos.y)?.let { y = it; snappedY = true }
                ?: magnet(y, pos.y + height + gap)?.let { y = it; snappedY = true }
        }
        if (snappedX && snappedY) break
    }

    if (!snappedX && grid > 0f) {
        val nearest = (x / grid).toDouble().let { Math.round(it) * grid }
        magnet(x, nearest)?.let { x = it }
    }
    if (!snappedY && grid > 0f) {
        val nearest = (y / grid).toDouble().let { Math.round(it) * grid }
        magnet(y, nearest)?.let { y = it }
    }

    return Offset(x.coerceIn(minX, maxX), y.coerceAtLeast(0f))
}
