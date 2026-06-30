package com.rendox.routineblocker

import android.app.Application
import android.os.Build
import android.os.Environment
import com.rendox.routinetracker.core.data.di.completionHistoryDataModule
import com.rendox.routinetracker.core.data.di.completionTimeDataModule
import com.rendox.routinetracker.core.data.di.routineDataModule
import com.rendox.routinetracker.core.data.di.streakDataModule
import com.rendox.routinetracker.core.data.di.vacationDataModule
import com.rendox.routinetracker.core.database.di.completionTimeLocalDataModule
import com.rendox.routinetracker.core.database.di.habitLocalDataModule
import com.rendox.routinetracker.core.database.di.localDataSourceModule
import com.rendox.routinetracker.core.database.di.streakLocalDataModule
import com.rendox.routinetracker.core.domain.databaseprepopulator.DatabasePrepopulator
import com.rendox.routinetracker.core.domain.databaseprepopulator.databasePrepopulatorModule
import com.rendox.routinetracker.core.domain.di.completionHistoryDomainModule
import com.rendox.routinetracker.core.domain.di.completionTimeDomainModule
import com.rendox.routinetracker.core.domain.di.domainModule
import com.rendox.routinetracker.core.domain.di.habitDomainModule
import com.rendox.routinetracker.core.domain.di.streakDomainModule
import com.rendox.routinetracker.feature.agenda.di.agendaScreenModule
import com.rendox.routinetracker.routinedetails.di.routineDetailsModule
import com.rendox.routineblocker.feature.shortsblocker.di.shortsBlockerModule
import kotlin.coroutines.CoroutineContext
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutineBlockerApp : Application() {
    private val ioDispatcher by inject<CoroutineContext>(qualifier = named("ioDispatcher"))
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val databasePrepopulator by inject<DatabasePrepopulator>()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        Timber.plant(CrashLogTree(applicationContext))

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            val stackTrace = LogWriter.getStackTrace(throwable)
            LogWriter.writeToFile(stackTrace, this)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        startKoin {
            androidContext(this@RoutineBlockerApp)
            modules(
                localDataSourceModule,
                habitLocalDataModule,
                completionTimeLocalDataModule,
                streakLocalDataModule,

                routineDataModule,
                completionHistoryDataModule,
                completionTimeDataModule,
                vacationDataModule,
                streakDataModule,
                databasePrepopulatorModule,

                domainModule,
                habitDomainModule,
                completionHistoryDomainModule,
                streakDomainModule,
                completionTimeDomainModule,

                agendaScreenModule,
                routineDetailsModule,
                shortsBlockerModule,
            )
        }

        applicationScope.launch(ioDispatcher) {
            databasePrepopulator.prepopulateDatabase(numOfHabits = 30)
        }
    }
}

class CrashLogTree(private val context: android.content.Context) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= android.util.Log.ERROR) {
            val logMessage = buildString {
                append("[$tag] $message")
                if (t != null) {
                    append("\n${LogWriter.getStackTrace(t)}")
                }
            }
            LogWriter.writeToFile(logMessage, context)
        }
    }
}

object LogWriter {
    private const val FILE_NAME = "routineblocker_crash.log"

    fun getStackTrace(throwable: Throwable): String {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    fun writeToFile(content: String, context: android.content.Context) {
        try {
            val dir = File(context.filesDir, "logs")
            dir.mkdirs()
            val file = File(dir, FILE_NAME)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val timestamp = sdf.format(Date())
            val entry = "=== $timestamp ===\n$content\n\n"
            FileWriter(file, true).use { it.append(entry) }
        } catch (e: Exception) {
            android.util.Log.e("LogWriter", "Failed to write crash log", e)
        }
    }
}
