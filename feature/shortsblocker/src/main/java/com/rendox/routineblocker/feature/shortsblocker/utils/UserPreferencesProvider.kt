package com.rendox.routineblocker.feature.shortsblocker.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rendox.routineblocker.feature.shortsblocker.models.TrackedPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shorts_blocker_settings")

class UserPreferencesProvider(context: Context) {
    private val userPreferencesKey = stringPreferencesKey("tracked_packages")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val disclosureAcceptedKey = booleanPreferencesKey("disclosure_accepted")
    private val allowedDaysKey = stringPreferencesKey("allowed_days")
    private val dailyQuotaKey = intPreferencesKey("daily_quota_minutes")
    private val todayDateKey = stringPreferencesKey("today_date")
    private val usedMinutesKey = intPreferencesKey("used_minutes_today")
    private val appBlockEnabledKey = booleanPreferencesKey("app_block_enabled")
    private val appBlockedDaysKey = stringPreferencesKey("app_blocked_days")
    private val passwordHashKey = stringPreferencesKey("password_hash")
    private val blockerEnabledKey = booleanPreferencesKey("blocker_enabled")

    private val dataStore = context.dataStore

    fun getTrackedPackages(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val packages = preferences[userPreferencesKey]?.split(",")?.filter { it.isNotBlank() }
                ?: PackageConstants.DEFAULT_ENABLED_PACKAGES
            packages
        }
    }

    suspend fun setTrackedPackages(packages: List<String>) {
        val packagesString = packages.joinToString(",")
        dataStore.edit { preferences ->
            preferences[userPreferencesKey] = packagesString
        }
    }

    fun getTrackedPackagesWithStatus(): Flow<List<TrackedPackage>> {
        return getTrackedPackages().map { enabledPackages ->
            PackageConstants.AVAILABLE_PACKAGES.map { pkg ->
                pkg.copy(isEnabled = enabledPackages.contains(pkg.packageName))
            }
        }
    }

    suspend fun togglePackage(packageName: String, enabled: Boolean) {
        val currentPackages = getTrackedPackages().first().toMutableList()
        if (enabled && !currentPackages.contains(packageName)) {
            currentPackages.add(packageName)
        } else if (!enabled) {
            currentPackages.remove(packageName)
        }
        setTrackedPackages(currentPackages)
    }

    fun getOnboardingCompleted(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[onboardingCompletedKey] ?: false
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = completed
        }
    }

    suspend fun setDisclosureAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[disclosureAcceptedKey] = accepted
        }
    }

    fun getAllowedDays(): Flow<Set<Int>> {
        return dataStore.data.map { prefs ->
            prefs[allowedDaysKey]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: emptySet()
        }
    }

    suspend fun setAllowedDays(days: Set<Int>) {
        dataStore.edit { it[allowedDaysKey] = days.joinToString(",") }
    }

    fun getDailyQuotaMinutes(): Flow<Int> {
        return dataStore.data.map { it[dailyQuotaKey] ?: 0 }
    }

    suspend fun setDailyQuotaMinutes(minutes: Int) {
        dataStore.edit { it[dailyQuotaKey] = minutes }
    }

    fun getAppBlockEnabled(): Flow<Boolean> {
        return dataStore.data.map { it[appBlockEnabledKey] ?: false }
    }

    suspend fun setAppBlockEnabled(enabled: Boolean) {
        dataStore.edit { it[appBlockEnabledKey] = enabled }
    }

    fun getAppBlockedDays(): Flow<Set<Int>> {
        return dataStore.data.map { prefs ->
            prefs[appBlockedDaysKey]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: emptySet()
        }
    }

    suspend fun setAppBlockedDays(days: Set<Int>) {
        dataStore.edit { it[appBlockedDaysKey] = days.joinToString(",") }
    }

    fun getBlockerEnabled(): Flow<Boolean> {
        return dataStore.data.map { it[blockerEnabledKey] ?: true }
    }

    suspend fun setBlockerEnabled(enabled: Boolean) {
        dataStore.edit { it[blockerEnabledKey] = enabled }
    }

    fun getPasswordHash(): Flow<String?> {
        return dataStore.data.map { it[passwordHashKey] }
    }

    suspend fun setPasswordHash(hash: String) {
        dataStore.edit { it[passwordHashKey] = hash }
    }

    suspend fun clearPasswordHash() {
        dataStore.edit { it.remove(passwordHashKey) }
    }

    fun getUsedMinutesToday(): Flow<Int> {
        return dataStore.data.map { it[usedMinutesKey] ?: 0 }
    }

    suspend fun getUsedMinutesTodayOnce(): Int {
        return dataStore.data.map { it[usedMinutesKey] ?: 0 }.first()
    }

    suspend fun incrementUsedMinutes(minutes: Int) {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val savedDate = prefs[todayDateKey]
            if (savedDate != today) {
                prefs[todayDateKey] = today
                prefs[usedMinutesKey] = 0
            }
            val current = prefs[usedMinutesKey] ?: 0
            prefs[usedMinutesKey] = current + minutes
        }
    }

    suspend fun resetUsedMinutesIfNewDay() {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val savedDate = prefs[todayDateKey]
            if (savedDate != today) {
                prefs[todayDateKey] = today
                prefs[usedMinutesKey] = 0
            }
        }
    }
}
