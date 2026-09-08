package com.rincon.espacio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.ui.icons.RinconIcons

/**
 * Deshacer.
 *
 * Aparece deslizándose desde abajo y se retira sola a los pocos segundos. No
 * pregunta ni bloquea: si no haces nada, el borrado se queda hecho.
 */
@Composable
fun UndoBar(
    visible: Boolean,
    label: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val motion = Rincon.motion

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(motion.offsetSpring()) { it } + fadeIn(motion.fade()),
        exit = slideOutVertically(motion.offsetSpring()) { it } + fadeOut(motion.quickFade()),
    ) {
        Row(
            modifier = Modifier
                .softShadow(12.dp, Rincon.shapes.pill)
                .clip(Rincon.shapes.pill)
                .background(colors.surface)
                .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
                .padding(start = Space.l, end = Space.s, top = Space.s, bottom = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Text(label, style = Rincon.type.navLabel, color = colors.textSecondary, maxLines = 1)
            Row(
                modifier = Modifier
                    .clip(Rincon.shapes.pill)
                    .background(colors.accentSoft)
                    .pressable(onClick = onUndo)
                    .padding(horizontal = Space.m, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                RinconIcon(RinconIcons.Undo, null, tint = colors.accent, size = 18.dp)
                Text("Deshacer", style = Rincon.type.navLabel, color = colors.accent, maxLines = 1)
            }
        }
    }
}
