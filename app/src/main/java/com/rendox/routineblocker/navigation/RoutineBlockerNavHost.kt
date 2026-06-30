package com.rendox.routineblocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rendox.routineblocker.feature.shortsblocker.navigation.shortsBlockerNavigationRoute
import com.rendox.routineblocker.ui.DashboardScreen
import com.rendox.routinetracker.addeditroutine.navigation.ADD_ROUTINE_ROUTE
import com.rendox.routinetracker.addeditroutine.navigation.addRoutineScreen
import com.rendox.routinetracker.addeditroutine.navigation.navigateToAddRoutine
import com.rendox.routinetracker.feature.agenda.navigation.AGENDA_NAV_ROUTE
import com.rendox.routinetracker.feature.agenda.navigation.agendaScreen
import com.rendox.routinetracker.feature.agenda.navigation.navigateToAgenda
import com.rendox.routinetracker.routinedetails.navigation.ROUTINE_DETAILS_ROUTE
import com.rendox.routinetracker.routinedetails.navigation.navigateToRoutineDetails
import com.rendox.routinetracker.routinedetails.navigation.routineDetailsScreen

@Composable
fun RoutineBlockerNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = modifier,
    ) {
        composable("dashboard") {
            DashboardScreen(
                onOpenRoutineTracker = { navController.navigate("routinetracker") },
                onOpenShortsBlocker = { navController.navigate("shortsblocker") },
            )
        }
        composable("routinetracker") {
            RoutineTrackerNavHost(
                startDestination = AGENDA_NAV_ROUTE,
            )
        }
        composable("shortsblocker") {
            shortsBlockerNavigationRoute()
        }
    }
}

@Composable
private fun RoutineTrackerNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        agendaScreen(
            onRoutineClick = { routineId ->
                navController.navigateToRoutineDetails(routineId)
            },
            onAddRoutineClick = {
                navController.navigateToAddRoutine()
            },
        )
        routineDetailsScreen(
            popBackStack = { navController.popBackStack() },
        )
        addRoutineScreen(
            navigateBackAndRecreate = {
                navController.popBackStack(route = AGENDA_NAV_ROUTE, inclusive = false)
            },
            navigateBack = { navController.popBackStack() },
        )
    }
}
