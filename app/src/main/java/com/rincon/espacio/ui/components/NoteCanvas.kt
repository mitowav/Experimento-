package com.rincon.espacio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.domain.model.NoteWithSubtasks
import com.rincon.espacio.ui.icons.RinconIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Alto de referencia de una nota mientras aún no se ha medido la real. */
private val AssumedNoteHeight = 200.dp

/** Estado físico de una nota mientras vive en el tablero. */
private class NoteMotion(start: Offset, restingRotation: Float) {
    /**
     * Posición dibujada, en coordenadas de tablero. Es un estado plano y no un
     * `Animatable` a propósito: durante el arrastre se escribe directamente
     * desde el manejador del gesto, sin pasar por una corrutina, para que el
     * papel no vaya ni un fotograma por detrás del dedo.
     */
    var value by mutableStateOf(start)

    /** Sólo se usa para el vuelo tras soltar (inercia + asentamiento). */
    private val flight = Animatable(start, Offset.VectorConverter)

    val lift = Animatable(0f)
    val tilt = Animatable(restingRotation)
    val appear = Animatable(0f)

    var dragging by mutableStateOf(false)
    var resting = restingRotation
    var height by mutableStateOf(0f)

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

/** Un trozo de papel roto volando. */
private class Shard(val local: Rect, val vx: Float, val vy: Float, val spin: Float)

private class Shatter(
    val origin: Offset,
    val color: Color,
    val shards: List<Shard>,
    val progress: Animatable<Float, *>,
)

private const val MaxTiltDegrees = 7f
private const val LiftScale = 0.055f
private const val InertiaSeconds = 0.055f
private const val VelocityHandover = 0.35f

/**
 * El escritorio.
 *
 * Las notas no son una lista: son papeles con posición propia sobre un tablero
 * más grande que la pantalla, por el que uno se pasea y se acerca.
 *
 * **Cómo se dibuja.** Todo vive en coordenadas de pantalla, calculadas a mano
 * con `pantalla = tablero * escala + desplazamiento`. No hay un nodo gigante
 * escalado con `graphicsLayer`: esa vía dependía de cómo Compose resuelve las
 * restricciones de un hijo mayor que su padre, y el encuadre salía mal en
 * pantallas reales. Aquí cada papel se coloca donde toca y se le da el tamaño
 * exacto que ocupa a la vista, de modo que lo que se ve y lo que responde al
 * dedo son lo mismo a cualquier zoom.
 *
 * **Física del arrastre.** La nota sigue el dedo sin retardo; se eleva, se
 * inclina según la dirección, y al soltarla continúa con la inercia medida
 * antes de asentarse con un muelle. Si el punto de reposo cae cerca del canto
 * de otra nota o de una guía, se acerca con tolerancia estrecha.
 */
@Composable
fun NoteCanvas(
    items: List<NoteWithSubtasks>,
    boardState: BoardState,
    onOpen: (Long) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onPlacementChange: (id: Long, x: Float, y: Float, rotation: Float, z: Int) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    emptyContent: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val motion = Rincon.motion
    val feedback = LocalFeedback.current
    val colors = Rincon.colors
    val scope = rememberCoroutineScope()
    val appearSpec = motion.playful<Float>()

    val states = remember { mutableStateMapOf<Long, NoteMotion>() }
    val shatters = remember { mutableStateListOf<Shatter>() }
    var dragTargetId by remember { mutableStateOf<Long?>(null) }
    var overTrash by remember { mutableStateOf(false) }
    var topZ by remember { mutableStateOf(0) }
    var settled by remember { mutableStateOf(false) }

    val boardW = with(density) { Board.Width.toPx() }
    val boardH = with(density) { Board.Height.toPx() }
    val noteWidthPx = with(density) { NoteWidth.toPx() }
    val assumedHeightPx = with(density) { AssumedNoteHeight.toPx() }
    val gridPx = with(density) { 48.dp.toPx() }
    val snapTolerance = with(density) { 11.dp.toPx() }
    val stackGapPx = with(density) { 12.dp.toPx() }
    val trashBandPx = with(density) { 108.dp.toPx() }
    val trashHalfWidthPx = with(density) { 140.dp.toPx() }
    val edgeZonePx = with(density) { 84.dp.toPx() }
    val maxEdgeSpeed = with(density) { 620.dp.toPx() }
    val fitPadding = with(density) { 32.dp.toPx() }
    val topInsetPx = with(density) { topInset.toPx() }
    val bottomInsetPx = with(density) { bottomInset.toPx() }

    // Los márgenes pueden cambiar (el encabezado se mide, la barra también),
    // así que el encuadre se reconfigura cuando lo hacen, no sólo al empezar.
    LaunchedEffect(topInsetPx, bottomInsetPx, boardW, boardH) {
        if (boardState.viewport != Size.Zero) {
            boardState.configure(boardState.viewport, Size(boardW, boardH), topInsetPx, bottomInsetPx)
        }
    }

    fun noteRect(item: NoteWithSubtasks): Rect {
        val x = with(density) { item.note.x.dp.toPx() }
        val y = with(density) { item.note.y.dp.toPx() }
        val h = states[item.note.id]?.height?.takeIf { it > 0f } ?: assumedHeightPx
        return Rect(x, y, x + noteWidthPx, y + h)
    }

    val contentBounds = remember(items, noteWidthPx, assumedHeightPx, density) {
        if (items.isEmpty()) null else {
            var left = Float.MAX_VALUE
            var top = Float.MAX_VALUE
            var right = -Float.MAX_VALUE
            var bottom = -Float.MAX_VALUE
            items.forEach { item ->
                val x = with(density) { item.note.x.dp.toPx() }
                val y = with(density) { item.note.y.dp.toPx() }
                if (x < left) left = x
                if (y < top) top = y
                if (x + noteWidthPx > right) right = x + noteWidthPx
                if (y + assumedHeightPx > bottom) bottom = y + assumedHeightPx
            }
            Rect(left, top, right, bottom)
        }
    }

    fun anyNoteVisible(): Boolean =
        items.isEmpty() || items.any { boardState.isVisible(noteRect(it)) }

    var lost by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableStateOf(0) }
    var controlsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(interactionTick) {
        if (interactionTick == 0) return@LaunchedEffect
        controlsVisible = true
        delay(2400)
        controlsVisible = false
    }

    /**
     * Encuadre de entrada.
     *
     * Se espera a tener ventana Y notas: la base de datos emite primero una
     * lista vacía, y dar el encuadre por hecho en ese momento era justo lo que
     * dejaba el escritorio mirando a un rincón vacío.
     */
    LaunchedEffect(boardState.viewport, contentBounds, topInsetPx, bottomInsetPx) {
        if (boardState.viewport == Size.Zero) return@LaunchedEffect
        if (settled) return@LaunchedEffect
        if (contentBounds == null) return@LaunchedEffect
        boardState.fitTo(contentBounds, fitPadding)
        settled = true
    }

    LaunchedEffect(boardState.offset, boardState.scale, items) {
        lost = settled && contentBounds != null && !anyNoteVisible()
    }

    fun shatter(item: NoteWithSubtasks, motionState: NoteMotion) {
        val paper = PaperColor.fromKey(item.note.colorKey)
        val screen = boardState.boardToScreen(motionState.value)
        val scale = boardState.scale
        val w = noteWidthPx * scale
        val h = (motionState.height.takeIf { it > 0f } ?: assumedHeightPx) * scale
        val random = Random(item.note.id * 31 + 7)
        val columns = 3
        val rows = 4
        val shards = buildList {
            for (r in 0 until rows) for (c in 0 until columns) {
                val rect = Rect(
                    left = w * c / columns,
                    top = h * r / rows,
                    right = w * (c + 1) / columns,
                    bottom = h * (r + 1) / rows,
                )
                val angle = kotlin.math.atan2(rect.center.y - h / 2f, rect.center.x - w / 2f) +
                    (random.nextFloat() - 0.5f) * 0.9f
                val speed = 120f + random.nextFloat() * 340f
                add(
                    Shard(
                        local = rect,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 220f,
                        spin = (random.nextFloat() - 0.5f) * 520f,
                    )
                )
            }
        }
        val entry = Shatter(screen, paper.paper(colors.isDark), shards, Animatable(0f))
        shatters.add(entry)
        scope.launch {
            entry.progress.animateTo(1f, tween(660, easing = LinearEasing))
            shatters.remove(entry)
        }
    }

    fun isOverTrash(screen: Offset, heightPx: Float): Boolean {
        val viewport = boardState.viewport
        if (viewport == Size.Zero) return false
        val scale = boardState.scale
        val bottom = screen.y + heightPx * scale
        val centerX = screen.x + noteWidthPx * scale / 2f
        return bottom > viewport.height - trashBandPx - bottomInsetPx &&
            abs(centerX - viewport.width / 2f) < trashHalfWidthPx
    }

    /**
     * Paneo de borde: al llevar una nota al filo, el tablero se desliza por
     * debajo a velocidad continua. Movimiento por fotograma, nunca un salto.
     */
    LaunchedEffect(dragTargetId) {
        val id = dragTargetId ?: return@LaunchedEffect
        val state = states[id] ?: return@LaunchedEffect
        var previousFrame = 0L
        while (true) {
            withFrameNanos { now ->
                val delta = if (previousFrame == 0L) 0f else (now - previousFrame) / 1_000_000_000f
                previousFrame = now
                val viewport = boardState.viewport
                if (delta > 0f && viewport != Size.Zero && state.dragging) {
                    val screen = boardState.boardToScreen(state.value)
                    val scale = boardState.scale
                    val cx = screen.x + noteWidthPx * scale / 2f
                    val cy = screen.y + state.height * scale / 2f

                    fun pressure(distance: Float): Float =
                        ((edgeZonePx - distance) / edgeZonePx).coerceIn(0f, 1f)

                    var pushX = 0f
                    var pushY = 0f
                    if (cx < edgeZonePx) pushX = pressure(cx)
                    if (cx > viewport.width - edgeZonePx) pushX = -pressure(viewport.width - cx)
                    if (cy < topInsetPx + edgeZonePx) pushY = pressure(cy - topInsetPx)
                    val bottomEdge = viewport.height - bottomInsetPx - edgeZonePx
                    if (cy > bottomEdge && !isOverTrash(screen, state.height)) {
                        pushY = -pressure(viewport.height - bottomInsetPx - cy)
                    }

                    if (pushX != 0f || pushY != 0f) {
                        val step = Offset(
                            pushX * abs(pushX) * maxEdgeSpeed * delta,
                            pushY * abs(pushY) * maxEdgeSpeed * delta,
                        )
                        val moved = boardState.clamp(boardState.offset + step)
                        val applied = moved - boardState.offset
                        boardState.offset = moved
                        state.value -= applied / boardState.scale
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged {
                boardState.configure(
                    viewport = Size(it.width.toFloat(), it.height.toFloat()),
                    board = Size(boardW, boardH),
                    top = topInsetPx,
                    bottom = bottomInsetPx,
                )
            }
            .drawBehind {
                // La mesa, dibujada en coordenadas de pantalla: nada de nodos
                // gigantes: sólo aritmética.
                val scale = boardState.scale
                val origin = boardState.boardToScreen(Offset.Zero)
                val boardSize = Size(boardW * scale, boardH * scale)
                val corner = CornerRadius(28.dp.toPx() * scale)

                drawRoundRect(
                    color = colors.surface.copy(alpha = 0.45f),
                    topLeft = origin,
                    size = boardSize,
                    cornerRadius = corner,
                )

                val step = 96.dp.toPx() * scale
                if (step > 6f) {
                    val faint = colors.outline.copy(alpha = 0.28f)
                    var x = origin.x + step
                    while (x < origin.x + boardSize.width) {
                        if (x >= 0f && x <= size.width) {
                            drawLine(
                                faint,
                                Offset(x, origin.y.coerceAtLeast(0f)),
                                Offset(x, (origin.y + boardSize.height).coerceAtMost(size.height)),
                                1f,
                            )
                        }
                        x += step
                    }
                    var y = origin.y + step
                    while (y < origin.y + boardSize.height) {
                        if (y >= 0f && y <= size.height) {
                            drawLine(
                                faint,
                                Offset(origin.x.coerceAtLeast(0f), y),
                                Offset((origin.x + boardSize.width).coerceAtMost(size.width), y),
                                1f,
                            )
                        }
                        y += step
                    }
                }

                drawRoundRect(
                    color = colors.outline.copy(alpha = 0.75f),
                    topLeft = origin,
                    size = boardSize,
                    cornerRadius = corner,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        feedback.click()
                        interactionTick++
                        val target = if (boardState.scale > 1.05f) 1f else Board.DoubleTapScale
                        boardState.zoomTo(target, position)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    interactionTick++
                    boardState.applyTransform(centroid, pan, zoom)
                }
            }
    ) {
        val scale = boardState.scale

        items.sortedBy { it.note.zIndex }.forEachIndexed { index, item ->
            val note = item.note
            key(note.id) {
                val state = remember(note.id) {
                    NoteMotion(
                        start = with(density) { Offset(note.x.dp.toPx(), note.y.dp.toPx()) },
                        restingRotation = note.rotation,
                    ).also { states[note.id] = it }
                }
                DisposableEffect(note.id) { onDispose { states.remove(note.id) } }
                LaunchedEffect(note.id) {
                    if (state.appear.value < 1f) {
                        if (!settled) delay(index * 28L)
                        state.appear.animateTo(1f, appearSpec)
                    }
                }
                // La posición guardada manda mientras no se esté arrastrando.
                LaunchedEffect(note.x, note.y, note.rotation) {
                    if (!state.dragging) {
                        state.resting = note.rotation
                        val target = with(density) { Offset(note.x.dp.toPx(), note.y.dp.toPx()) }
                        if ((state.value - target).getDistance() > 1.5f) state.value = target
                    }
                }

                val screen = boardState.boardToScreen(state.value)
                val liftValue = state.lift.value
                val appearValue = state.appear.value
                val measuredHeight = state.height.takeIf { it > 0f } ?: assumedHeightPx

                Box(
                    modifier = Modifier
                        .offset { IntOffset(screen.x.roundToInt(), screen.y.roundToInt()) }
                        // El tamaño del nodo es el que ocupa a la vista, así lo
                        // que se toca coincide con lo que se ve a cualquier zoom.
                        .requiredSize(
                            width = with(density) { (noteWidthPx * scale).toDp() },
                            height = with(density) { (measuredHeight * scale).toDp() },
                        )
                        .graphicsLayer {
                            val born = 0.9f + 0.1f * appearValue
                            val grow = born * (1f + liftValue * LiftScale * motion.physicality)
                            scaleX = grow
                            scaleY = grow
                            alpha = appearValue.coerceIn(0f, 1f)
                            rotationZ = state.tilt.value * appearValue
                            transformOrigin = TransformOrigin.Center
                        }
                        .softShadow(
                            elevation = (4 + liftValue * 16).dp,
                            shape = Rincon.shapes.note,
                            ambientAlpha = 0.10f + liftValue * 0.06f,
                            spotAlpha = 0.14f + liftValue * 0.16f,
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
                                    val boardDelta = dragAmount / boardState.scale
                                    accumulated += dragAmount
                                    tracker.addPosition(change.uptimeMillis, accumulated)
                                    state.value += boardDelta
                                    val lean = (dragAmount.x * 1.15f)
                                        .coerceIn(-MaxTiltDegrees, MaxTiltDegrees) * motion.physicality
                                    scope.launch {
                                        state.tilt.animateTo(state.resting + lean, motion.snappy())
                                    }
                                    val nowOverTrash = isOverTrash(
                                        boardState.boardToScreen(state.value),
                                        state.height,
                                    )
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
                                        feedback.tear()
                                        shatter(item, state)
                                        scope.launch { state.lift.animateTo(0f, motion.gentle()) }
                                        onDelete(note.id)
                                    } else {
                                        val currentScale = boardState.scale
                                        val predicted = state.value + Offset(
                                            velocity.x * InertiaSeconds / currentScale,
                                            velocity.y * InertiaSeconds / currentScale,
                                        )
                                        val others = items
                                            .filter { it.note.id != note.id }
                                            .mapNotNull { other ->
                                                states[other.note.id]?.let { it.value to it.height }
                                            }
                                        val target = settlePosition(
                                            candidate = predicted,
                                            noteWidth = noteWidthPx,
                                            noteHeight = state.height,
                                            minX = 0f,
                                            maxX = boardW - noteWidthPx,
                                            maxY = boardH - measuredHeight,
                                            grid = gridPx,
                                            gap = stackGapPx,
                                            tolerance = snapTolerance,
                                            others = others,
                                        )
                                        feedback.settle()
                                        scope.launch { state.lift.animateTo(0f, motion.settle()) }
                                        scope.launch { state.tilt.animateTo(state.resting, motion.settle()) }
                                        scope.launch {
                                            state.settleTo(
                                                target = target,
                                                spec = motion.settle(),
                                                initialVelocity = Offset(
                                                    velocity.x * VelocityHandover / currentScale,
                                                    velocity.y * VelocityHandover / currentScale,
                                                ),
                                            )
                                        }
                                        onPlacementChange(
                                            note.id,
                                            with(density) { target.x.toDp().value },
                                            with(density) { target.y.toDp().value },
                                            note.rotation,
                                            topZ,
                                        )
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
                                onTap = { feedback.tap(); onOpen(note.id) },
                                onLongPress = { feedback.pop(); onOpen(note.id) },
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
                    // El papel se compone a su tamaño natural y se escala; así
                    // el texto conserva su maquetación al alejar el zoom.
                    Box(
                        Modifier
                            .requiredWidth(NoteWidth)
                            .onSizeChanged { state.height = it.height.toFloat() }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                    ) {
                        PaperNote(
                            item = item,
                            onToggleDone = if (note.isTask) {
                                { onToggleDone(note.id, !note.done) }
                            } else null,
                        )
                    }
                }
            }
        }

        if (shatters.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                shatters.forEach { entry ->
                    val t = entry.progress.value
                    val alpha = ((1f - t) * (1f - t)).coerceIn(0f, 1f)
                    entry.shards.forEach { shard ->
                        val x = entry.origin.x + shard.local.left + shard.vx * t
                        val y = entry.origin.y + shard.local.top + shard.vy * t + 1500f * t * t
                        val shrink = 1f - 0.3f * t
                        rotate(
                            degrees = shard.spin * t,
                            pivot = Offset(x + shard.local.width / 2f, y + shard.local.height / 2f),
                        ) {
                            drawRect(
                                color = entry.color.copy(alpha = alpha),
                                topLeft = Offset(x, y),
                                size = Size(shard.local.width * shrink, shard.local.height * shrink),
                            )
                        }
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset + Space.xl, bottom = bottomInset + Space.xxxl),
                contentAlignment = Alignment.Center,
            ) { emptyContent() }
        }

        AnimatedVisibility(
            visible = controlsVisible && dragTargetId == null,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = Space.m),
            enter = fadeIn(motion.fade()) + scaleIn(motion.gentle(), initialScale = 0.85f),
            exit = fadeOut(motion.fade()) + scaleOut(motion.gentle(), targetScale = 0.85f),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                ZoomButton(RinconIcons.Plus, "Acercar") {
                    interactionTick++
                    boardState.zoomTo(boardState.scale * 1.35f, boardState.safeRect.center)
                }
                ZoomButton(RinconIcons.Minus, "Alejar") {
                    interactionTick++
                    boardState.zoomTo(boardState.scale / 1.35f, boardState.safeRect.center)
                }
            }
        }

        AnimatedVisibility(
            visible = lost && dragTargetId == null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + Space.m),
            enter = fadeIn(motion.fade()) + scaleIn(motion.playful(), initialScale = 0.85f),
            exit = fadeOut(motion.quickFade()) + scaleOut(motion.snappy(), targetScale = 0.9f),
        ) {
            Row(
                modifier = Modifier
                    .softShadow(10.dp, Rincon.shapes.pill)
                    .clip(Rincon.shapes.pill)
                    .background(colors.surface)
                    .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
                    .pressable { contentBounds?.let { boardState.fitTo(it, fitPadding) } }
                    .padding(horizontal = Space.l, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                RinconIcon(RinconIcons.Undo, null, tint = colors.accent, size = 20.dp)
                Text("Volver a mis notas", style = Rincon.type.navLabel, color = colors.textPrimary, maxLines = 1)
            }
        }

        AnimatedVisibility(
            visible = dragTargetId != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + Space.s),
            enter = scaleIn(motion.gentle(), initialScale = 0.8f) + fadeIn(motion.fade()),
            exit = scaleOut(motion.snappy(), targetScale = 0.85f) + fadeOut(motion.quickFade()),
        ) {
            Row(
                modifier = Modifier
                    .softShadow(if (overTrash) 16.dp else 8.dp, Rincon.shapes.pill)
                    .clip(Rincon.shapes.pill)
                    .background(if (overTrash) colors.danger else colors.surface)
                    .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
                    .padding(horizontal = Space.xl, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                RinconIcon(
                    RinconIcons.Trash,
                    null,
                    tint = if (overTrash) colors.accentInk else colors.textSecondary,
                    size = 22.dp,
                )
                Text(
                    if (overTrash) "Suelta" else "Tirar",
                    style = Rincon.type.navLabel,
                    color = if (overTrash) colors.accentInk else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = Rincon.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .softShadow(6.dp, Rincon.shapes.pill)
            .clip(Rincon.shapes.pill)
            .background(colors.surface.copy(alpha = 0.92f))
            .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
            .pressable(hapticOnPress = false, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RinconIcon(icon, null, tint = colors.textSecondary, size = 20.dp)
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
    maxY: Float,
    grid: Float,
    gap: Float,
    tolerance: Float,
    others: List<Pair<Offset, Float>>,
): Offset {
    var x = candidate.x.coerceIn(minX, maxX)
    var y = candidate.y.coerceIn(0f, maxY.coerceAtLeast(0f))

    fun magnet(value: Float, anchor: Float): Float? =
        if (abs(value - anchor) <= tolerance) anchor else null

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
        val nearest = Math.round((x / grid).toDouble()) * grid
        magnet(x, nearest)?.let { x = it }
    }
    if (!snappedY && grid > 0f) {
        val nearest = Math.round((y / grid).toDouble()) * grid
        magnet(y, nearest)?.let { y = it }
    }

    return Offset(x.coerceIn(minX, maxX), y.coerceIn(0f, maxY.coerceAtLeast(0f)))
}
