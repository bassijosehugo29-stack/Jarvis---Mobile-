package br.com.jarvis.mobile.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.provider.AlarmClock
import br.com.jarvis.mobile.domain.LaunchableApp

sealed interface AppAction {
    data object OpenChatGpt : AppAction
    data class OpenApp(val app: LaunchableApp) : AppAction
    data object RequestCalendarPermission : AppAction
    data class SetAlarm(val hour: Int? = null, val minute: Int? = null) : AppAction
}

/** Official intents and exported launcher activities only. Never automate another app's private UI. */
class AndroidActions {
    fun execute(activity: Activity, action: AppAction): String? {
        return try {
            val intent = when (action) {
                AppAction.OpenChatGpt -> activity.packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                    ?: return "O aplicativo oficial do ChatGPT não está instalado. Instale-o pela Play Store e tente novamente."
                is AppAction.SetAlarm -> alarmIntent(action)
                is AppAction.OpenApp -> launchIntent(action.app)
                AppAction.RequestCalendarPermission -> return "A permissão da agenda deve ser solicitada pela Activity."
            }
            activity.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Não há aplicativo disponível para executar essa ação."
        } catch (_: SecurityException) {
            "O Android bloqueou essa ação. Verifique as permissões do aplicativo de destino."
        }
    }

    internal fun launchIntent(app: LaunchableApp): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(requireNotNull(ComponentName.unflattenFromString(app.component)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    internal fun alarmIntent(action: AppAction.SetAlarm): Intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Mobile")
        // Clock providers may still show their own confirmation. Android gives no save result.
        putExtra(AlarmClock.EXTRA_SKIP_UI, action.hour != null && action.minute != null)
        if (action.hour != null && action.minute != null) {
            require(action.hour in 0..23 && action.minute in 0..59)
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
        }
    }
}
