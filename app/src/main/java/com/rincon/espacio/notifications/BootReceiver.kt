package com.rincon.espacio.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rincon.espacio.di.AppContainer
import com.rincon.espacio.di.launchSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Las alarmas no sobreviven a un reinicio, a un cambio de hora ni a una
 * actualización de la app: aquí se vuelven a programar todas.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
        if (!relevant) return

        val pending = goAsync()
        val container = AppContainer.get(context)
        container.applicationScope.launchSafely(pending) {
            withContext(Dispatchers.IO) { container.reminderRepository.rescheduleAll() }
        }
    }
}
