package com.rincon.espacio.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rincon.espacio.core.design.RinconMotion
import com.rincon.espacio.ui.components.CozyBackground
import com.rincon.espacio.ui.components.RinconBottomBar
import com.rincon.espacio.ui.screens.calendar.CalendarScreen
import com.rincon.espacio.ui.screens.canvas.DeskScreen
import com.rincon.espacio.ui.screens.goals.GoalsScreen
import com.rincon.espacio.ui.screens.goals.HabitsScreen
import com.rincon.espacio.ui.screens.home.HomeScreen
import com.rincon.espacio.ui.screens.settings.MeScreen
import com.rincon.espacio.ui.screens.study.StudyScreen
import com.rincon.espacio.ui.screens.today.TodayScreen
import com.rincon.espacio.ui.vm.CalendarViewModel
import com.rincon.espacio.ui.vm.GoalsViewModel
import com.rincon.espacio.ui.vm.HabitsViewModel
import com.rincon.espacio.ui.vm.HomeViewModel
import com.rincon.espacio.ui.vm.NotesViewModel
import com.rincon.espacio.ui.vm.RinconViewModelFactory
import com.rincon.espacio.ui.vm.SettingsViewModel
import com.rincon.espacio.ui.vm.StudyViewModel
import com.rincon.espacio.ui.vm.TodayViewModel

/**
 * Altura de arranque de la barra. Sólo se usa durante el primer fotograma:
 * en cuanto la barra se dibuja informa de su altura real, que depende de la
 * barra del sistema y de la escala de texto del teléfono.
 */
private val InitialBottomBarHeight = 92.dp

/**
 * Navegación.
 *
 * Las pestañas se cruzan con un fundido y una escala mínima (cambiar de sitio,
 * no viajar); las pantallas secundarias entran deslizándose desde la derecha,
 * que es lo que la gente espera de una jerarquía. Las duraciones son cortas a
 * propósito: la app debe sentirse rápida incluso cuando está animada.
 */
@Composable
fun RinconNavHost(
    factory: RinconViewModelFactory,
    settingsViewModel: SettingsViewModel,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTab = TabDestination.entries.any { it.route == currentRoute }
    val duration = if (reduceMotion) 90 else 240

    // Se comparten entre pantallas: el editor de notas vive en varias.
    val notesViewModel: NotesViewModel = viewModel(factory = factory)

    var bottomBarHeight by remember { mutableStateOf(InitialBottomBarHeight) }

    CozyBackground(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(tween(duration, easing = RinconMotion.EnterEasing)) +
                        scaleIn(tween(duration, easing = RinconMotion.EnterEasing), initialScale = 0.98f)
                },
                exitTransition = { fadeOut(tween(duration / 2)) },
                popEnterTransition = {
                    fadeIn(tween(duration, easing = RinconMotion.EnterEasing))
                },
                popExitTransition = {
                    fadeOut(tween(duration / 2)) +
                        scaleOut(tween(duration), targetScale = 0.98f)
                },
            ) {
                composable(Routes.HOME) {
                    val vm: HomeViewModel = viewModel(factory = factory)
                    HomeScreen(
                        viewModel = vm,
                        notesViewModel = notesViewModel,
                        onOpenDesk = { navController.switchTab(Routes.DESK) },
                        onOpenToday = { navController.switchTab(Routes.TODAY) },
                        onOpenGoals = { navController.navigate(Routes.GOALS) },
                        onOpenHabits = { navController.navigate(Routes.HABITS) },
                        onOpenStudy = { navController.navigate(Routes.STUDY) },
                        onOpenCalendar = { navController.switchTab(Routes.CALENDAR) },
                        bottomInset = bottomBarHeight,
                    )
                }

                composable(Routes.DESK) {
                    DeskScreen(viewModel = notesViewModel, bottomInset = bottomBarHeight)
                }

                composable(Routes.TODAY) {
                    val vm: TodayViewModel = viewModel(factory = factory)
                    TodayScreen(
                        viewModel = vm,
                        notesViewModel = notesViewModel,
                        bottomInset = bottomBarHeight,
                    )
                }

                composable(Routes.CALENDAR) {
                    val vm: CalendarViewModel = viewModel(factory = factory)
                    CalendarScreen(
                        viewModel = vm,
                        notesViewModel = notesViewModel,
                        bottomInset = bottomBarHeight,
                    )
                }

                composable(Routes.ME) {
                    MeScreen(
                        viewModel = settingsViewModel,
                        onOpenGoals = { navController.navigate(Routes.GOALS) },
                        onOpenHabits = { navController.navigate(Routes.HABITS) },
                        onOpenStudy = { navController.navigate(Routes.STUDY) },
                        bottomInset = bottomBarHeight,
                    )
                }

                composable(
                    route = Routes.GOALS,
                    enterTransition = { slideInHorizontally(tween(duration)) { it / 3 } + fadeIn(tween(duration)) },
                    popExitTransition = { slideOutHorizontally(tween(duration)) { it / 3 } + fadeOut(tween(duration)) },
                ) {
                    val vm: GoalsViewModel = viewModel(factory = factory)
                    GoalsScreen(viewModel = vm, bottomInset = 0.dp)
                }

                composable(
                    route = Routes.HABITS,
                    enterTransition = { slideInHorizontally(tween(duration)) { it / 3 } + fadeIn(tween(duration)) },
                    popExitTransition = { slideOutHorizontally(tween(duration)) { it / 3 } + fadeOut(tween(duration)) },
                ) {
                    val vm: HabitsViewModel = viewModel(factory = factory)
                    HabitsScreen(viewModel = vm, bottomInset = 0.dp)
                }

                composable(
                    route = Routes.STUDY,
                    enterTransition = { slideInHorizontally(tween(duration)) { it / 3 } + fadeIn(tween(duration)) },
                    popExitTransition = { slideOutHorizontally(tween(duration)) { it / 3 } + fadeOut(tween(duration)) },
                ) {
                    val vm: StudyViewModel = viewModel(factory = factory)
                    StudyScreen(
                        viewModel = vm,
                        notesViewModel = notesViewModel,
                        bottomInset = 0.dp,
                    )
                }
            }

            if (isTab) {
                RinconBottomBar(
                    current = currentRoute,
                    onSelect = { tab -> navController.switchTab(tab.route) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onHeightChanged = { bottomBarHeight = it },
                )
            }
        }
    }
}

/** Cambiar de pestaña no apila pantallas: vuelve a la raíz y conserva su estado. */
private fun NavHostController.switchTab(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
