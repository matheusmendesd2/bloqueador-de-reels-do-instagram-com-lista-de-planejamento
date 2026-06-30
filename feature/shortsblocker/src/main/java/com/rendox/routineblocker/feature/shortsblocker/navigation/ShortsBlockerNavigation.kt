package com.rendox.routineblocker.feature.shortsblocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.rendox.routineblocker.feature.shortsblocker.ui.screens.BlockerMainScreen
import com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun shortsBlockerNavigationRoute() {
    val viewModel: BlockerViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    BlockerMainScreen(
        state = state,
        isOnboarding = !state.isOnboardingCompleted,
        showDisclosure = state.showDisclosure,
        onCompleteOnboarding = { viewModel.completeOnboarding(context) },
        onAcceptDisclosure = { viewModel.acceptDisclosure(context) },
        onDismissDisclosure = { viewModel.dismissDisclosure() },
        onCheckService = { viewModel.checkServiceStatus(context) },
        onOpenSettings = { viewModel.openAccessibilitySettings(context) },
        onTogglePackage = { pkg, enabled -> viewModel.togglePackage(pkg, enabled) },
        onUpdateAllowedDays = { viewModel.updateAllowedDays(it) },
        onUpdateDailyQuota = { viewModel.updateDailyQuotaMinutes(it) },
        onToggleAppBlock = { viewModel.toggleAppBlock() },
        onUpdateAppBlockedDays = { viewModel.updateAppBlockedDays(it) },
        onSetPassword = { viewModel.setPassword(it) },
        onChangePassword = { current, new -> viewModel.changePassword(current, new) },
        onUnlock = { viewModel.unlockWithPassword(it) },
        onClearPasswordError = { viewModel.clearPasswordError() },
        onToggleBlocker = { viewModel.toggleBlocker() },
        onLock = { viewModel.lockNow() },
        onActivateDeviceAdmin = { viewModel.activateDeviceAdmin(context) },
        onRefreshAdminStatus = { viewModel.refreshDeviceAdminStatus() },
    )
}
