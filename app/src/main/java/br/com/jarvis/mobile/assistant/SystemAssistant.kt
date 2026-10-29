package br.com.jarvis.mobile.assistant

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Activity-based official assistant integration: no persistent VoiceInteractionService or hotword. */
object SystemAssistant {
    fun selected(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 29) {
        val roles = context.getSystemService(RoleManager::class.java)
        roles?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true && roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)
    } else false
    fun selection(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true && !roles.isRoleHeld(RoleManager.ROLE_ASSISTANT))
                return roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
        }
        return Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
    }
}
