package com.rendox.routineblocker.feature.shortsblocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rendox.routineblocker.feature.shortsblocker.ui.BlockerActions
import com.rendox.routineblocker.feature.shortsblocker.ui.components.ChangePasswordDialog
import com.rendox.routineblocker.feature.shortsblocker.ui.components.RemovePasswordDialog
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SetPasswordDialog
import com.rendox.routineblocker.feature.shortsblocker.ui.components.UnlockDialog
import com.rendox.routineblocker.feature.shortsblocker.ui.screens.AppScheduleScreen
import com.rendox.routineblocker.feature.shortsblocker.ui.screens.BlockerHomeScreen
import com.rendox.routineblocker.feature.shortsblocker.ui.screens.BlockerSettingsScreen
import com.rendox.routineblocker.feature.shortsblocker.ui.screens.OnboardingScreen
import com.rendox.routineblocker.feature.shortsblocker.ui.screens.ProminentDisclosureScreen
import com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerViewModel
import com.rendox.routineblocker.feature.shortsblocker.utils.PackageConstants
import org.koin.androidx.compose.koinViewModel

private const val HOME_ROUTE = "blocker/home"
private const val SETTINGS_ROUTE = "blocker/settings"
private const val SCHEDULE_ROUTE = "blocker/schedule"
private const val PACKAGE_ARG = "packageName"

private enum class PasswordDialog { NONE, SET, UNLOCK, CHANGE, REMOVE }

/** Ponto de entrada da feature de bloqueio. */
@Composable
fun ShortsBlockerFeature(modifier: Modifier = Modifier) {
    val viewModel: BlockerViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val navController = rememberNavController()

    var passwordDialog by remember { mutableStateOf(PasswordDialog.NONE) }

    // O usuario pode ativar o servico nas configuracoes do sistema e voltar para o app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkServiceStatus(context)
                viewModel.refreshDeviceAdminStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val actions = remember(viewModel, context) {
        BlockerActions(
            setProtectionEnabled = viewModel::setProtectionEnabled,
            pauseFor = viewModel::pauseFor,
            cancelPause = viewModel::cancelPause,
            setBlockAction = viewModel::setBlockAction,
            setShowBlockWarning = viewModel::setShowBlockWarning,
            setBlockMessage = viewModel::setBlockMessage,
            setStrictMode = viewModel::setStrictMode,
            setUnlockDurationMinutes = viewModel::setUnlockDurationMinutes,
            resetTodayUsage = viewModel::resetTodayUsage,
            setMonitored = viewModel::setMonitored,
            updateDay = viewModel::updateDay,
            addWindow = viewModel::addWindow,
            removeWindow = viewModel::removeWindow,
            copyDayTo = viewModel::copyDayTo,
            resetSchedule = viewModel::resetSchedule,
            setPassword = viewModel::setPassword,
            changePassword = viewModel::changePassword,
            removePassword = viewModel::removePassword,
            unlock = viewModel::unlockWithPassword,
            lock = viewModel::lockNow,
            clearPasswordError = viewModel::clearPasswordError,
            consumePasswordChanged = viewModel::consumePasswordChanged,
            openAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
            activateDeviceAdmin = { viewModel.activateDeviceAdmin(context) },
            deactivateDeviceAdmin = viewModel::deactivateDeviceAdmin,
        )
    }

    if (state.showDisclosure) {
        ProminentDisclosureScreen(
            onAgree = { viewModel.acceptDisclosure(context) },
            onCancel = viewModel::dismissDisclosure,
        )
        return
    }

    if (!state.isOnboardingCompleted) {
        OnboardingScreen(onComplete = viewModel::completeOnboarding)
        return
    }

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier,
    ) {
        composable(HOME_ROUTE) {
            BlockerHomeScreen(
                state = state,
                actions = actions,
                onOpenAppSchedule = { packageName ->
                    navController.navigateToSchedule(packageName)
                },
                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                onUnlockRequest = { passwordDialog = PasswordDialog.UNLOCK },
            )
        }
        composable(
            route = "$SCHEDULE_ROUTE/{$PACKAGE_ARG}",
            arguments = listOf(navArgument(PACKAGE_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString(PACKAGE_ARG)
                ?: PackageConstants.INSTAGRAM_PACKAGE
            AppScheduleScreen(
                state = state,
                packageName = packageName,
                actions = actions,
                onBack = { navController.popBackStack() },
                onUnlockRequest = { passwordDialog = PasswordDialog.UNLOCK },
            )
        }
        composable(SETTINGS_ROUTE) {
            BlockerSettingsScreen(
                state = state,
                actions = actions,
                onBack = { navController.popBackStack() },
                onSetPasswordRequest = { passwordDialog = PasswordDialog.SET },
                onChangePasswordRequest = { passwordDialog = PasswordDialog.CHANGE },
                onRemovePasswordRequest = { passwordDialog = PasswordDialog.REMOVE },
                onUnlockRequest = { passwordDialog = PasswordDialog.UNLOCK },
            )
        }
    }

    when (passwordDialog) {
        PasswordDialog.NONE -> Unit
        PasswordDialog.SET -> SetPasswordDialog(
            onConfirm = { password ->
                actions.setPassword(password)
                passwordDialog = PasswordDialog.NONE
            },
            onDismiss = { passwordDialog = PasswordDialog.NONE },
        )
        PasswordDialog.UNLOCK -> UnlockDialog(
            error = state.passwordError,
            unlockDurationMinutes = state.settings.unlockDurationMinutes,
            onConfirm = actions.unlock,
            onDismiss = {
                passwordDialog = PasswordDialog.NONE
                actions.clearPasswordError()
            },
        )
        PasswordDialog.CHANGE -> ChangePasswordDialog(
            error = state.passwordError,
            onConfirm = actions.changePassword,
            onDismiss = {
                passwordDialog = PasswordDialog.NONE
                actions.clearPasswordError()
            },
        )
        PasswordDialog.REMOVE -> RemovePasswordDialog(
            error = state.passwordError,
            onConfirm = actions.removePassword,
            onDismiss = {
                passwordDialog = PasswordDialog.NONE
                actions.clearPasswordError()
            },
        )
    }

    // Fecha o dialogo assim que a acao correspondente da certo.
    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked && passwordDialog == PasswordDialog.UNLOCK) {
            passwordDialog = PasswordDialog.NONE
        }
    }
    LaunchedEffect(state.passwordChangedSuccessfully) {
        if (state.passwordChangedSuccessfully) {
            passwordDialog = PasswordDialog.NONE
            actions.consumePasswordChanged()
        }
    }
    LaunchedEffect(state.hasPassword) {
        if (!state.hasPassword && passwordDialog == PasswordDialog.REMOVE) {
            passwordDialog = PasswordDialog.NONE
        }
    }
}

private fun NavHostController.navigateToSchedule(packageName: String) {
    navigate("$SCHEDULE_ROUTE/$packageName")
}
