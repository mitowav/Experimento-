package com.rincon.espacio.di

import android.content.BroadcastReceiver
import android.content.Context
import com.rincon.espacio.data.local.RinconDatabase
import com.rincon.espacio.data.prefs.PreferencesRepository
import com.rincon.espacio.data.repo.EventRepository
import com.rincon.espacio.data.repo.GoalRepository
import com.rincon.espacio.data.repo.HabitRepository
import com.rincon.espacio.data.repo.NoteRepository
import com.rincon.espacio.data.repo.ReminderRepository
import com.rincon.espacio.data.repo.StudyRepository
import com.rincon.espacio.core.feedback.FeedbackController
import com.rincon.espacio.notifications.AlarmReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * Contenedor de dependencias hecho a mano.
 *
 * Se descarta Hilt a propósito: el grafo de esta app es pequeño y estable, y un
 * contenedor explícito evita una capa de generación de código, acelera la
 * compilación y hace que se pueda leer de un vistazo quién depende de quién.
 * Si el proyecto creciera hasta necesitar ámbitos y multi-módulo, migrar a Hilt
 * sería un cambio localizado en este archivo.
 */
class AppContainer private constructor(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: RinconDatabase by lazy { RinconDatabase.build(appContext) }

    val preferences: PreferencesRepository by lazy { PreferencesRepository(appContext) }

    val feedback: FeedbackController by lazy { FeedbackController(appContext) }

    val scheduler: AlarmReminderScheduler by lazy {
        AlarmReminderScheduler(appContext, database.reminderDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), scheduler)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao(), reminderRepository)
    }

    val goalRepository: GoalRepository by lazy { GoalRepository(database.goalDao()) }

    val habitRepository: HabitRepository by lazy {
        HabitRepository(database.habitDao(), reminderRepository)
    }

    val studyRepository: StudyRepository by lazy {
        StudyRepository(database.studyDao(), reminderRepository)
    }

    val eventRepository: EventRepository by lazy {
        EventRepository(database.eventDao(), reminderRepository)
    }

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}

/**
 * Ejecuta trabajo asíncrono desde un BroadcastReceiver liberando siempre el
 * `PendingResult`, incluso si algo falla.
 */
fun CoroutineScope.launchSafely(
    pending: BroadcastReceiver.PendingResult,
    block: suspend () -> Unit,
) {
    launch {
        try {
            block()
        } catch (t: Throwable) {
            android.util.Log.w("Rincon", "Trabajo en segundo plano fallido", t)
        } finally {
            runCatching { pending.finish() }
        }
    }
}
