package com.rincon.espacio

import android.app.Application
import com.rincon.espacio.di.AppContainer
import com.rincon.espacio.notifications.NotificationChannels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RinconApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
        NotificationChannels.ensure(this)

        container.applicationScope.launch {
            // Los ajustes de sonido/hápticos viven en DataStore pero los consume
            // una capa sin Compose, así que se sincronizan aquí una vez y en
            // cada cambio posterior.
            container.preferences.settings.collect { settings ->
                container.feedback.soundEnabled = settings.soundEnabled
                container.feedback.hapticsEnabled = settings.hapticsEnabled
                container.feedback.intensity = settings.soundIntensity
            }
        }

        container.applicationScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { container.reminderRepository.rescheduleAll() }
            }
        }
    }
}
