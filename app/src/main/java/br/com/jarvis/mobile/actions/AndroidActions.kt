package br.com.jarvis.mobile.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock

sealed interface AppAction {
    data object OpenChatGpt : AppAction
    data class SetAlarm(val hour: Int? = null, val minute: Int? = null) : AppAction
}

/** Official intents only. Alarm creation is confirmed in the device clock UI. */
class AndroidActions {
    fun execute(activity: Activity, action: AppAction): String? {
        return try {
            val intent = when (action) {
                AppAction.OpenChatGpt -> activity.packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                    ?: return "O aplicativo oficial do ChatGPT não está instalado. Instale-o pela Play Store e tente novamente."
                is AppAction.SetAlarm -> alarmIntent(action)
            }
            activity.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Não há aplicativo disponível para executar essa ação."
        } catch (_: SecurityException) {
            "O Android bloqueou essa ação. Verifique as permissões do aplicativo de destino."
        }
    }

    internal fun alarmIntent(action: AppAction.SetAlarm): Intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Mobile")
        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        if (action.hour != null && action.minute != null) {
            require(action.hour in 0..23 && action.minute in 0..59)
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
        }
    }
}
