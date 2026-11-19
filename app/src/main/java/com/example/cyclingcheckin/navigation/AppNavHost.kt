package com.example.cyclingcheckin.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.ui.home.HomeScreen
import com.example.cyclingcheckin.ui.settings.BackupRestoreScreen
import com.example.cyclingcheckin.ui.settings.SettingsScreen
import com.example.cyclingcheckin.ui.settings.WebDavConfigScreen
import com.example.cyclingcheckin.ui.stats.StatsScreen

@Composable
fun RideLogApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val showBottomBar = currentRoute == Routes.HOME || currentRoute == Routes.STATS || currentRoute == Routes.SETTINGS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                RideLogBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    container = container,
                    onGoCheckIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onOpenWebDav = { navController.navigate(Routes.WEBDAV) },
                    onOpenBackupRestore = { navController.navigate(Routes.BACKUP_RESTORE) }
                )
            }
            composable(Routes.WEBDAV) {
                WebDavConfigScreen(
                    container = container,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BACKUP_RESTORE) {
                BackupRestoreScreen(
                    container = container,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onOpenWebDav = { navController.navigate(Routes.WEBDAV) }
                )
            }
        }
    }
}

@Composable
private fun RideLogBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onNavigate(Routes.HOME) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bike),
                    contentDescription = stringResource(R.string.nav_check_in)
                )
            },
            label = { Text(stringResource(R.string.nav_check_in)) },
            colors = navBarItemColors()
        )
        NavigationBarItem(
            selected = currentRoute == Routes.STATS,
            onClick = { onNavigate(Routes.STATS) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stats),
                    contentDescription = stringResource(R.string.nav_stats)
                )
            },
            label = { Text(stringResource(R.string.nav_stats)) },
            colors = navBarItemColors()
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = { onNavigate(Routes.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.nav_settings)
                )
            },
            label = { Text(stringResource(R.string.nav_settings)) },
            colors = navBarItemColors()
        )
    }
}

@Composable
private fun navBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
