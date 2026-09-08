package com.rincon.espacio.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.rincon.espacio.ui.icons.RinconIcons

/** Rutas de la app. Cinco pestañas; todo lo demás está a un toque de ellas. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DESK = "desk"
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val ME = "me"
    const val GOALS = "goals"
    const val HABITS = "habits"
    const val STUDY = "study"
    const val SETTINGS = "settings"
}

enum class TabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    // Etiquetas cortas a propósito: con la escala de texto grande del
    // sistema, "Escritorio" o "Calendario" no caben en una quinta parte de la
    // pantalla y se parten. El nombre largo vive en el título de cada pantalla.
    Home(Routes.HOME, "Inicio", RinconIcons.Home),
    Desk(Routes.DESK, "Notas", RinconIcons.Note),
    Today(Routes.TODAY, "Hoy", RinconIcons.Sun),
    Calendar(Routes.CALENDAR, "Agenda", RinconIcons.Calendar),
    Me(Routes.ME, "Yo", RinconIcons.Person),
}
