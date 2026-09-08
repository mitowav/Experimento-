package com.rincon.espacio.ui.screens.canvas

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.FloatingAddButton
import com.rincon.espacio.ui.components.NoteCanvas
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.NotesViewModel

/**
 * El escritorio: la pantalla que da identidad a Rincón.
 *
 * No hay listas aquí. Hay papeles, y se colocan donde uno quiera.
 */
@Composable
fun DeskScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val colors = Rincon.colors

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = Space.screen, vertical = Space.m)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tu escritorio", style = Rincon.type.title, color = colors.textPrimary)
                        Text(
                            when (val count = state.notes.size) {
                                0 -> "Todavía está vacío"
                                1 -> "1 nota"
                                else -> "$count notas"
                            },
                            style = Rincon.type.body,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                NoteCanvas(
                    items = state.notes,
                    onOpen = viewModel::openNote,
                    onToggleDone = viewModel::toggleDone,
                    onPlacementChange = viewModel::savePlacement,
                    onDelete = viewModel::deleteNote,
                    emptyContent = {
                        EmptyState(
                            icon = RinconIcons.Note,
                            title = "Aquí no hay nada todavía",
                            message = "Crea tu primera nota con el botón +.\nLuego arrástrala donde quieras.",
                        )
                    },
                )
            }

            Spacer(Modifier.height(bottomInset))
        }

        FloatingAddButton(
            onClick = { viewModel.startNewNote(xFraction = 0.12f, y = 40f) },
            description = "Crear nota",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.screen, bottom = bottomInset + Space.l),
        )
    }

    NoteEditorSheet(
        draft = draft,
        subjects = state.subjects,
        goals = state.goals,
        viewModel = viewModel,
    )
}
