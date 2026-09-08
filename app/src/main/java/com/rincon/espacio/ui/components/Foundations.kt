package com.rincon.espacio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.feedback.LocalFeedback
import kotlinx.coroutines.launch

/**
 * Compresión elástica al pulsar.
 *
 * Es la microinteracción más repetida de la app, así que vive en un solo sitio:
 * cualquier elemento pulsable se siente igual y con el mismo muelle.
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    hapticOnPress: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val motion = Rincon.motion
    val feedback = LocalFeedback.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled, onClick, onLongClick) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    scope.launch { scale.animateTo(pressedScale, motion.snappy()) }
                    val released = tryAwaitRelease()
                    scope.launch { scale.animateTo(1f, motion.playful()) }
                    if (!released) Unit
                },
                onTap = {
                    if (hapticOnPress) feedback.tap()
                    onClick()
                },
                onLongPress = onLongClick?.let { action -> { feedback.pop(); action() } },
            )
        }
}

/**
 * Sombra cálida y suave. En Rincón la sombra nunca es gris neutro: toma el tono
 * de la madera del tema, que es lo que hace que las tarjetas parezcan apoyadas
 * sobre una superficie y no recortadas sobre un fondo.
 */
@Composable
fun Modifier.softShadow(
    elevation: Dp,
    shape: Shape,
    ambientAlpha: Float = 0.10f,
    spotAlpha: Float = 0.16f,
): Modifier {
    val colors = Rincon.colors
    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = colors.shadow.copy(alpha = ambientAlpha),
        spotColor = colors.shadow.copy(alpha = spotAlpha),
    )
}

/** Superficie base de tarjeta: sombra suave + borde tenue + fondo del tema. */
@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    shape: Shape = Rincon.shapes.card,
    color: Color = Rincon.colors.surface,
    elevation: Dp = 6.dp,
    border: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = Rincon.colors
    Box(
        modifier = modifier
            .softShadow(elevation, shape)
            .clip(shape)
            .background(color)
            .then(
                if (border) Modifier.border(1.dp, colors.outlineSoft, shape) else Modifier
            )
    ) {
        content()
    }
}

/** Icono de la familia propia, con etiqueta accesible obligatoria o explícitamente nula. */
@Composable
fun RinconIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Rincon.colors.textPrimary,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = Rincon.type.section,
            color = Rincon.colors.textPrimary,
        )
        trailing?.invoke()
    }
}

/** Barra de progreso redondeada que se anima siempre con el mismo muelle. */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    trackColor: Color = Rincon.colors.surfaceSunken,
    fillColor: Color = Rincon.colors.accent,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = Rincon.motion.gentle(),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clearAndSetSemantics { }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(fillColor.copy(alpha = 0.82f), fillColor)
                    )
                )
        )
    }
}

@Composable
fun CozyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = Rincon.colors.accent,
) {
    val colors = Rincon.colors
    val bg = if (selected) accent else colors.surfaceSunken
    val fg = if (selected) colors.accentInk else colors.textSecondary
    Row(
        modifier = modifier
            .clip(Rincon.shapes.pill)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else colors.outlineSoft, Rincon.shapes.pill)
            .pressable(onClick = onClick)
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = Space.l, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        if (icon != null) RinconIcon(icon, null, tint = fg, size = 18.dp)
        Text(label, style = Rincon.type.label, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = Rincon.colors
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .softShadow(if (enabled) 8.dp else 0.dp, Rincon.shapes.pill, spotAlpha = 0.22f)
            .clip(Rincon.shapes.pill)
            .background(colors.accent.copy(alpha = alpha))
            .pressable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = Space.touch)
            .padding(horizontal = Space.xl, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            RinconIcon(icon, null, tint = colors.accentInk, size = 20.dp)
            Spacer(Modifier.width(Space.s))
        }
        Text(label, style = Rincon.type.bodyStrong, color = colors.accentInk)
    }
}

@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Rincon.colors.textSecondary,
) {
    Row(
        modifier = modifier
            .clip(Rincon.shapes.pill)
            .border(1.5.dp, Rincon.colors.outline, Rincon.shapes.pill)
            .pressable(onClick = onClick)
            .defaultMinSize(minHeight = Space.touch)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            RinconIcon(icon, null, tint = tint, size = 20.dp)
            Spacer(Modifier.width(Space.s))
        }
        Text(label, style = Rincon.type.bodyStrong, color = tint)
    }
}

/** Interruptor propio: pastilla blanda con muelle, sin aspecto Material. */
@Composable
fun CozySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = Rincon.colors
    val feedback = LocalFeedback.current
    val offset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = Rincon.motion.snappy(),
        label = "switch",
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(Rincon.shapes.pill)
            .background(
                if (checked) colors.accent else colors.surfaceSunken
            )
            .border(1.dp, if (checked) Color.Transparent else colors.outline, Rincon.shapes.pill)
            .pointerInput(checked) {
                detectTapGestures {
                    feedback.toggle()
                    onCheckedChange(!checked)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .graphicsLayer { translationX = offset * 24.dp.toPx() }
                .size(26.dp)
                .softShadow(3.dp, Rincon.shapes.pill, spotAlpha = 0.25f)
                .clip(Rincon.shapes.pill)
                .background(if (checked) colors.accentInk else colors.surface)
        )
        // El estado se comunica también por posición y forma, no sólo por color.
        Spacer(Modifier.size(0.dp).clearAndSetSemantics { })
    }
}

/**
 * Estado vacío: nunca una pantalla en blanco. Un icono grande, una frase amable
 * y, si procede, la acción que resuelve el vacío.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = Rincon.colors
    val breathe by animateFloatAsState(
        targetValue = 1f,
        animationSpec = Rincon.motion.playful(),
        label = "empty",
    )
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = breathe; scaleY = breathe }
                .size(96.dp)
                .clip(Rincon.shapes.panel)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surfaceSunken, colors.surface)
                    )
                )
                .border(1.dp, colors.outlineSoft, Rincon.shapes.panel),
            contentAlignment = Alignment.Center,
        ) {
            RinconIcon(icon, null, tint = colors.textMuted, size = 40.dp)
        }
        Spacer(Modifier.height(Space.l))
        Text(title, style = Rincon.type.cardTitle, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text(
            message,
            style = Rincon.type.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(Space.l))
            action()
        }
    }
}

/**
 * Fondo de la app: degradado cálido muy leve más una textura de puntos casi
 * imperceptible. Se dibuja una sola vez por composición (no hay animación
 * infinita) para que no cueste batería.
 */
@Composable
fun CozyBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = Rincon.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.backgroundTop,
                    0.45f to colors.background,
                    1f to colors.background,
                )
            )
            .drawBehind {
                val dot = colors.textMuted.copy(alpha = if (colors.isDark) 0.045f else 0.055f)
                val step = 26.dp.toPx()
                var y = step / 2
                var row = 0
                while (y < size.height) {
                    var x = if (row % 2 == 0) step / 2 else step
                    while (x < size.width) {
                        drawCircle(dot, radius = 1.15f, center = androidx.compose.ui.geometry.Offset(x, y))
                        x += step
                    }
                    y += step
                    row++
                }
            }
    ) {
        content()
    }
}
