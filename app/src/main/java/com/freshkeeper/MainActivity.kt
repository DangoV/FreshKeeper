package com.freshkeeper

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.freshkeeper.ui.FreshKeeperRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private val pendingPermissionRequests = ArrayDeque<PermissionRequest>()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        requestNextPermissionIfNeeded()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        requestNextPermissionIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("freshkeeper_prefs", Context.MODE_PRIVATE)
        enqueueFirstLaunchPermissions()
        requestNextPermissionIfNeeded()

        setContent {
            AppContent()
        }
    }

    private fun enqueueFirstLaunchPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            maybeAddPermissionRequest(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                prefKey = KEY_NOTIFICATIONS_REQUESTED,
                launcher = notificationPermissionLauncher::launch,
            )
        }

        maybeAddPermissionRequest(
            permission = Manifest.permission.CAMERA,
            prefKey = KEY_CAMERA_REQUESTED,
            launcher = cameraPermissionLauncher::launch,
        )
    }

    private fun maybeAddPermissionRequest(
        permission: String,
        prefKey: String,
        launcher: (String) -> Unit,
    ) {
        val alreadyRequested = prefs.getBoolean(prefKey, false)
        val granted = ContextCompat.checkSelfPermission(
            this,
            permission,
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyRequested && !granted) {
            pendingPermissionRequests.addLast(
                PermissionRequest(
                    permission = permission,
                    prefKey = prefKey,
                    launcher = launcher,
                ),
            )
        }
    }

    private fun requestNextPermissionIfNeeded() {
        val request = pendingPermissionRequests.removeFirstOrNull() ?: return
        prefs.edit().putBoolean(request.prefKey, true).apply()
        request.launcher(request.permission)
    }

    private data class PermissionRequest(
        val permission: String,
        val prefKey: String,
        val launcher: (String) -> Unit,
    )

    companion object {
        private const val KEY_NOTIFICATIONS_REQUESTED = "notifications_requested"
        private const val KEY_CAMERA_REQUESTED = "camera_requested"
    }
}

@Composable
private fun AppContent() {
    FreshKeeperRoot()
}
