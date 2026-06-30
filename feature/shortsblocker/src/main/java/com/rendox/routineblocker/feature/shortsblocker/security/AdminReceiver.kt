package com.rendox.routineblocker.feature.shortsblocker.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Desative o administrador do dispositivo apenas se pretende desinstalar o " +
            "RoutineBlocker. Se precisar alterar as configurações, use a senha dentro do app."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Timber.w("Device admin desativado pelo usuario")
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Timber.i("Device admin ativado")
    }
}
