package app.cleanlaunch.tv

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.cleanlaunch.tv.theme.LocalPaperScale
import app.cleanlaunch.tv.ui.EditorScreen
import app.cleanlaunch.tv.ui.FolioDim
import app.cleanlaunch.tv.ui.HomeScreen
import app.cleanlaunch.tv.data.PhotoFolder
import app.cleanlaunch.tv.ui.PickerScreen
import app.cleanlaunch.tv.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val model: EveningViewModel by viewModels()
    private val openPhotos = registerForActivityResult(OpenImages()) { uris ->
        uris.forEach { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        if (!model.takePickedPhotos(uris)) model.addPickedPhotos(uris)
    }
    private val askPhotoRead = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.onPhotoAccess(granted)
    }
    private var swallowUp = false
    private var packagesRegistered = false
    private var volumeRegistered = false
    private val volumeObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            model.onVolumeHint()
        }
    }
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val stream = intent?.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) ?: -1
            if (stream != -1 && stream != android.media.AudioManager.STREAM_MUSIC) return
            model.onVolumeHint()
        }
    }
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED && replacing) return
            val pkg = intent.data?.schemeSpecificPart.orEmpty()
            Log.i("CleanLaunch", "Index refresh ${intent.action} $pkg")
            model.refreshApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    model.onBack()
                }
            },
        )
        model.launchPhotoPicker = { openPhotos.launch(Unit) }
        model.askPhotoRead = { askPhotoRead.launch(PhotoFolder.permission()) }
        model.applyDebugScheme(intent)
        setContent { EveningRoot(model) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val home = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        if (home) model.onHomeIntent()
        model.applyDebugScheme(intent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packages, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(packages, filter)
        }
        packagesRegistered = true
        contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeObserver)
        val volumeFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(volumeReceiver, volumeFilter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(volumeReceiver, volumeFilter)
        }
        volumeRegistered = true
        model.onVolumeHint()
    }

    override fun onResume() {
        super.onResume()
        model.setForeground(true)
        model.refreshApps()
        model.refreshInputs()
        model.noteInput()
        model.refreshSlotPhotos()
        model.refreshNowPlaying()
        if (PhotoFolder.canRead(this)) model.refreshPhotoFolder()
        else if (!model.photoReadAsked()) model.requestPhotoRead()
    }

    override fun onPause() {
        model.setForeground(false)
        super.onPause()
    }

    override fun onStop() {
        if (packagesRegistered) {
            unregisterReceiver(packages)
            packagesRegistered = false
        }
        if (volumeRegistered) {
            contentResolver.unregisterContentObserver(volumeObserver)
            unregisterReceiver(volumeReceiver)
            volumeRegistered = false
        }
        super.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (model.onKeyWhileDimmed()) {
                swallowUp = true
                return true
            }
            model.noteInput()
        } else if (event.action == KeyEvent.ACTION_UP && swallowUp) {
            swallowUp = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

private class OpenImages : androidx.activity.result.contract.ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            return (0 until clip.itemCount).map { clip.getItemAt(it).uri }
        }
        return listOfNotNull(intent.data)
    }
}

@Composable
private fun EveningRoot(model: EveningViewModel) {
    var state by remember { mutableStateOf(model.state) }
    DisposableEffect(model) {
        val stop = model.observe { state = it }
        onDispose(stop)
    }
    val scheme = model.design.scheme(state.schemeId)
    // While music plays, the panel stays awake so the TV's own screen saver does not start.
    val activity = LocalContext.current as? Activity
    val musicPlaying = state.nowPlaying?.playing == true
    DisposableEffect(musicPlaying, activity) {
        if (musicPlaying) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = if (maxWidth > 0.dp) maxWidth / 960.dp else 1f
        CompositionLocalProvider(LocalPaperScale provides scale) {
            Box(Modifier.fillMaxSize().background(scheme.background)) {
                if (state.dimmed && state.overlay == Overlay.None) {
                    FolioDim(state, model.design, scheme)
                } else {
                    when (state.overlay) {
                        Overlay.None -> HomeScreen(model, state, model.design, scheme)
                        Overlay.Settings -> SettingsScreen(model, state, model.design, scheme)
                        Overlay.Editor -> EditorScreen(model, state, model.design, scheme)
                        Overlay.Picker -> PickerScreen(model, state, model.design, scheme)
                    }
                }
            }
        }
    }
}
