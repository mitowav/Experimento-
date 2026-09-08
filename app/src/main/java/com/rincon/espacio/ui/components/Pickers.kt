package com.rincon.espacio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.PaperStyle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.design.ThemePalette
import com.rincon.espacio.core.feedback.LocalFeedback

/**
 * Selector de estilo de papel.
 *
 * Cada opción se dibuja tal y como se va a ver, en pequeño. Enseñar el
 * resultado siempre gana a nombrarlo: nadie tiene que imaginar qué es
 * "reciclado".
 */
@Composable
fun PaperStylePicker(
    selectedStyle: String,
    paperColorKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val dark = colors.isDark
    val paper = PaperColor.fromKey(paperColorKey)
    val feedback = LocalFeedback.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        contentPadding = PaddingValues(horizontal = Space.xl),
    ) {
        items(PaperStyle.entries.toList(), key = { it.name }) { style ->
            val isSelected = style.name == selectedStyle
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.9f,
                animationSpec = Rincon.motion.playful(),
                label = "styleScale",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = 58.dp, height = 46.dp)
                        .scaled(scale)
                        .softShadow(if (isSelected) 7.dp else 2.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(paper.paper(dark))
                        .drawBehind { drawStylePreview(style, paper.ink(dark), paper.edge(dark)) }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.accent else paper.edge(dark),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .pressable(hapticOnPress = false) { feedback.click(); onSelect(style.name) }
                        .semantics { contentDescription = "Papel ${style.label}" },
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    style.label,
                    style = Rincon.type.navLabel,
                    color = if (isSelected) colors.accent else colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStylePreview(
    style: PaperStyle,
    ink: Color,
    edge: Color,
) {
    when (style) {
        PaperStyle.Plain -> Unit
        PaperStyle.Lined -> {
            var y = size.height / 4f
            while (y < size.height) {
                drawLine(ink.copy(alpha = 0.22f), Offset(6f, y), Offset(size.width - 6f, y), 1.4f)
                y += size.height / 4f
            }
        }
        PaperStyle.Grid -> {
            val step = size.width / 5f
            var v = step
            while (v < size.width) {
                drawLine(ink.copy(alpha = 0.16f), Offset(v, 0f), Offset(v, size.height), 1f)
                v += step
            }
            var h = step
            while (h < size.height) {
                drawLine(ink.copy(alpha = 0.16f), Offset(0f, h), Offset(size.width, h), 1f)
                h += step
            }
        }
        PaperStyle.Sticky -> {
            drawRect(ink.copy(alpha = 0.08f), size = Size(size.width, size.height * 0.3f))
            drawRect(
                Color.White.copy(alpha = 0.28f),
                topLeft = Offset(size.width * 0.28f, -2f),
                size = Size(size.width * 0.44f, size.height * 0.22f),
            )
        }
        PaperStyle.Recycled -> {
            repeat(10) { i ->
                val x = (i * 37 % size.width.toInt()).toFloat()
                val y = (i * 23 % size.height.toInt()).toFloat()
                drawLine(ink.copy(alpha = 0.16f), Offset(x, y), Offset(x + 6f, y + 3f), 1f)
            }
            val depth = size.height * 0.16f
            val steps = 7
            for (i in 0 until steps) {
                val w = size.width / steps
                drawRect(
                    edge.copy(alpha = 0.0f),
                    topLeft = Offset(i * w, size.height - depth),
                    size = Size(w, depth),
                )
            }
        }
    }
}

/**
 * Selector de paleta.
 *
 * Con seis temas, una fila de botones de texto se rompe en cuanto la escala de
 * fuente crece. Cada paleta se muestra como una muestra de sí misma: se elige
 * por lo que se ve, y el nombre acompaña.
 */
@Composable
fun PalettePicker(
    selected: ThemePalette,
    dark: Boolean,
    onSelect: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val feedback = LocalFeedback.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        items(ThemePalette.entries.toList(), key = { it.name }) { palette ->
            val preview = palette.colors(dark)
            val isSelected = palette == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.93f,
                animationSpec = Rincon.motion.playful(),
                label = "paletteScale",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 52.dp)
                        .scaled(scale)
                        .softShadow(if (isSelected) 8.dp else 3.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(preview.background)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.accent else preview.outline,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .pressable(hapticOnPress = false) { feedback.click(); onSelect(palette) }
                        .semantics { contentDescription = "Paleta ${palette.label}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier
                                .size(width = 16.dp, height = 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preview.accent)
                        )
                        Box(
                            Modifier
                                .size(width = 16.dp, height = 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preview.surface)
                        )
                        Box(
                            Modifier
                                .size(width = 16.dp, height = 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preview.surfaceSunken)
                        )
                    }
                }
                Spacer(Modifier.height(Space.xs))
                Text(
                    palette.label,
                    style = Rincon.type.navLabel,
                    color = if (isSelected) colors.accent else colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(66.dp),
                )
            }
        }
    }
}
