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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    /** 0 = acaba de nacer, 1 = asentada. Da la entrada de las notas nuevas. */
    val appear = Animatable(0f)

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

/** Un trozo de papel roto volando. */
private class Shard(
    val local: Rect,
    val vx: Float,
    val vy: Float,
    val spin: Float,
)

private class Shatter(
    val id: Long,
    val origin: Offset,
    val color: Color,
    val edge: Color,
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
 * Las notas no son una lista: son objetos con posición propia sobre un tablero
 * mucho más grande que la pantalla. Se puede pasear por él arrastrando el
 * fondo, acercarse con dos dedos o con los botones, y volver a la vista general
 * de un toque.
 *
 * Física del arrastre:
 *  - la nota sigue el dedo sin retardo (el gesto escribe la posición directamente);
 *  - se eleva: escala +5,5 %, sombra más abierta y capa superior;
 *  - se inclina según la dirección del movimiento, con un tope de 7°;
 *  - al soltar continúa con la inercia medida y se asienta con un muelle;
 *  - si el punto de reposo cae cerca de una guía o del canto de otra nota, se
 *    acerca suavemente (imán con tolerancia estrecha, nunca cuadrícula rígida).
 *
 * Todo el gesto se divide por la escala actual, de modo que arrastrar se siente
 * igual de preciso con el tablero alejado que con la nota a tamaño real.
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
    emptyContent: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val motion = Rincon.motion
    val feedback = LocalFeedback.current
    val colors = Rincon.colors
    val scope = rememberCoroutineScope()

    val states = remember { mutableStateMapOf<Long, NoteMotion>() }
    val shatters = remember { mutableStateListOf<Shatter>() }
    var dragTargetId by remember { mutableStateOf<Long?>(null) }
    var overTrash by remember { mutableStateOf(false) }
    var topZ by remember { mutableStateOf(0) }
    var initialized by remember { mutableStateOf(false) }

    val boardW = with(density) { Board.Width.toPx() }
    val boardH = with(density) { Board.Height.toPx() }
    val noteWidthPx = with(density) { NoteWidth.toPx() }
    val gridPx = with(density) { 48.dp.toPx() }
    val snapTolerance = with(density) { 11.dp.toPx() }
    val stackGapPx = with(density) { 12.dp.toPx() }
    val trashBandPx = with(density) { 116.dp.toPx() }
    val trashHalfWidthPx = with(density) { 140.dp.toPx() }
    val edgeZonePx = with(density) { 84.dp.toPx() }
    val maxEdgeSpeed = with(density) { 620.dp.toPx() }

    LaunchedEffect(boardW, boardH) {
        boardState.boardPx = Size(boardW, boardH)
        boardState.offset = boardState.clamp()
    }

    // Sincroniza el estado físico con los datos, sin tocar la nota que se está
    // arrastrando en este momento.
    LaunchedEffect(items) {
        val firstLoad = !initialized
        items.forEachIndexed { index, item ->
            val note = item.note
            val target = with(density) { Offset(note.x.dp.toPx(), note.y.dp.toPx()) }
            val existing = states[note.id]
            if (existing == null) {
                val fresh = NoteMotion(target, note.rotation)
                states[note.id] = fresh
                launch {
                    // En la primera carga entran en cascada; después, al vuelo.
                    if (firstLoad) delay(index * 28L)
                    fresh.appear.animateTo(1f, motion.playful())
                }
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
        initialized = true
    }

    /** ¿Está la nota sobre la papelera? Sólo cuenta la franja central. */
    fun isOverTrash(screen: Offset, height: Float): Boolean {
        val viewport = boardState.viewport
        if (viewport == Size.Zero) return false
        val bottom = screen.y + height * boardState.scale
        val centerX = screen.x + noteWidthPx * boardState.scale / 2f
        val withinBand = bottom > viewport.height - trashBandPx
        val withinPill = abs(centerX - viewport.width / 2f) < trashHalfWidthPx
        return withinBand && withinPill
    }

    /**
     * Paneo de borde.
     *
     * Al arrastrar una nota hasta el filo de la pantalla, el tablero se desliza
     * por debajo a velocidad continua, proporcional a lo cerca que esté el dedo
     * del borde. Es un movimiento por fotograma, no un salto: la nota nunca da
     * un tirón, y como el tablero se mueve bajo el dedo, la nota se queda
     * exactamente donde la sujetas.
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
                    val cx = screen.x + noteWidthPx * boardState.scale / 2f
                    val cy = screen.y + state.height * boardState.scale / 2f

                    fun pressure(distance: Float): Float =
                        ((edgeZonePx - distance) / edgeZonePx).coerceIn(0f, 1f)

                    var pushX = 0f
                    var pushY = 0f
                    if (cx < edgeZonePx) pushX = pressure(cx)
                    if (cx > viewport.width - edgeZonePx) pushX = -pressure(viewport.width - cx)
                    if (cy < edgeZonePx) pushY = pressure(cy)
                    // Abajo no se panea sobre la papelera: ahí el gesto significa tirar.
                    if (cy > viewport.height - edgeZonePx && !isOverTrash(screen, state.height)) {
                        pushY = -pressure(viewport.height - cy)
                    }

                    if (pushX != 0f || pushY != 0f) {
                        // Curva cuadrática: cerca del borde apenas se mueve y
                        // se acelera al insistir, en vez de arrancar de golpe.
                        val step = Offset(
                            pushX * abs(pushX) * maxEdgeSpeed * delta,
                            pushY * abs(pushY) * maxEdgeSpeed * delta,
                        )
                        val moved = boardState.clamp(boardState.offset + step)
                        val applied = moved - boardState.offset
                        boardState.offset = moved
                        // El tablero se ha deslizado; la nota viaja con el dedo.
                        state.value -= applied / boardState.scale
                    }
                }
            }
        }
    }

    fun shatter(item: NoteWithSubtasks, motionState: NoteMotion) {
        val dark = colors.isDark
        val paper = PaperColor.fromKey(item.note.colorKey)
        val screen = boardState.boardToScreen(motionState.value)
        val w = noteWidthPx * boardState.scale
        val h = (if (motionState.height > 0f) motionState.height else noteWidthPx) * boardState.scale
        val random = Random(item.note.id * 31 + 7)
        val cols = 3
        val rows = 4
        val shards = buildList {
            for (r in 0 until rows) for (c in 0 until cols) {
                val rect = Rect(
                    left = w * c / cols,
                    top = h * r / rows,
                    right = w * (c + 1) / cols,
                    bottom = h * (r + 1) / rows,
                )
                val cx = rect.center.x - w / 2f
                val cy = rect.center.y - h / 2f
                val angle = kotlin.math.atan2(cy, cx) + (random.nextFloat() - 0.5f) * 0.9f
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
        val entry = Shatter(
            id = item.note.id,
            origin = screen,
            color = paper.paper(dark),
            edge = paper.edge(dark),
            shards = shards,
            progress = Animatable(0f),
        )
        shatters.add(entry)
        scope.launch {
            entry.progress.animateTo(1f, tween(660, easing = LinearEasing))
            shatters.remove(entry)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged {
                boardState.viewport = Size(it.width.toFloat(), it.height.toFloat())
                boardState.offset = boardState.clamp()
            }
            // Doble toque para acercarse justo donde se ha tocado.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        feedback.click()
                        val target = if (boardState.scale > 1.05f) 1f else Board.DoubleTapScale
                        boardState.zoomTo(target, position)
                    },
                )
            }
            // Pasear y acercar el tablero. Las notas consumen sus propios
            // gestos, así que esto sólo actúa sobre el fondo.
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    boardState.applyTransform(centroid, pan, zoom)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = boardState.scale
                    scaleY = boardState.scale
                    translationX = boardState.offset.x
                    translationY = boardState.offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .size(Board.Width, Board.Height)
                .boardSurface(colors.outlineSoft, colors.surfaceSunken)
        ) {
            items.sortedBy { it.note.zIndex }.forEach { item ->
                val note = item.note
                val state = states[note.id] ?: return@forEach
                val liftValue = state.lift.value
                val appearValue = state.appear.value

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
                            val born = 0.86f + 0.14f * appearValue
                            val scale = born * (1f + liftValue * LiftScale * motion.physicality)
                            scaleX = scale
                            scaleY = scale
                            alpha = appearValue.coerceIn(0f, 1f)
                            rotationZ = state.tilt.value * appearValue
                            transformOrigin = TransformOrigin.Center
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
                                    // El gesto llega en píxeles de pantalla; el
                                    // tablero vive en su propia escala.
                                    val boardDelta = dragAmount / boardState.scale
                                    accumulated += dragAmount
                                    tracker.addPosition(change.uptimeMillis, accumulated)
                                    val next = state.value + boardDelta
                                    state.value = next
                                    val lean = (dragAmount.x * 1.15f)
                                        .coerceIn(-MaxTiltDegrees, MaxTiltDegrees) * motion.physicality
                                    scope.launch {
                                        state.tilt.animateTo(state.resting + lean, motion.snappy())
                                    }
                                    val nowOverTrash =
                                        isOverTrash(boardState.boardToScreen(next), state.height)
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
                                        val current = state.value
                                        val predicted = current + Offset(
                                            velocity.x * InertiaSeconds / boardState.scale,
                                            velocity.y * InertiaSeconds / boardState.scale,
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
                                            minX = 0f,
                                            maxX = boardW - noteWidthPx,
                                            maxY = boardH - state.height.coerceAtLeast(1f),
                                            grid = gridPx,
                                            gap = stackGapPx,
                                            tolerance = snapTolerance,
                                            others = others,
                                        )
                                        val storedX = with(density) { settled.x.toDp().value }
                                        val storedY = with(density) { settled.y.toDp().value }
                                        feedback.settle()
                                        scope.launch { state.lift.animateTo(0f, motion.settle()) }
                                        scope.launch { state.tilt.animateTo(state.resting, motion.settle()) }
                                        scope.launch {
                                            state.settleTo(
                                                target = settled,
                                                spec = motion.settle(),
                                                initialVelocity = Offset(
                                                    velocity.x * VelocityHandover / boardState.scale,
                                                    velocity.y * VelocityHandover / boardState.scale,
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
        }

        // Los pedazos vuelan sobre la ventana, no sobre el tablero: así no se
        // encogen ni se recortan al estar alejado el zoom.
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
                modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter,
            ) { emptyContent() }
        }

        // Controles de zoom: el gesto de pinza es cómodo, pero nunca debe ser
        // la única forma de hacer algo.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            ZoomButton(RinconIcons.Plus, "Acercar") {
                boardState.zoomTo(boardState.scale * 1.35f, boardState.viewport.viewportCenter())
            }
            ZoomButton(RinconIcons.Grip, "Alejar") {
                boardState.zoomTo(boardState.scale / 1.35f, boardState.viewport.viewportCenter())
            }
            ZoomButton(RinconIcons.Undo, "Ver todo el tablero") {
                boardState.zoomTo(Board.MinScale + 0.05f, boardState.viewport.viewportCenter())
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
            Row(
                modifier = Modifier
                    .softShadow(if (overTrash) 16.dp else 8.dp, Rincon.shapes.pill)
                    .clip(Rincon.shapes.pill)
                    .background(bg)
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
                    if (overTrash) "Suelta para romperla" else "Arrastra aquí para tirar",
                    style = Rincon.type.navLabel,
                    color = if (overTrash) colors.accentInk else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Size.viewportCenter(): Offset = Offset(width / 2f, height / 2f)

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
 * Guías tenues del tablero.
 *
 * Sin ellas, alejar el zoom sobre un fondo liso no se siente como alejarse:
 * no hay nada que se haga pequeño. Estas líneas dan esa referencia espacial, y
 * el marco recuerda dónde termina el tablero.
 */
private fun Modifier.boardSurface(line: Color, edge: Color): Modifier = this.drawBehind {
    val step = 96.dp.toPx()
    val faint = line.copy(alpha = 0.35f)
    var x = step
    while (x < size.width) {
        drawLine(faint, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(faint, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
    drawRect(
        color = edge.copy(alpha = 0.6f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
    )
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
