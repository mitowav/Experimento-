package com.rincon.espacio.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rincon.espacio.core.design.Rincon
import com.rincon.espacio.core.design.Space
import com.rincon.espacio.core.design.ThemeMode
import com.rincon.espacio.core.design.ThemePalette
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.core.feedback.SoundIntensity
import com.rincon.espacio.data.prefs.UiDensity
import com.rincon.espacio.notifications.ReminderTone
import com.rincon.espacio.ui.components.CozyChip
import com.rincon.espacio.ui.components.CozySwitch
import com.rincon.espacio.ui.components.CozyTextField
import com.rincon.espacio.ui.components.PalettePicker
import com.rincon.espacio.ui.components.PaperSurface
import com.rincon.espacio.ui.components.appear
import com.rincon.espacio.ui.components.SectionHeader
import com.rincon.espacio.ui.components.SegmentedControl
import com.rincon.espacio.ui.components.SettingRow
import com.rincon.espacio.ui.components.GhostButton
import com.rincon.espacio.ui.icons.RinconIcons
import com.rincon.espacio.ui.vm.SettingsViewModel

/**
 * "Yo": ajustes y accesos.
 *
 * Los permisos se explican antes de pedirlos, y sólo cuando sirven para algo
 * que la persona ya ha querido hacer.
 */
@Composable
fun MeScreen(
    viewModel: SettingsViewModel,
    onOpenGoals: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenStudy: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = Rincon.colors
    val context = LocalContext.current
    val feedback = LocalFeedback.current

    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Space.screen,
            end = Space.screen,
            bottom = bottomInset + Space.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = Space.l, bottom = Space.s).appear(0)) {
                Text("Tu rincón", style = Rincon.type.display, color = colors.textPrimary)
                Text("Ajusta cómo se ve y cómo se siente.", style = Rincon.type.body, color = colors.textSecondary)
            }
        }

        item {
            PaperSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.l)) {
                    Text("¿Cómo te llamamos?", style = Rincon.type.label, color = colors.textSecondary)
                    Spacer(Modifier.height(Space.s))
                    CozyTextField(
                        value = settings.displayName,
                        onValueChange = viewModel::setDisplayName,
                        placeholder = "Tu nombre (opcional)",
                        singleLine = true,
                    )
                }
            }
        }

        item {
            // Sin `weight`: cada botón ocupa lo que necesita su texto, y la
            // fila se desliza. Con la escala de fuente grande del sistema,
            // repartir el ancho a partes iguales parte las palabras.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                GhostButton("Objetivos", onOpenGoals, icon = RinconIcons.Target)
                GhostButton("Hábitos", onOpenHabits, icon = RinconIcons.Leaf)
                GhostButton("Estudio", onOpenStudy, icon = RinconIcons.Book)
            }
        }

        item { SectionHeader("Aspecto") }

        item {
            PaperSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = Space.s)) {
                    Column(Modifier.padding(horizontal = Space.xl, vertical = Space.s)) {
                        Text("Modo de color", style = Rincon.type.label, color = colors.textSecondary)
                        Spacer(Modifier.height(Space.s))
                        SegmentedControl(
                            options = ThemeMode.entries.toList(),
                            selected = settings.themeMode,
                            label = { it.label },
                            onSelect = viewModel::setThemeMode,
                        )
                        Spacer(Modifier.height(Space.l))
                        Text("Paleta", style = Rincon.type.label, color = colors.textSecondary)
                        Spacer(Modifier.height(Space.s))
                        PalettePicker(
                            selected = settings.palette,
                            dark = colors.isDark,
                            onSelect = viewModel::setPalette,
                        )
                        Spacer(Modifier.height(Space.l))
                        Text("Densidad", style = Rincon.type.label, color = colors.textSecondary)
                        Spacer(Modifier.height(Space.s))
                        SegmentedControl(
                            options = UiDensity.entries.toList(),
                            selected = settings.density,
                            label = { it.label },
                            onSelect = viewModel::setDensity,
                        )
                    }
                    SettingRow(
                        icon = RinconIcons.Sparkle,
                        title = "Detalles decorativos",
                        subtitle = "Textura de fondo y pequeños adornos",
                        trailing = {
                            CozySwitch(settings.decorations, viewModel::setDecorations)
                        },
                    )
                }
            }
        }

        item { SectionHeader("Cómo se siente") }

        item {
            PaperSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = Space.s)) {
                    SettingRow(
                        icon = RinconIcons.Speaker,
                        title = "Sonidos",
                        subtitle = "Pequeños sonidos de papel y confirmación",
                        trailing = {
                            CozySwitch(settings.soundEnabled, onCheckedChange = { value ->
                                viewModel.setSoundEnabled(value)
                                if (value) feedback.pop()
                            })
                        },
                    )
                    if (settings.soundEnabled) {
                        Column(Modifier.padding(horizontal = Space.xl, vertical = Space.s)) {
                            Text("Intensidad", style = Rincon.type.label, color = colors.textSecondary)
                            Spacer(Modifier.height(Space.s))
                            SegmentedControl(
                                options = SoundIntensity.entries.toList(),
                                selected = settings.soundIntensity,
                                label = { it.label },
                                onSelect = { value ->
                                    viewModel.setSoundIntensity(value)
                                    feedback.pop()
                                },
                            )
                        }
                    }
                    SettingRow(
                        icon = RinconIcons.Vibrate,
                        title = "Vibración",
                        subtitle = "Respuesta háptica muy breve al tocar",
                        trailing = {
                            CozySwitch(settings.hapticsEnabled, onCheckedChange = { value ->
                                viewModel.setHaptics(value)
                                if (value) feedback.tap()
                            })
                        },
                    )
                    SettingRow(
                        icon = RinconIcons.Undo,
                        title = "Reducir movimiento",
                        subtitle = "Animaciones más cortas y sin inclinaciones",
                        trailing = {
                            CozySwitch(settings.reduceMotion, viewModel::setReduceMotion)
                        },
                    )
                }
            }
        }

        item { SectionHeader("Avisos") }

        item {
            PaperSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.l)) {
                    Text("Tono", style = Rincon.type.label, color = colors.textSecondary)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        settings.reminderTone.description,
                        style = Rincon.type.caption,
                        color = colors.textMuted,
                    )
                    Spacer(Modifier.height(Space.m))
                    // Se escucha al tocarlo: elegir un tono sin oírlo es
                    // elegir a ciegas.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        ReminderTone.entries.forEach { tone ->
                            CozyChip(
                                label = tone.label,
                                selected = settings.reminderTone == tone,
                                icon = RinconIcons.Bell,
                                onClick = {
                                    viewModel.setReminderTone(tone)
                                    feedback.previewTone(tone.rawRes)
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            PaperSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = Space.s)) {
                    SettingRow(
                        icon = RinconIcons.Bell,
                        title = if (notificationsGranted) "Notificaciones activadas" else "Activar notificaciones",
                        subtitle = if (notificationsGranted)
                            "Recibirás los recordatorios que crees"
                        else "Hacen falta para avisarte de tus recordatorios",
                        onClick = if (notificationsGranted) null else {
                            {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsGranted = true
                                }
                            }
                        },
                        trailing = {
                            if (!notificationsGranted) {
                                Text("Permitir", style = Rincon.type.label, color = colors.accent)
                            }
                        },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingRow(
                            icon = RinconIcons.Clock,
                            title = "Avisos a la hora exacta",
                            subtitle = "Android pide permiso aparte para alarmas exactas. " +
                                "Sin él, los avisos pueden llegar unos minutos tarde.",
                            onClick = { openExactAlarmSettings(context) },
                            trailing = {
                                Text("Abrir", style = Rincon.type.label, color = colors.accent)
                            },
                        )
                    }
                }
            }
        }

        item {
            PaperSurface(Modifier.fillMaxWidth(), elevation = 3.dp) {
                Column(Modifier.padding(Space.l)) {
                    Text("Tus datos son tuyos", style = Rincon.type.bodyStrong, color = colors.textPrimary)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Rincón guarda todo en tu teléfono. No hay cuentas, ni servidores, " +
                            "ni analítica: la app funciona igual sin conexión.",
                        style = Rincon.type.body,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        item {
            Text(
                "Rincón · versión 1.0.0",
                style = Rincon.type.caption,
                color = colors.textMuted,
                modifier = Modifier.padding(top = Space.m),
            )
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
