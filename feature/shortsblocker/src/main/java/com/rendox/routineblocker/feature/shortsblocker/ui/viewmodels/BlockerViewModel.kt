package com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rendox.routineblocker.feature.shortsblocker.models.TrackedPackage
import com.rendox.routineblocker.feature.shortsblocker.security.PasswordUtils
import com.rendox.routineblocker.feature.shortsblocker.utils.AccessibilityServiceManager
import com.rendox.routineblocker.feature.shortsblocker.utils.PackageConstants
import com.rendox.routineblocker.feature.shortsblocker.utils.UserPreferencesProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

data class BlockerUiState(
    val isServiceEnabled: Boolean = false,
    val isCheckingService: Boolean = false,
    val trackedPackages: List<TrackedPackage> = emptyList(),
    val isOnboardingCompleted: Boolean = false,
    val showDisclosure: Boolean = false,
    val allowedDays: Set<Int> = emptySet(),
    val dailyQuotaMinutes: Int = 0,
    val usedMinutesToday: Int = 0,
    val appBlockEnabled: Boolean = false,
    val appBlockedDays: Set<Int> = emptySet(),
    val blockerEnabled: Boolean = true,
    val hasPassword: Boolean = false,
    val isUnlocked: Boolean = false,
    val passwordError: String? = null,
    val passwordChangedSuccessfully: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
)

class BlockerViewModel(
    application: Application,
    private val prefs: UserPreferencesProvider,
) : AndroidViewModel(application) {
    companion object {
        private const val SERVICE_NAME = "com.rendox.routineblocker" +
            "/com.rendox.routineblocker.feature.shortsblocker.services.ShortFormContentBlockerService"
    }

    private val _state = MutableStateFlow(BlockerUiState())
    val state: StateFlow<BlockerUiState> = _state.asStateFlow()
    private var isMonitoring = false
    private var unlockJob: Job? = null
    private val devicePolicyManager: DevicePolicyManager by lazy {
        getApplication<Application>().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val adminComponent: ComponentName by lazy {
        ComponentName(getApplication(), com.rendox.routineblocker.feature.shortsblocker.security.AdminReceiver::class.java)
    }

    init {
        Timber.d("BlockerViewModel initialized")
        observeOnboardingState()
        observeTrackedPackages()
        observeAllowedDays()
        observeDailyQuota()
        observeUsedMinutes()
        observeAppBlockEnabled()
        observeAppBlockedDays()
        observeBlockerEnabled()
        observePasswordHash()
        checkDeviceAdmin()
    }

    private fun observeOnboardingState() {
        viewModelScope.launch {
            prefs.getOnboardingCompleted().collect { completed ->
                _state.update { it.copy(isOnboardingCompleted = completed) }
            }
        }
    }

    private fun observeTrackedPackages() {
        viewModelScope.launch {
            prefs.getTrackedPackagesWithStatus().collect { packages ->
                _state.update { it.copy(trackedPackages = packages) }
            }
        }
    }

    private fun observeAllowedDays() {
        viewModelScope.launch {
            prefs.getAllowedDays().collect { days ->
                Timber.d("Allowed days updated: $days")
                _state.update { it.copy(allowedDays = days) }
            }
        }
    }

    private fun observeDailyQuota() {
        viewModelScope.launch {
            prefs.getDailyQuotaMinutes().collect { quota ->
                Timber.d("Daily quota updated: $quota")
                _state.update { it.copy(dailyQuotaMinutes = quota) }
            }
        }
    }

    private fun observeUsedMinutes() {
        viewModelScope.launch {
            prefs.getUsedMinutesToday().collect { used ->
                Timber.d("Used minutes today: $used")
                _state.update { it.copy(usedMinutesToday = used) }
            }
        }
    }

    private fun observeAppBlockEnabled() {
        viewModelScope.launch {
            prefs.getAppBlockEnabled().collect { enabled ->
                _state.update { it.copy(appBlockEnabled = enabled) }
            }
        }
    }

    private fun observeAppBlockedDays() {
        viewModelScope.launch {
            prefs.getAppBlockedDays().collect { days ->
                _state.update { it.copy(appBlockedDays = days) }
            }
        }
    }

    private fun observeBlockerEnabled() {
        viewModelScope.launch {
            prefs.getBlockerEnabled().collect { enabled ->
                _state.update { it.copy(blockerEnabled = enabled) }
            }
        }
    }

    private fun observePasswordHash() {
        viewModelScope.launch {
            prefs.getPasswordHash().collect { hash ->
                _state.update { it.copy(hasPassword = hash != null) }
            }
        }
    }

    private fun checkDeviceAdmin() {
        viewModelScope.launch {
            _state.update { it.copy(isDeviceAdminActive = devicePolicyManager.isAdminActive(adminComponent)) }
        }
    }

    fun completeOnboarding(context: Context) {
        viewModelScope.launch {
            prefs.setOnboardingCompleted(true)
            _state.update { it.copy(showDisclosure = true) }
        }
    }

    fun acceptDisclosure(context: Context) {
        viewModelScope.launch {
            prefs.setDisclosureAccepted(true)
        }
        _state.update { it.copy(showDisclosure = false) }
        openAccessibilitySettings(context)
    }

    fun dismissDisclosure() {
        _state.update { it.copy(showDisclosure = false) }
    }

    fun checkServiceStatus(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isCheckingService = true) }
            val isGranted = AccessibilityServiceManager.isAccessibilityServiceEnabled(context, SERVICE_NAME)
            _state.update { it.copy(isServiceEnabled = isGranted, isCheckingService = false) }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        AccessibilityServiceManager.openAccessibilitySettings(context)
        startMonitoring(context)
    }

    private fun startMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true
        viewModelScope.launch {
            while (isActive && isMonitoring) {
                delay(1000L)
                checkServiceStatus(context)
                if (_state.value.isServiceEnabled) {
                    isMonitoring = false
                    break
                }
            }
        }
    }

    fun togglePackage(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            prefs.togglePackage(packageName, enabled)
        }
    }

    fun updateAllowedDays(days: Set<Int>) {
        viewModelScope.launch {
            prefs.setAllowedDays(days)
        }
    }

    fun updateDailyQuotaMinutes(minutes: Int) {
        viewModelScope.launch {
            prefs.setDailyQuotaMinutes(minutes)
        }
    }

    fun toggleAppBlock() {
        viewModelScope.launch {
            prefs.setAppBlockEnabled(!_state.value.appBlockEnabled)
        }
    }

    fun toggleBlocker() {
        viewModelScope.launch {
            prefs.setBlockerEnabled(!_state.value.blockerEnabled)
        }
    }

    fun updateAppBlockedDays(days: Set<Int>) {
        viewModelScope.launch {
            prefs.setAppBlockedDays(days)
        }
    }

    fun setPassword(password: String) {
        val hash = PasswordUtils.hash(password)
        viewModelScope.launch {
            prefs.setPasswordHash(hash)
            _state.update { it.copy(hasPassword = true, passwordError = null) }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val storedHash = prefs.getPasswordHash().first()
            if (storedHash != null && PasswordUtils.verify(currentPassword, storedHash)) {
                val newHash = PasswordUtils.hash(newPassword)
                prefs.setPasswordHash(newHash)
                _state.update { it.copy(passwordError = null, passwordChangedSuccessfully = true) }
            } else {
                _state.update { it.copy(passwordError = "Senha atual incorreta") }
            }
        }
    }

    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            val storedHash = prefs.getPasswordHash().first()
            if (storedHash != null && PasswordUtils.verify(password, storedHash)) {
                _state.update { it.copy(isUnlocked = true, passwordError = null) }
                startUnlockTimer()
            } else {
                _state.update { it.copy(passwordError = "Senha incorreta") }
            }
        }
    }

    fun clearPasswordError() {
        _state.update { it.copy(passwordError = null) }
    }

    fun lockNow() {
        unlockJob?.cancel()
        unlockJob = null
        _state.update { it.copy(isUnlocked = false) }
    }

    private fun startUnlockTimer() {
        unlockJob?.cancel()
        unlockJob = viewModelScope.launch {
            delay(300_000L)
            _state.update { it.copy(isUnlocked = false) }
        }
    }

    fun activateDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Ative o administrador para proteger o app contra desinstalacao sem senha"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun refreshDeviceAdminStatus() {
        checkDeviceAdmin()
    }

    override fun onCleared() {
        super.onCleared()
        isMonitoring = false
    }
}
