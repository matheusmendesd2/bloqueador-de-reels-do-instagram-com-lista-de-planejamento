package com.rendox.routineblocker.feature.shortsblocker.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.BlockAction
import com.rendox.routineblocker.feature.shortsblocker.models.BlockReason
import com.rendox.routineblocker.feature.shortsblocker.models.BlockerSettings
import com.rendox.routineblocker.feature.shortsblocker.services.detectors.InstagramReelsDetector
import com.rendox.routineblocker.feature.shortsblocker.services.detectors.ShortFormContentDetector
import com.rendox.routineblocker.feature.shortsblocker.services.detectors.YouTubeShortsDetector
import com.rendox.routineblocker.feature.shortsblocker.utils.PackageConstants
import com.rendox.routineblocker.feature.shortsblocker.utils.UserPreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Aplica as regras configuradas pelo usuario.
 *
 * Duas camadas de bloqueio, avaliadas nesta ordem:
 *  1. abertura do app - dias totalmente bloqueados e faixas de horario permitidas
 *  2. conteudo curto - Reels/Shorts bloqueados, liberados ou limitados por cota
 */
@SuppressLint("AccessibilityPolicy")
class ShortFormContentBlockerService : AccessibilityService() {

    private companion object {
        const val ACTION_COOLDOWN_MILLIS = 1_500L
        const val HEARTBEAT_INTERVAL_MILLIS = 20_000L
        const val SHORTS_SESSION_TIMEOUT_MILLIS = 90_000L
        const val NO_MONITORED_PACKAGE_PLACEHOLDER = "com.rendox.routineblocker.nenhum"
    }

    private val preferences by lazy { UserPreferencesProvider(applicationContext) }
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val detectors: Map<String, ShortFormContentDetector> by lazy {
        mapOf(
            PackageConstants.YOUTUBE_PACKAGE to YouTubeShortsDetector(),
            PackageConstants.INSTAGRAM_PACKAGE to InstagramReelsDetector(),
        )
    }

    private val lastActionTimestamps = ConcurrentHashMap<String, Long>()

    @Volatile private var settings = BlockerSettings()
    @Volatile private var schedules: Map<String, AppSchedule> = emptyMap()
    @Volatile private var usageToday: Map<String, Int> = emptyMap()

    /** Pacote em primeiro plano, usado para reavaliar as regras fora dos eventos. */
    @Volatile private var foregroundPackage: String? = null

    /** Pacote cujo conteudo curto esta sendo assistido agora. */
    @Volatile private var shortsActivePackage: String? = null

    @Volatile private var lastShortsDetectionUptime = 0L
    private var accumulatedSeconds = 0

    override fun onServiceConnected() {
        coroutineScope.launch {
            preferences.migrateLegacySettingsIfNeeded()
            preferences.resetUsageIfNewDay()
        }

        preferences.monitoredPackages
            .onEach { packages -> applyServiceInfo(packages) }
            .catch { error -> Timber.e(error, "Falha ao aplicar os pacotes monitorados") }
            .launchIn(coroutineScope)

        preferences.settings
            .onEach { settings = it }
            .catch { error -> Timber.e(error, "Falha ao ler as configuracoes") }
            .launchIn(coroutineScope)

        preferences.schedules
            .onEach { list -> schedules = list.associateBy { it.packageName } }
            .catch { error -> Timber.e(error, "Falha ao ler as agendas") }
            .launchIn(coroutineScope)

        preferences.usageToday
            .onEach { usageToday = it }
            .catch { error -> Timber.e(error, "Falha ao ler o consumo do dia") }
            .launchIn(coroutineScope)

        startHeartbeat()
    }

    private fun applyServiceInfo(packages: List<String>) {
        val watched = packages.ifEmpty { listOf(NO_MONITORED_PACKAGE_PLACEHOLDER) }
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            packageNames = watched.toTypedArray()
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val isRelevantContentChange =
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0
        if (!isWindowChange && !isRelevantContentChange) return

        val packageName = event.packageName?.toString() ?: return
        if (isWindowChange && packageName != foregroundPackage) {
            // trocou de app: a sessao de conteudo curto anterior acabou
            endShortsSession()
            foregroundPackage = packageName
        }

        val schedule = schedules[packageName]
        if (schedule == null || !schedule.monitored) return
        if (!settings.isActive(System.currentTimeMillis())) return

        // 1) abertura do app
        val accessReason = schedule.appAccessReason(currentDayOfWeek(), currentMinuteOfDay())
        if (accessReason != BlockReason.NENHUM) {
            endShortsSession()
            block(packageName, accessReason)
            return
        }

        // 2) conteudo curto dentro do app
        val detector = detectors[packageName] ?: return
        val isWatchingShortForm = windows.any { window ->
            if (!window.isFocused || !window.isActive) return@any false
            val root = window.root ?: return@any false
            detector.isShortFormContent(event, root, resources)
        }
        // Uma deteccao negativa isolada nao encerra a sessao - o Instagram dispara muitos
        // eventos intermediarios. Quem encerra e o timeout do heartbeat.
        if (!isWatchingShortForm) return

        val shortsReason = schedule.shortsReason(
            dayOfWeek = currentDayOfWeek(),
            usedMinutesToday = usageToday[packageName] ?: 0,
        )
        if (shortsReason != BlockReason.NENHUM) {
            endShortsSession()
            block(packageName, shortsReason)
        } else {
            shortsActivePackage = packageName
            lastShortsDetectionUptime = SystemClock.uptimeMillis()
        }
    }

    override fun onInterrupt() {
        Timber.w("Servico de bloqueio interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    /**
     * Roda em paralelo aos eventos e cuida de duas coisas que eventos nao resolvem:
     * contar os minutos consumidos e expulsar o usuario quando a janela de horario acaba.
     */
    private fun startHeartbeat() {
        coroutineScope.launch {
            var lastKnownDate = LocalDate.now()
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MILLIS)

                val today = LocalDate.now()
                if (today != lastKnownDate) {
                    lastKnownDate = today
                    accumulatedSeconds = 0
                    preferences.resetUsageIfNewDay()
                }

                if (!settings.isActive(System.currentTimeMillis())) continue

                accumulateShortsUsage()
                enforceScheduleOnForegroundApp()
            }
        }
    }

    private suspend fun accumulateShortsUsage() {
        val packageName = shortsActivePackage ?: return
        val idleMillis = SystemClock.uptimeMillis() - lastShortsDetectionUptime
        if (idleMillis >= SHORTS_SESSION_TIMEOUT_MILLIS) {
            Timber.d("Sessao de conteudo curto encerrada por inatividade")
            endShortsSession()
            return
        }
        accumulatedSeconds += (HEARTBEAT_INTERVAL_MILLIS / 1000).toInt()
        while (accumulatedSeconds >= 60) {
            accumulatedSeconds -= 60
            preferences.incrementUsedMinutes(packageName, 1)
            Timber.d("[$packageName] +1 min de consumo hoje")
        }
    }

    /**
     * Aplica o fim de uma janela de horario mesmo sem novos eventos de acessibilidade.
     *
     * O pacote vem da janela ativa de verdade, e nao do ultimo evento recebido: como o
     * servico so recebe eventos dos apps monitorados, o ultimo evento pode ser de um app
     * que o usuario ja fechou.
     */
    private fun enforceScheduleOnForegroundApp() {
        val packageName = runCatching { rootInActiveWindow?.packageName?.toString() }
            .getOrNull()
            ?: return
        foregroundPackage = packageName
        val schedule = schedules[packageName] ?: return
        if (!schedule.monitored) return
        val reason = schedule.appAccessReason(currentDayOfWeek(), currentMinuteOfDay())
        if (reason != BlockReason.NENHUM) {
            endShortsSession()
            block(packageName, reason)
        }
    }

    private fun endShortsSession() {
        shortsActivePackage = null
        accumulatedSeconds = 0
    }

    private fun block(packageName: String, reason: BlockReason) {
        if (!shouldPerformAction("$packageName:${reason.name}")) return
        Timber.i("[$packageName] Bloqueando: $reason")

        val action = when (settings.blockAction) {
            BlockAction.VOLTAR -> GLOBAL_ACTION_BACK
            BlockAction.TELA_INICIAL -> GLOBAL_ACTION_HOME
        }
        performGlobalAction(action)

        if (settings.showBlockWarning) {
            showWarning(packageName, reason)
        }
    }

    private fun showWarning(packageName: String, reason: BlockReason) {
        val appName = PackageConstants.displayNameOf(packageName)
        val shortFormName = PackageConstants.shortFormNameOf(packageName)
        val detail = when (reason) {
            BlockReason.APP_BLOQUEADO_HOJE -> "$appName está bloqueado hoje"
            BlockReason.FORA_DA_JANELA -> "$appName está fora do horário liberado"
            BlockReason.SHORTS_BLOQUEADO -> "$shortFormName estão bloqueados hoje"
            BlockReason.COTA_ESGOTADA -> "Cota de $shortFormName de hoje esgotada"
            BlockReason.NENHUM -> return
        }
        val message = "$detail\n${settings.blockMessage}"
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shouldPerformAction(key: String): Boolean {
        val now = SystemClock.uptimeMillis()
        val last = lastActionTimestamps[key] ?: 0L
        if (now - last < ACTION_COOLDOWN_MILLIS) return false
        lastActionTimestamps[key] = now
        return true
    }

    private fun currentDayOfWeek(): Int = LocalDate.now().dayOfWeek.value

    private fun currentMinuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
}
