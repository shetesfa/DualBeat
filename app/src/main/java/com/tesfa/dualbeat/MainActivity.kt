package com.tesfa.dualbeat
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tesfa.dualbeat.core.theme.DualBeatTheme
import com.tesfa.dualbeat.core.util.PermissionManager
import com.tesfa.dualbeat.data.preferences.UserPreferences
import com.tesfa.dualbeat.ui.navigation.DualBeatNavGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DualBeatApp

        // Only perform media and bluetooth indexing on startup if permissions have already been granted.
        // Contextual permission requests are handled gracefully inside each screen.
        if (PermissionManager.hasMediaPermissions(this)) {
            CoroutineScope(Dispatchers.IO).launch {
                app.mediaRepository.refreshAll()
            }
        }

        if (PermissionManager.hasBluetoothPermissions(this)) {
            app.bluetoothDeviceManager.refreshDevices()
        }

        setContent {
            val prefs by app.preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())

            DualBeatTheme(
                themeMode = prefs.themeMode,
                accentChoice = prefs.accentChoice
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DualBeatNavGraph()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as DualBeatApp
        if (PermissionManager.hasBluetoothPermissions(this)) {
            app.bluetoothDeviceManager.refreshDevices()
        }
        if (PermissionManager.hasMediaPermissions(this) && app.mediaRepository.audioTracks.value.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                app.mediaRepository.refreshAll()
            }
        }
    }
}
