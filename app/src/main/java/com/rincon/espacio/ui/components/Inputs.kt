package com.rincon.espacio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.domain.model.Priority
import com.rincon.espacio.ui.icons.RinconIcons

/** Campo de texto con aspecto de papel, sin la estética de Material. */
@Composable
fun CozyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = Rincon.type.body,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    /** Pide el foco al aparecer: escribir es lo primero que se quiere hacer. */
    autoFocus: Boolean = false,
) {
    val colors = Rincon.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }
    Box(
        modifier = modifier
            .clip(Rincon.shapes.chip)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.outlineSoft, Rincon.shapes.chip)
            .padding(horizontal = Space.l, vertical = Space.m),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = colors.textMuted,
                        fontStyle = FontStyle.Italic,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Rincon.type.label,
        color = Rincon.colors.textSecondary,
        modifier = modifier.padding(bottom = Space.s),
    )
}

/** Selector de color de papel: muestras físicas, no puntos abstractos. */
@Composable
fun PaperColorPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = Rincon.colors.isDark
    val feedback = LocalFeedback.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.xl),
    ) {
        items(PaperColor.entries.toList(), key = { it.name }) { color ->
            val isSelected = color.name == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.86f,
                animationSpec = Rincon.motion.playful(),
                label = "swatch",
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scaled(scale)
                    .softShadow(if (isSelected) 8.dp else 3.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.paper(dark))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) color.ink(dark).copy(alpha = 0.65f) else color.edge(dark),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .pressable(hapticOnPress = false) { feedback.click(); onSelect(color.name) }
                    .semantics { contentDescription = "Color ${color.label}" },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    RinconIcon(RinconIcons.Check, null, tint = color.ink(dark), size = 22.dp)
                }
            }
        }
    }
}

@Composable
fun IconPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val feedback = LocalFeedback.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.xl),
    ) {
        items(RinconIcons.catalog, key = { it.first }) { (key, icon) ->
            val isSelected = key == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) colors.accent else colors.surfaceSunken,
                animationSpec = Rincon.motion.fade(),
                label = "iconBg",
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) Color.Transparent else colors.outlineSoft, RoundedCornerShape(16.dp))
                    .pressable(hapticOnPress = false) { feedback.click(); onSelect(key) }
                    .semantics { contentDescription = RinconIcons.labelFor(key) },
                contentAlignment = Alignment.Center,
            ) {
                RinconIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = if (isSelected) colors.accentInk else colors.textSecondary,
                    size = 24.dp,
                )
            }
        }
    }
}

@Composable
fun PriorityPicker(
    selected: Priority,
    onSelect: (Priority) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Priority.entries.forEach { priority ->
            CozyChip(
                label = priority.label,
                selected = priority == selected,
                onClick = { onSelect(priority) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Control segmentado genérico para opciones excluyentes cortas. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rincon.colors
    val feedback = LocalFeedback.current
    Row(
        modifier = modifier
            .clip(Rincon.shapes.pill)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.outlineSoft, Rincon.shapes.pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) colors.surface else Color.Transparent,
                animationSpec = Rincon.motion.fade(),
                label = "segment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(Rincon.shapes.pill)
                    .background(bg)
                    .then(
                        if (isSelected) Modifier.softShadow(3.dp, Rincon.shapes.pill, spotAlpha = 0.18f)
                        else Modifier
                    )
                    .pressable(hapticOnPress = false) { feedback.click(); onSelect(option) }
                    .defaultMinSize(minHeight = 42.dp)
                    .padding(horizontal = 4.dp, vertical = Space.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = Rincon.type.navLabel,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Fila de ajuste: icono grande, título, descripción y control a la derecha. */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Rincon.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = Space.xl, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            RinconIcon(icon, null, tint = colors.textSecondary, size = 22.dp)
        }
        Spacer(Modifier.width(Space.l))
        Column(Modifier.weight(1f)) {
            Text(title, style = Rincon.type.bodyStrong, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = Rincon.type.caption, color = colors.textSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.m))
            trailing()
        }
    }
}

/** Pequeño helper para escalar sin repetir `graphicsLayer` por todas partes. */
fun Modifier.scaled(scale: Float): Modifier =
    this.graphicsLayer(scaleX = scale, scaleY = scale)

@Composable
fun StepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    suffix: String = "",
    step: Int = 1,
) {
    val colors = Rincon.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Rincon.type.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
        StepperButton(RinconIcons.ChevronDown, "Menos") {
            onChange((value - step).coerceIn(range))
        }
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            Text("$value$suffix", style = Rincon.type.bodyStrong, color = colors.textPrimary)
        }
        StepperButton(RinconIcons.ChevronUp, "Más") {
            onChange((value + step).coerceIn(range))
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = Rincon.colors
    Box(
        modifier = Modifier
            .size(Space.touch)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceSunken)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RinconIcon(icon, null, tint = colors.textSecondary, size = 20.dp)
    }
}
