package com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.BlockAction
import com.rendox.routineblocker.feature.shortsblocker.models.BlockReason
import com.rendox.routineblocker.feature.shortsblocker.models.BlockerSettings
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.TimeWindow
import com.rendox.routineblocker.feature.shortsblocker.security.AdminReceiver
import com.rendox.routineblocker.feature.shortsblocker.security.PasswordUtils
import com.rendox.routineblocker.feature.shortsblocker.utils.AccessibilityServiceManager
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
import java.time.LocalDate
import java.time.LocalTime

data class BlockerUiState(
    val isServiceEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val showDisclosure: Boolean = false,
    val settings: BlockerSettings = BlockerSettings(),
    val schedules: List<AppSchedule> = emptyList(),
    val usageToday: Map<String, Int> = emptyMap(),
    val hasPassword: Boolean = false,
    val isUnlocked: Boolean = false,
    val passwordError: String? = null,
    val passwordChangedSuccessfully: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val emergencyUnlockUsedToday: Boolean = false,
    val today: Int = LocalDate.now().dayOfWeek.value,
    val minuteOfDay: Int = 0,
    val nowEpochMillis: Long = 0L,
) {
    /** As configuracoes estao travadas por senha? */
    val isLocked: Boolean
        get() = hasPassword && !isUnlocked

    val isPaused: Boolean
        get() = settings.isPaused(nowEpochMillis)

    val pauseRemainingMinutes: Int
        get() = settings.pauseRemainingMinutes(nowEpochMillis)

    /** A protecao esta valendo neste instante (ligada e sem pausa ativa)? */
    val isProtectionActiveNow: Boolean
        get() = settings.isActive(nowEpochMillis)

    val monitoredSchedules: List<AppSchedule>
        get() = schedules.filter { it.monitored }

    fun scheduleFor(packageName: String): AppSchedule =
        schedules.firstOrNull { it.packageName == packageName }
            ?: AppSchedule(packageName = packageName)

    fun usedMinutes(packageName: String): Int = usageToday[packageName] ?: 0

    /** Algum app monitorado esta com a abertura bloqueada neste momento. */
    val hasActiveAppBlockNow: Boolean
        get() = monitoredSchedules.any {
            it.appAccessReason(today, minuteOfDay) != BlockReason.NENHUM
        }

    /**
     * O modo rigido impede afrouxar a protecao enquanto um bloqueio de abertura
     * estiver valendo. Como esses bloqueios sempre tem hora para acabar, o usuario
     * nunca fica preso.
     */
    val canRelaxProtection: Boolean
        get() = !settings.strictMode || !hasActiveAppBlockNow

    /** Pode editar configuracoes agora? */
    val canEdit: Boolean
        get() = !isLocked

    /** A liberacao de emergencia esta disponivel agora? (1x por dia, sem senha) */
    val canUseEmergencyUnlock: Boolean
        get() = !emergencyUnlockUsedToday && settings.protectionEnabled && !isPaused
}

class BlockerViewModel(
    application: Application,
    private val preferences: UserPreferencesProvider,
) : AndroidViewModel(application) {

    private companion object {
        const val SERVICE_NAME = "com.rendox.routineblocker" +
            "/com.rendox.routineblocker.feature.shortsblocker.services.ShortFormContentBlockerService"
        const val CLOCK_TICK_MILLIS = 15_000L
        const val SERVICE_POLL_MILLIS = 1_000L
        const val MIN_PASSWORD_LENGTH = 4
    }

    private val _state = MutableStateFlow(BlockerUiState())
    val state: StateFlow<BlockerUiState> = _state.asStateFlow()

    private var unlockJob: Job? = null
    private var serviceWatchJob: Job? = null

    private val devicePolicyManager: DevicePolicyManager by lazy {
        getApplication<Application>()
            .getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val adminComponent: ComponentName by lazy {
        ComponentName(getApplication(), AdminReceiver::class.java)
    }

    init {
        viewModelScope.launch { preferences.migrateLegacySettingsIfNeeded() }
        observePreferences()
        startClock()
        refreshDeviceAdminStatus()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.onboardingCompleted.collect { completed ->
                _state.update { it.copy(isOnboardingCompleted = completed) }
            }
        }
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            preferences.schedules.collect { schedules ->
                _state.update { it.copy(schedules = schedules) }
            }
        }
        viewModelScope.launch {
            preferences.usageToday.collect { usage ->
                _state.update { it.copy(usageToday = usage) }
            }
        }
        viewModelScope.launch {
            preferences.passwordHash.collect { hash ->
                _state.update { current ->
                    current.copy(
                        hasPassword = hash != null,
                        isUnlocked = if (hash == null) false else current.isUnlocked,
                    )
                }
            }
        }
        viewModelScope.launch {
            preferences.emergencyUnlockUsedToday.collect { used ->
                _state.update { it.copy(emergencyUnlockUsedToday = used) }
            }
        }
    }

    /** Mantem o estado ciente da hora atual para os cartoes de status ao vivo. */
    private fun startClock() {
        viewModelScope.launch {
            while (isActive) {
                val now = LocalTime.now()
                _state.update {
                    it.copy(
                        today = LocalDate.now().dayOfWeek.value,
                        minuteOfDay = now.hour * 60 + now.minute,
                        nowEpochMillis = System.currentTimeMillis(),
                    )
                }
                delay(CLOCK_TICK_MILLIS)
            }
        }
    }

    // ---------------------------------------------------------------- onboarding

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            _state.update { it.copy(showDisclosure = true) }
        }
    }

    fun acceptDisclosure(context: Context) {
        viewModelScope.launch { preferences.setDisclosureAccepted(true) }
        _state.update { it.copy(showDisclosure = false) }
        openAccessibilitySettings(context)
    }

    fun dismissDisclosure() {
        _state.update { it.copy(showDisclosure = false) }
    }

    // ---------------------------------------------------------------- servico

    fun checkServiceStatus(context: Context) {
        val isGranted = AccessibilityServiceManager.isAccessibilityServiceEnabled(
            context = context,
            serviceName = SERVICE_NAME,
        )
        _state.update { it.copy(isServiceEnabled = isGranted) }
    }

    fun openAccessibilitySettings(context: Context) {
        AccessibilityServiceManager.openAccessibilitySettings(context)
        watchForServiceActivation(context)
    }

    private fun watchForServiceActivation(context: Context) {
        serviceWatchJob?.cancel()
        serviceWatchJob = viewModelScope.launch {
            repeat(120) {
                delay(SERVICE_POLL_MILLIS)
                checkServiceStatus(context)
                if (_state.value.isServiceEnabled) return@launch
            }
        }
    }

    // ---------------------------------------------------------------- protecao

    fun setProtectionEnabled(enabled: Boolean) {
        if (!enabled) {
            if (_state.value.isLocked) {
                Timber.d("Desligar a protecao exige a senha")
                return
            }
            if (!_state.value.canRelaxProtection) {
                Timber.d("Modo rigido impede desligar a protecao agora")
                return
            }
        }
        viewModelScope.launch { preferences.setProtectionEnabled(enabled) }
    }

    /** Desliga a protecao depois de confirmar a senha salva. */
    fun disableProtectionWithPassword(password: String) {
        viewModelScope.launch {
            if (!verify(password)) {
                _state.update { it.copy(passwordError = "Senha incorreta") }
                return@launch
            }
            if (!_state.value.canRelaxProtection) {
                _state.update {
                    it.copy(passwordError = "Modo rígido ativo: não dá para desligar agora.")
                }
                return@launch
            }
            preferences.setProtectionEnabled(false)
            _state.update { it.copy(passwordError = null) }
        }
    }

    /** Liberacao de emergencia: flexibiliza as regras por 5 min, sem senha, 1x por dia. */
    fun useEmergencyUnlock() {
        if (!_state.value.canUseEmergencyUnlock) return
        viewModelScope.launch {
            preferences.useEmergencyUnlock(BlockerSettings.EMERGENCY_UNLOCK_MINUTES)
        }
    }

    fun cancelPause() {
        viewModelScope.launch { preferences.cancelPause() }
    }

    fun setBlockAction(action: BlockAction) {
        viewModelScope.launch { preferences.setBlockAction(action) }
    }

    fun setShowBlockWarning(show: Boolean) {
        viewModelScope.launch { preferences.setShowBlockWarning(show) }
    }

    fun setBlockMessage(message: String) {
        viewModelScope.launch { preferences.setBlockMessage(message) }
    }

    fun setStrictMode(enabled: Boolean) {
        if (!enabled && !_state.value.canRelaxProtection) return
        viewModelScope.launch { preferences.setStrictMode(enabled) }
    }

    fun setUnlockDurationMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setUnlockDurationMinutes(minutes) }
    }

    fun resetTodayUsage() {
        viewModelScope.launch { preferences.clearUsageToday() }
    }

    // ---------------------------------------------------------------- agendas

    fun setMonitored(packageName: String, monitored: Boolean) {
        if (!monitored && !_state.value.canRelaxProtection) return
        updateSchedule(packageName) { it.copy(monitored = monitored) }
    }

    fun updateDay(packageName: String, dayOfWeek: Int, day: DaySchedule) {
        updateSchedule(packageName) { it.withDay(dayOfWeek, day) }
    }

    fun addWindow(packageName: String, dayOfWeek: Int, window: TimeWindow) {
        if (!window.isValid) return
        updateSchedule(packageName) { schedule ->
            schedule.withDay(dayOfWeek, schedule.day(dayOfWeek).withWindow(window))
        }
    }

    fun removeWindow(packageName: String, dayOfWeek: Int, window: TimeWindow) {
        updateSchedule(packageName) { schedule ->
            schedule.withDay(dayOfWeek, schedule.day(dayOfWeek).withoutWindow(window))
        }
    }

    fun copyDayTo(packageName: String, from: Int, targets: List<Int>) {
        updateSchedule(packageName) { it.copyDay(from, targets) }
    }

    fun resetSchedule(packageName: String) {
        updateSchedule(packageName) { it.copy(days = emptyMap()) }
    }

    private fun updateSchedule(packageName: String, transform: (AppSchedule) -> AppSchedule) {
        if (!_state.value.canEdit) return
        viewModelScope.launch {
            val current = preferences.schedule(packageName).first()
            preferences.saveSchedule(transform(current))
        }
    }

    // ---------------------------------------------------------------- senha

    fun setPassword(password: String) {
        if (password.length < MIN_PASSWORD_LENGTH) return
        viewModelScope.launch {
            preferences.setPasswordHash(PasswordUtils.hash(password))
            _state.update { it.copy(hasPassword = true, isUnlocked = true, passwordError = null) }
            startUnlockTimer()
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (newPassword.length < MIN_PASSWORD_LENGTH) return
        viewModelScope.launch {
            if (!verify(currentPassword)) {
                _state.update { it.copy(passwordError = "Senha atual incorreta") }
                return@launch
            }
            preferences.setPasswordHash(PasswordUtils.hash(newPassword))
            _state.update { it.copy(passwordError = null, passwordChangedSuccessfully = true) }
        }
    }

    fun removePassword(currentPassword: String) {
        viewModelScope.launch {
            if (!verify(currentPassword)) {
                _state.update { it.copy(passwordError = "Senha incorreta") }
                return@launch
            }
            preferences.clearPasswordHash()
            unlockJob?.cancel()
            _state.update {
                it.copy(hasPassword = false, isUnlocked = false, passwordError = null)
            }
        }
    }

    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            if (!verify(password)) {
                _state.update { it.copy(passwordError = "Senha incorreta") }
                return@launch
            }
            _state.update { it.copy(isUnlocked = true, passwordError = null) }
            startUnlockTimer()
        }
    }

    fun lockNow() {
        unlockJob?.cancel()
        unlockJob = null
        _state.update { it.copy(isUnlocked = false) }
    }

    fun clearPasswordError() {
        _state.update { it.copy(passwordError = null) }
    }

    fun consumePasswordChanged() {
        _state.update { it.copy(passwordChangedSuccessfully = false) }
    }

    private suspend fun verify(password: String): Boolean {
        val storedHash = preferences.passwordHash.first() ?: return false
        return PasswordUtils.verify(password, storedHash)
    }

    private fun startUnlockTimer() {
        unlockJob?.cancel()
        unlockJob = viewModelScope.launch {
            val minutes = _state.value.settings.unlockDurationMinutes
            delay(minutes * 60_000L)
            _state.update { it.copy(isUnlocked = false) }
        }
    }

    // ---------------------------------------------------------------- device admin

    fun activateDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Ative o administrador para dificultar a desinstalação do app por impulso.",
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun deactivateDeviceAdmin() {
        if (!_state.value.canEdit) return
        runCatching { devicePolicyManager.removeActiveAdmin(adminComponent) }
            .onFailure { Timber.e(it, "Falha ao remover o administrador do dispositivo") }
        refreshDeviceAdminStatus()
    }

    fun refreshDeviceAdminStatus() {
        val isActive = runCatching { devicePolicyManager.isAdminActive(adminComponent) }
            .getOrDefault(false)
        _state.update { it.copy(isDeviceAdminActive = isActive) }
    }
}
