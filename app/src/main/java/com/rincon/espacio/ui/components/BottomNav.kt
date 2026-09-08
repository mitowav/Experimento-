package com.rincon.espacio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.ui.navigation.TabDestination

/**
 * Barra inferior: una pastilla flotante, no una barra pegada al borde.
 *
 * La pestaña activa se marca con fondo, icono teñido y una etiqueta que aparece;
 * nunca sólo con color, para que se entienda sin depender de la vista cromática.
 */
@Composable
fun RinconBottomBar(
    current: String?,
    onSelect: (TabDestination) -> Unit,
    modifier: Modifier = Modifier,
    onHeightChanged: (Dp) -> Unit = {},
) {
    val colors = Rincon.colors
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            // La altura depende de la barra del sistema y de la escala de texto
            // del teléfono, así que no se puede suponer: se mide y se comunica
            // hacia arriba para que nada quede debajo.
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) }
            .navigationBarsPadding()
            .padding(horizontal = Space.l, vertical = Space.m),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(14.dp, Rincon.shapes.pill, spotAlpha = 0.2f)
                .clip(Rincon.shapes.pill)
                .background(colors.surface)
                .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabDestination.entries.forEach { tab ->
                val selected = current == tab.route
                val bg by animateColorAsState(
                    targetValue = if (selected) colors.accentSoft else Color.Transparent,
                    animationSpec = Rincon.motion.fade(),
                    label = "tabBg",
                )
                val lift by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = Rincon.motion.playful(),
                    label = "tabLift",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(Rincon.shapes.pill)
                        .background(bg)
                        .pressable(hapticOnPress = true) { onSelect(tab) }
                        .padding(vertical = Space.s)
                        .semantics {
                            contentDescription = if (selected) "${tab.label}, seleccionado" else tab.label
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { translationY = -lift * 2.dp.toPx() },
                        contentAlignment = Alignment.Center,
                    ) {
                        RinconIcon(
                            icon = tab.icon,
                            contentDescription = null,
                            tint = if (selected) colors.accent else colors.textMuted,
                            size = 23.dp,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = Rincon.type.navLabel,
                        color = if (selected) colors.accent else colors.textMuted,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Botón "+" principal: grande, cómodo con el pulgar y con respuesta elástica. */
@Composable
fun FloatingAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Crear",
    icon: androidx.compose.ui.graphics.vector.ImageVector = com.rincon.espacio.ui.icons.RinconIcons.Plus,
) {
    val colors = Rincon.colors
    Box(
        modifier = modifier
            .size(62.dp)
            .softShadow(16.dp, RoundedCornerShape(24.dp), spotAlpha = 0.28f)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.accent)
            .pressable(pressedScale = 0.9f, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RinconIcon(icon, null, tint = colors.accentInk, size = 30.dp)
    }
}
