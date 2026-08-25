package com.rendox.routineblocker.feature.shortsblocker.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rendox.routineblocker.feature.shortsblocker.models.AppAccess
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.BlockAction
import com.rendox.routineblocker.feature.shortsblocker.models.BlockerSettings
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shorts_blocker_settings",
)

/**
 * Unica fonte de verdade das configuracoes do bloqueador (DataStore Preferences).
 *
 * A agenda semanal de cada app e guardada como uma string codificada por [ScheduleCodec],
 * uma chave por pacote.
 */
class UserPreferencesProvider(context: Context) {

    private val dataStore = context.dataStore

    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val disclosureAcceptedKey = booleanPreferencesKey("disclosure_accepted")
    private val passwordHashKey = stringPreferencesKey("password_hash")

    private val protectionEnabledKey = booleanPreferencesKey("protection_enabled")
    private val blockActionKey = stringPreferencesKey("block_action")
    private val showBlockWarningKey = booleanPreferencesKey("show_block_warning")
    private val blockMessageKey = stringPreferencesKey("block_message")
    private val strictModeKey = booleanPreferencesKey("strict_mode")
    private val unlockDurationKey = intPreferencesKey("unlock_duration_minutes")
    private val pausedUntilKey = longPreferencesKey("paused_until_millis")
    private val emergencyUnlockDateKey = stringPreferencesKey("emergency_unlock_date")

    private val usageDateKey = stringPreferencesKey("usage_date")
    private val migratedKey = booleanPreferencesKey("migrated_to_schedules")

    private fun scheduleKey(packageName: String) =
        stringPreferencesKey("schedule_${packageName.replace('.', '_')}")

    private fun usageKey(packageName: String) =
        intPreferencesKey("used_minutes_${packageName.replace('.', '_')}")

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            Timber.e(error, "Falha ao ler as preferencias do bloqueador")
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    // ---------------------------------------------------------------- onboarding

    val onboardingCompleted: Flow<Boolean> =
        preferences.map { it[onboardingCompletedKey] ?: false }.distinctUntilChanged()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun setDisclosureAccepted(accepted: Boolean) {
        dataStore.edit { it[disclosureAcceptedKey] = accepted }
    }

    // ---------------------------------------------------------------- agendas

    val schedules: Flow<List<AppSchedule>> = preferences.map { prefs ->
        PackageConstants.ALL_PACKAGE_NAMES.map { packageName ->
            ScheduleCodec.decode(packageName, prefs[scheduleKey(packageName)])
        }
    }.distinctUntilChanged()

    fun schedule(packageName: String): Flow<AppSchedule> = preferences.map { prefs ->
        ScheduleCodec.decode(packageName, prefs[scheduleKey(packageName)])
    }.distinctUntilChanged()

    suspend fun saveSchedule(schedule: AppSchedule) {
        dataStore.edit { it[scheduleKey(schedule.packageName)] = ScheduleCodec.encode(schedule) }
    }

    /** Pacotes que o servico de acessibilidade precisa observar. */
    val monitoredPackages: Flow<List<String>> = schedules.map { list ->
        list.filter { it.monitored }.map { it.packageName }
    }.distinctUntilChanged()

    // ---------------------------------------------------------------- configuracoes

    val settings: Flow<BlockerSettings> = preferences.map { prefs ->
        BlockerSettings(
            protectionEnabled = prefs[protectionEnabledKey] ?: true,
            blockAction = runCatching {
                BlockAction.valueOf(prefs[blockActionKey] ?: BlockAction.VOLTAR.name)
            }.getOrDefault(BlockAction.VOLTAR),
            showBlockWarning = prefs[showBlockWarningKey] ?: true,
            blockMessage = prefs[blockMessageKey]?.takeIf { it.isNotBlank() }
                ?: BlockerSettings.DEFAULT_BLOCK_MESSAGE,
            strictMode = prefs[strictModeKey] ?: false,
            unlockDurationMinutes = prefs[unlockDurationKey] ?: 5,
            pausedUntilEpochMillis = prefs[pausedUntilKey] ?: 0L,
        )
    }.distinctUntilChanged()

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[protectionEnabledKey] = enabled
            if (enabled) prefs[pausedUntilKey] = 0L
        }
    }

    suspend fun setBlockAction(action: BlockAction) {
        dataStore.edit { it[blockActionKey] = action.name }
    }

    suspend fun setShowBlockWarning(show: Boolean) {
        dataStore.edit { it[showBlockWarningKey] = show }
    }

    suspend fun setBlockMessage(message: String) {
        dataStore.edit {
            it[blockMessageKey] = message.trim().ifBlank { BlockerSettings.DEFAULT_BLOCK_MESSAGE }
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        dataStore.edit { it[strictModeKey] = enabled }
    }

    suspend fun setUnlockDurationMinutes(minutes: Int) {
        dataStore.edit { it[unlockDurationKey] = minutes.coerceIn(1, 60) }
    }

    suspend fun cancelPause() {
        dataStore.edit { it[pausedUntilKey] = 0L }
    }

    /**
     * Liberacao de emergencia: pausa as regras por [durationMinutes] sem pedir senha
     * e registra o dia em que foi usada. So pode ser usada uma vez por dia.
     */
    suspend fun useEmergencyUnlock(durationMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[pausedUntilKey] = System.currentTimeMillis() + durationMinutes * 60_000L
            prefs[emergencyUnlockDateKey] = LocalDate.now().toString()
        }
    }

    /** A liberacao de emergencia ja foi usada hoje? */
    val emergencyUnlockUsedToday: Flow<Boolean> = preferences.map { prefs ->
        prefs[emergencyUnlockDateKey] == LocalDate.now().toString()
    }.distinctUntilChanged()

    // ---------------------------------------------------------------- consumo do dia

    /** Minutos de conteudo curto consumidos hoje, por pacote. */
    val usageToday: Flow<Map<String, Int>> = preferences.map { prefs ->
        val today = LocalDate.now().toString()
        if (prefs[usageDateKey] != today) {
            PackageConstants.ALL_PACKAGE_NAMES.associateWith { 0 }
        } else {
            PackageConstants.ALL_PACKAGE_NAMES.associateWith { prefs[usageKey(it)] ?: 0 }
        }
    }.distinctUntilChanged()

    suspend fun incrementUsedMinutes(packageName: String, minutes: Int) {
        dataStore.edit { prefs ->
            resetUsageIfNewDay(prefs)
            val current = prefs[usageKey(packageName)] ?: 0
            prefs[usageKey(packageName)] = current + minutes
        }
    }

    suspend fun resetUsageIfNewDay() {
        dataStore.edit { resetUsageIfNewDay(it) }
    }

    /** Zera o consumo de hoje - usado no botao "reiniciar cota de hoje". */
    suspend fun clearUsageToday() {
        dataStore.edit { prefs ->
            prefs[usageDateKey] = LocalDate.now().toString()
            PackageConstants.ALL_PACKAGE_NAMES.forEach { prefs[usageKey(it)] = 0 }
        }
    }

    private fun resetUsageIfNewDay(prefs: MutablePreferences) {
        val today = LocalDate.now().toString()
        if (prefs[usageDateKey] != today) {
            prefs[usageDateKey] = today
            PackageConstants.ALL_PACKAGE_NAMES.forEach { prefs[usageKey(it)] = 0 }
        }
    }

    // ---------------------------------------------------------------- senha

    val passwordHash: Flow<String?> =
        preferences.map { it[passwordHashKey] }.distinctUntilChanged()

    suspend fun setPasswordHash(hash: String) {
        dataStore.edit { it[passwordHashKey] = hash }
    }

    suspend fun clearPasswordHash() {
        dataStore.edit { it.remove(passwordHashKey) }
    }

    // ---------------------------------------------------------------- migracao

    /**
     * Converte as configuracoes da versao antiga (dias liberados + cota unica +
     * dias de bloqueio total) para a agenda semanal por app. Roda uma unica vez.
     */
    suspend fun migrateLegacySettingsIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[migratedKey] == true) return@edit

            val legacyTracked = prefs[stringPreferencesKey("tracked_packages")]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val legacyAllowedDays = prefs[stringPreferencesKey("allowed_days")]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
            val legacyBlockedDays = prefs[stringPreferencesKey("app_blocked_days")]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
            val legacyQuota = prefs[intPreferencesKey("daily_quota_minutes")] ?: 0
            val legacyAppBlockEnabled = prefs[booleanPreferencesKey("app_block_enabled")] ?: false
            val legacyBlockerEnabled = prefs[booleanPreferencesKey("blocker_enabled")]

            if (legacyBlockerEnabled != null && prefs[protectionEnabledKey] == null) {
                prefs[protectionEnabledKey] = legacyBlockerEnabled
            }

            PackageConstants.ALL_PACKAGE_NAMES.forEach { packageName ->
                if (prefs[scheduleKey(packageName)] != null) return@forEach
                val days = (1..7).associateWith { dayOfWeek ->
                    val blockedDay = legacyAppBlockEnabled && dayOfWeek in legacyBlockedDays
                    DaySchedule(
                        access = if (blockedDay) AppAccess.BLOQUEADO else AppAccess.LIBERADO,
                        windows = emptyList(),
                        shorts = when {
                            dayOfWeek !in legacyAllowedDays -> ShortsPolicy.BLOQUEADO
                            legacyQuota > 0 -> ShortsPolicy.COTA
                            else -> ShortsPolicy.BLOQUEADO
                        },
                        shortsQuotaMinutes = legacyQuota.coerceIn(
                            0,
                            DaySchedule.MAX_QUOTA_MINUTES,
                        ),
                    )
                }
                val schedule = AppSchedule(
                    packageName = packageName,
                    monitored = legacyTracked.contains(packageName),
                    days = days,
                )
                prefs[scheduleKey(packageName)] = ScheduleCodec.encode(schedule)
            }

            prefs[migratedKey] = true
            Timber.i("Configuracoes antigas migradas para a agenda semanal")
        }
    }
}
