package com.rincon.espacio.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.ui.components.CozySheet
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.EmptyState
import com.rincon.espacio.ui.components.SheetTitle
import com.rincon.espacio.ui.components.TaskRow
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.NotesViewModel
import java.text.Normalizer

/**
 * Buscar.
 *
 * Una app de notas que no encuentra lo que escribiste no sirve de nada. La
 * búsqueda ignora tildes y mayúsculas —nadie escribe "matemáticas" con tilde
 * cuando busca con prisa— y mira también dentro de los pasos de cada nota.
 */
@Composable
fun SearchSheet(
    visible: Boolean,
    notesViewModel: NotesViewModel,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val state by notesViewModel.state.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    var query by remember { mutableStateOf("") }

    val results = remember(query, state.notes) {
        val needle = query.simplify()
        if (needle.isBlank()) emptyList() else state.notes.filter { item ->
            item.note.text.simplify().contains(needle) ||
                item.subtasks.any { it.text.simplify().contains(needle) }
        }
    }

    CozySheet(visible = true, onDismiss = onDismiss) {
        Column {
            SheetTitle("Buscar")
            Spacer(Modifier.height(Space.m))
            Column(Modifier.padding(horizontal = Space.xl)) {
                CozyTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Escribe para buscar...",
                    singleLine = true,
                    imeAction = ImeAction.Search,
                )
            }
            Spacer(Modifier.height(Space.m))

            when {
                query.isBlank() -> EmptyState(
                    icon = RinconIcons.Sparkle,
                    title = "¿Qué buscas?",
                    message = "Busca por lo que escribiste en cualquier nota.",
                )

                results.isEmpty() -> EmptyState(
                    icon = RinconIcons.Note,
                    title = "Nada por aquí",
                    message = "No hay ninguna nota con “$query”.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Space.xl,
                        end = Space.xl,
                        bottom = Space.l,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    items(results, key = { it.note.id }) { item ->
                        TaskRow(
                            note = item.note,
                            subtaskProgress = item.progress.takeIf { item.subtasks.isNotEmpty() },
                            onToggle = {
                                notesViewModel.toggleDone(item.note.id, !item.note.done)
                            },
                            onOpen = {
                                notesViewModel.openNote(item.note.id)
                                onDismiss()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.s))
        }
    }
}

/** Sin tildes y en minúsculas: buscar no debería exigir escribir perfecto. */
private fun String.simplify(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase()
        .trim()
