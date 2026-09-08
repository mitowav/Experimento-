package com.rincon.espacio.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rincon.espacio.core.design.PaperColor
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.components.PrimaryButton
import com.rincon.espacio.ui.components.RinconIcon
import com.rincon.espacio.ui.components.softShadow
import com.rincon.espacio.ui.icons.RinconIcons

private data class OnboardingStep(
    val title: String,
    val body: String,
    val iconKey: String,
    val colorKey: String,
)

private val steps = listOf(
    OnboardingStep(
        title = "Tu espacio para organizar tu vida.",
        body = "Notas, tareas, objetivos y hábitos en un sitio que apetece abrir.",
        iconKey = "sparkle",
        colorKey = "Butter",
    ),
    OnboardingStep(
        title = "Arrastra tus notas.",
        body = "Son papeles de verdad: se cogen, se mueven y se quedan donde las dejas.",
        iconKey = "note",
        colorKey = "Peach",
    ),
    OnboardingStep(
        title = "Organiza tu día.",
        body = "Pon fecha y hora a lo que importa y deja que la app te avise.",
        iconKey = "sun",
        colorKey = "Sky",
    ),
    OnboardingStep(
        title = "Alcanza tus objetivos.",
        body = "Divide lo grande en pasos pequeños y ve cómo avanza.",
        iconKey = "target",
        colorKey = "Sage",
    ),
)

/** Introducción corta: cuatro frases y dentro. Se puede saltar en cualquier momento. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Rincon.colors
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val paper = PaperColor.fromKey(step.colorKey)

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = Space.m), horizontalArrangement = Arrangement.End) {
            if (index < steps.lastIndex) {
                GhostButton("Saltar", onFinish)
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (scaleIn(Rincon.motion.gentle(), initialScale = 0.9f) + fadeIn(Rincon.motion.fade()))
                    .togetherWith(fadeOut(Rincon.motion.quickFade()))
            },
            label = "onboarding",
        ) { current ->
            val currentStep = steps[current]
            val currentPaper = PaperColor.fromKey(currentStep.colorKey)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer { rotationZ = if (current % 2 == 0) -4f else 4f }
                        .softShadow(18.dp, RoundedCornerShape(34.dp), spotAlpha = 0.24f)
                        .clip(RoundedCornerShape(34.dp))
                        .background(currentPaper.paper(colors.isDark)),
                    contentAlignment = Alignment.Center,
                ) {
                    RinconIcon(
                        RinconIcons.byKey(currentStep.iconKey),
                        null,
                        tint = currentPaper.ink(colors.isDark),
                        size = 60.dp,
                    )
                }
                Spacer(Modifier.height(Space.xxl))
                Text(
                    currentStep.title,
                    style = Rincon.type.title,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.m))
                Text(
                    currentStep.body,
                    style = Rincon.type.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.padding(bottom = Space.xl),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.indices.forEach { i ->
                val active = i == index
                val width by animateFloatAsState(
                    targetValue = if (active) 26f else 8f,
                    animationSpec = Rincon.motion.gentle(),
                    label = "dot",
                )
                Box(
                    Modifier
                        .width(width.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) colors.accent else colors.outline)
                )
            }
        }

        PrimaryButton(
            label = if (index == steps.lastIndex) "Entrar a mi rincón" else "Siguiente",
            icon = if (index == steps.lastIndex) RinconIcons.Check else RinconIcons.ChevronRight,
            onClick = { if (index == steps.lastIndex) onFinish() else index++ },
            modifier = Modifier.fillMaxWidth().padding(bottom = Space.xxl),
        )
    }
}
