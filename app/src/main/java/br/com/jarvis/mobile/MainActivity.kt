package br.com.jarvis.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.jarvis.mobile.actions.AndroidActions
import br.com.jarvis.mobile.actions.AppAction
import br.com.jarvis.mobile.ui.JarvisScreen
import br.com.jarvis.mobile.ui.JarvisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: JarvisViewModel by viewModels()
    private val actions = AndroidActions()
    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.start() else model.permissionDenied()
    }
    private val requestCalendar = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        model.onCalendarPermissionResult()
    }
    private val importConversation = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(model::importConversation)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.actions.collect { action ->
                    if (action == AppAction.RequestCalendarPermission) {
                        requestCalendar.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    } else actions.execute(this@MainActivity, action)?.let(model::actionFailed)
                }
            }
        }
        setContent {
            JarvisTheme {
                val state = model.state.collectAsStateWithLifecycle().value
                JarvisScreen(
                    state = state,
                    onToggle = {
                        if (state.enabled || state.thinking || state.speaking) model.stop()
                        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) model.start()
                        else requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName")))
                    },
                    onVoiceTest = model::testVoice,
                    onVoiceSettings = model::voiceSettings,
                    onDownloadModel = model::downloadConversation,
                    onImportModel = { importConversation.launch(arrayOf("*/*")) },
                    onSendMessage = model::sendMessage,
                    onCancelModel = model::cancelModelPreparation
                )
            }
        }
    }
    override fun onStart() { super.onStart(); model.onForeground() }
    override fun onStop() { model.onBackground(); super.onStop() }
}
