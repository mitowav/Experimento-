package com.rincon.espacio.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rincon.espacio.di.AppContainer
import com.rincon.espacio.domain.usecase.ObserveDay

/**
 * Fábrica única de ViewModels.
 *
 * Al no usar Hilt, este es el punto donde el grafo de dependencias se conecta
 * con la UI. Es explícito y cabe en una pantalla.
 */
class RinconViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    private val observeDay by lazy {
        ObserveDay(
            notes = container.noteRepository,
            events = container.eventRepository,
            study = container.studyRepository,
            habits = container.habitRepository,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        when (modelClass) {
            SettingsViewModel::class.java -> SettingsViewModel(
                preferences = container.preferences,
                backup = container.backupRepository,
            )
            NotesViewModel::class.java -> NotesViewModel(
                notes = container.noteRepository,
                reminders = container.reminderRepository,
                preferences = container.preferences,
                study = container.studyRepository,
                goals = container.goalRepository,
            )
            TodayViewModel::class.java -> TodayViewModel(
                observeDay = observeDay,
                notes = container.noteRepository,
                habits = container.habitRepository,
                study = container.studyRepository,
            )
            HomeViewModel::class.java -> HomeViewModel(
                observeDay = observeDay,
                notes = container.noteRepository,
                goals = container.goalRepository,
                preferences = container.preferences,
            )
            CalendarViewModel::class.java -> CalendarViewModel(
                observeDay = observeDay,
                notes = container.noteRepository,
                events = container.eventRepository,
                reminders = container.reminderRepository,
            )
            GoalsViewModel::class.java -> GoalsViewModel(container.goalRepository)
            HabitsViewModel::class.java -> HabitsViewModel(
                habits = container.habitRepository,
                reminders = container.reminderRepository,
            )
            StudyViewModel::class.java -> StudyViewModel(
                study = container.studyRepository,
                notes = container.noteRepository,
                reminders = container.reminderRepository,
            )
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        } as T
}
