package com.rincon.espacio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rincon.espacio.core.design.RinconTheme
import com.rincon.espacio.core.feedback.LocalFeedback
import com.rincon.espacio.ui.components.CozyBackground
import com.rincon.espacio.ui.navigation.RinconNavHost
import com.rincon.espacio.ui.screens.onboarding.OnboardingScreen
import com.rincon.espacio.ui.vm.RinconViewModelFactory
import com.rincon.espacio.ui.vm.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as RinconApp).container
        val factory = RinconViewModelFactory(container)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            RinconTheme(
                mode = settings.themeMode,
                palette = settings.palette,
                reduceMotion = settings.reduceMotion,
            ) {
                CompositionLocalProvider(LocalFeedback provides container.feedback) {
                    Box(Modifier.fillMaxSize()) {
                        RinconNavHost(
                            factory = factory,
                            settingsViewModel = settingsViewModel,
                            reduceMotion = settings.reduceMotion,
                        )

                        AnimatedVisibility(
                            visible = !settings.onboardingDone,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            CozyBackground(Modifier.fillMaxSize()) {
                                OnboardingScreen(onFinish = settingsViewModel::finishOnboarding)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_TARGET_TYPE = "reminder_target_type"
        const val EXTRA_REMINDER_TARGET_ID = "reminder_target_id"
    }
}
