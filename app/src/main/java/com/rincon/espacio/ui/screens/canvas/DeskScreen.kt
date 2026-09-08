package com.rincon.espacio.ui.screens.canvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.NoteCanvas
import com.rincon.espacio.ui.components.appear
import com.rincon.espacio.ui.components.rememberBoardState
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.NotesViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * El escritorio: la pantalla que da identidad a Rincón.
 *
 * No hay listas aquí. Hay papeles sobre un tablero grande, y una ventana que se
 * pasea por él. Al salir se recuerda dónde estabas mirando y con cuánto zoom.
 */
@OptIn(FlowPreview::class)
@Composable
fun DeskScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val savedView by viewModel.deskView.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    val density = LocalDensity.current
    val board = rememberBoardState()

    // Restaurar la vista guardada una sola vez, cuando ya se conoce el tamaño.
    LaunchedEffect(Unit) {
        snapshotFlow { board.viewport }
            .collect { viewport ->
                if (viewport.width > 0f && board.scale == 1f && board.offset == Offset.Zero) {
                    board.scale = savedView.scale.coerceIn(0.35f, 2.2f)
                    board.offset = board.clamp(Offset(savedView.offsetX, savedView.offsetY))
                    return@collect
                }
            }
    }

    // Y guardarla mientras se usa, sin escribir en disco en cada fotograma.
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(board.scale, board.offset.x, board.offset.y) }
            .debounce(450)
            .collect { (scale, x, y) -> viewModel.rememberDeskView(scale, x, y) }
    }

    val zoomLabel = (board.scale * 100).toInt()
    val headerAlpha by animateFloatAsState(
        targetValue = if (board.scale < 0.6f) 0.45f else 1f,
        animationSpec = Rincon.motion.fade(),
        label = "header",
    )

    Box(modifier.fillMaxSize()) {
        NoteCanvas(
            items = state.notes,
            boardState = board,
            onOpen = viewModel::openNote,
            onToggleDone = viewModel::toggleDone,
            onPlacementChange = viewModel::savePlacement,
            onDelete = viewModel::deleteNote,
            bottomInset = bottomInset,
            emptyContent = {
                EmptyState(
                    icon = RinconIcons.Note,
                    title = "Aquí no hay nada todavía",
                    message = "Crea tu primera nota con el botón +.\nLuego arrástrala donde quieras.",
                )
            },
        )

        // El encabezado flota sobre el tablero, así que necesita un velo que
        // se funda hacia abajo: sin él, el título compite con las notas y las
        // guías que pasan por detrás y ninguno de los dos se lee bien.
        Box(
            Modifier
                .fillMaxWidth()
                .height(148.dp)
                .background(
                    Brush.verticalGradient(
                        0f to colors.background,
                        0.55f to colors.background.copy(alpha = 0.88f),
                        1f to Color.Transparent,
                    )
                )
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = Space.screen, vertical = Space.m)
                .appear(0),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tu escritorio",
                        style = Rincon.type.title,
                        color = colors.textPrimary.copy(alpha = headerAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(
                                when (val count = state.notes.size) {
                                    0 -> "Todavía está vacío"
                                    1 -> "1 nota"
                                    else -> "$count notas"
                                }
                            )
                            if (zoomLabel != 100) append(" · $zoomLabel%")
                        },
                        style = Rincon.type.body,
                        color = colors.textSecondary.copy(alpha = headerAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        FloatingAddButton(
            onClick = {
                val center = board.visibleCenterDp(density)
                // Nace centrada en lo que estás viendo, no encima del dedo.
                viewModel.startNewNote(center.x - 84f, center.y - 70f)
            },
            description = "Crear nota",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.m),
        )

        Spacer(Modifier.height(bottomInset))
    }

    NoteEditorSheet(
        draft = draft,
        subjects = state.subjects,
        goals = state.goals,
        viewModel = viewModel,
    )
}
