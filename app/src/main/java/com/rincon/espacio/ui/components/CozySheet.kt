package com.rincon.espacio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space

/**
 * Panel inferior propio.
 *
 * Se apoya en [Dialog] para que quede por encima de todo y respete el botón
 * "atrás" y el teclado, pero el aspecto y el movimiento son de Rincón: entra
 * deslizándose con muelle, el fondo se oscurece de forma cálida y se cierra
 * tocando fuera o arrastrando el tirador.
 */
@Composable
fun CozySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    val colors = Rincon.colors
    val motion = Rincon.motion
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = motion.fade(),
        label = "scrim",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.shadow.copy(alpha = 0.42f * scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = shown,
                enter = slideInVertically(motion.offsetSpring()) { it } + fadeIn(motion.quickFade()),
                exit = slideOutVertically(motion.offsetSpring()) { it } + fadeOut(motion.quickFade()),
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .softShadow(24.dp, Rincon.shapes.sheet, spotAlpha = 0.3f)
                        .clip(Rincon.shapes.sheet)
                        .background(colors.surface)
                        // Los toques dentro del panel no deben cerrarlo.
                        .pointerInput(Unit) { detectTapGestures { } }
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    Column(Modifier.fillMaxWidth().padding(bottom = Space.l)) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = Space.m),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.outline)
                            )
                        }
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = Space.xl)) {
        androidx.compose.material3.Text(
            title,
            style = Rincon.type.title,
            color = Rincon.colors.textPrimary,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Space.xxs))
            androidx.compose.material3.Text(
                subtitle,
                style = Rincon.type.body,
                color = Rincon.colors.textSecondary,
            )
        }
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Rincon.colors.outlineSoft.copy(alpha = 0.9f))
    )
}

@Composable
fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(50)).background(color))
}
