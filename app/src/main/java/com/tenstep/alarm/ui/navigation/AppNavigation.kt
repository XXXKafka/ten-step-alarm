package com.tenstep.alarm.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tenstep.alarm.R
import com.tenstep.alarm.ui.alarm.AlarmEditScreen
import com.tenstep.alarm.ui.alarm.AlarmListScreen
import com.tenstep.alarm.ui.clock.FlipClockScreen
import com.tenstep.alarm.ui.pomodoro.PomodoroScreen
import com.tenstep.alarm.ui.settings.SettingsScreen
import com.tenstep.alarm.timer.TimerScreen

object Routes {
    const val HOME = "home"
    const val POMODORO = "pomodoro"
    const val TIMER = "timer"
    const val CLOCK = "clock"
    const val SETTINGS = "settings"
    const val ALARM_EDIT = "alarm_edit"
    const val ALARM_EDIT_ARG = "alarm_edit/{alarmId}"

    fun alarmEdit(alarmId: Long): String = "$ALARM_EDIT/$alarmId"
}

private data class BottomItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // These destinations own their whole screen (fullscreen clock, editor).
    val hideBottomBar = currentRoute == Routes.ALARM_EDIT_ARG || currentRoute == Routes.CLOCK

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideBottomBar) {
                AppBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        // The fullscreen clock owns the whole screen: it must draw behind
        // the status/navigation bars so its background (including the status
        // bar area) matches the flip-clock color instead of the app theme.
        val contentModifier = if (currentRoute == Routes.CLOCK) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        }

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = contentModifier
        ) {
            composable(Routes.HOME) {
                AlarmListScreen(
                    onAdd = { navController.navigate(Routes.alarmEdit(0L)) },
                    onEdit = { navController.navigate(Routes.alarmEdit(it)) }
                )
            }
            composable(
                route = Routes.ALARM_EDIT_ARG,
                arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
            ) {
                AlarmEditScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.POMODORO) { PomodoroScreen() }
            composable(Routes.TIMER) { TimerScreen() }
            composable(Routes.CLOCK) {
                FlipClockScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController, currentRoute: String?) {
    val items = listOf(
        BottomItem(Routes.HOME, R.string.tab_alarm, Icons.Filled.Alarm),
        BottomItem(Routes.POMODORO, R.string.tab_pomodoro, Icons.Filled.Timer),
        BottomItem(Routes.TIMER, R.string.tab_timer, Icons.Filled.HourglassEmpty),
        BottomItem(Routes.CLOCK, R.string.tab_clock, Icons.Filled.Schedule),
        BottomItem(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings)
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.labelRes)) }
            )
        }
    }
}