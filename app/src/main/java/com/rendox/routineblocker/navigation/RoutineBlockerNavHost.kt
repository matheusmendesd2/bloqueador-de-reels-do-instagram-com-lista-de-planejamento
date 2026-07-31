package com.rendox.routineblocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rendox.routineblocker.ui.HomeScaffold
import com.rendox.routinetracker.addeditroutine.navigation.addRoutineScreen
import com.rendox.routinetracker.addeditroutine.navigation.navigateToAddRoutine
import com.rendox.routinetracker.feature.agenda.navigation.AGENDA_NAV_ROUTE
import com.rendox.routinetracker.feature.agenda.navigation.agendaScreen
import com.rendox.routinetracker.routinedetails.navigation.navigateToRoutineDetails
import com.rendox.routinetracker.routinedetails.navigation.routineDetailsScreen

@Composable
fun RoutineBlockerNavHost(modifier: Modifier = Modifier) {
    HomeScaffold(
        modifier = modifier,
        routineTrackerContent = { contentModifier ->
            RoutineTrackerNavHost(modifier = contentModifier)
        },
    )
}

@Composable
private fun RoutineTrackerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AGENDA_NAV_ROUTE,
        modifier = modifier,
    ) {
        agendaScreen(
            onRoutineClick = { routineId -> navController.navigateToRoutineDetails(routineId) },
            onAddRoutineClick = { navController.navigateToAddRoutine() },
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
