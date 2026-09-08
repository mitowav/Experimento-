package com.rincon.espacio.ui.screens.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
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
import com.rincon.espacio.ui.components.UndoBar
import com.rincon.espacio.ui.components.rememberBoardState
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.NotesViewModel

/**
 * El escritorio.
 *
 * La pantalla más despojada de la app a propósito: un título, tus papeles y un
 * botón. Todo lo demás (zoom, papelera, atajos) aparece sólo cuando hace falta
 * y se va solo cuando deja de hacer falta.
 */
@Composable
fun DeskScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val deleted by viewModel.recentlyDeleted.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    val density = LocalDensity.current
    val board = rememberBoardState()

    // El encabezado flota sobre el tablero: hay que medirlo para que el
    // escritorio encuadre las notas debajo de él y no detrás.
    var headerHeight by remember { mutableStateOf(104.dp) }

    Box(modifier.fillMaxSize()) {
        NoteCanvas(
            items = state.notes,
            boardState = board,
            onOpen = viewModel::openNote,
            onToggleDone = viewModel::toggleDone,
            onPlacementChange = viewModel::savePlacement,
            onDelete = viewModel::deleteNote,
            topInset = headerHeight,
            bottomInset = bottomInset,
            emptyContent = {
                EmptyState(
                    icon = RinconIcons.Note,
                    title = "Tu escritorio está vacío",
                    message = "Crea tu primera nota con el botón +.",
                )
            },
        )

        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeight = with(density) { it.height.toDp() } }
                .background(
                    Brush.verticalGradient(
                        0f to colors.background,
                        0.6f to colors.background.copy(alpha = 0.85f),
                        1f to Color.Transparent,
                    )
                )
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = Space.screen, end = Space.screen, top = Space.s, bottom = Space.xxl)
                    .appear(0),
            ) {
                Text(
                    "Tu escritorio",
                    style = Rincon.type.title,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        UndoBar(
            visible = deleted != null,
            label = "Nota rota",
            onUndo = viewModel::undoDelete,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Space.screen, bottom = bottomInset + Space.m),
        )

        FloatingAddButton(
            onClick = {
                val center = board.visibleCenterDp(density)
                // Nace centrada en lo que estás viendo, no en una esquina fija.
                viewModel.startNewNote(center.x - 84f, center.y - 90f)
            },
            description = "Crear nota",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.m),
        )
    }

    NoteEditorSheet(
        draft = draft,
        subjects = state.subjects,
        goals = state.goals,
        viewModel = viewModel,
    )
}
