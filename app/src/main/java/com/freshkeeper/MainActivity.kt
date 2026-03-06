package com.freshkeeper

import android.Manifest
import android.content.Context
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // no-op
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // no-op
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsOnFirstLaunch()
        setContent {
            AppContent()
        }
    }

    private fun requestPermissionsOnFirstLaunch() {
        val prefs = getSharedPreferences("freshkeeper_prefs", Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionOnce(
                prefs = prefs,
                permission = Manifest.permission.POST_NOTIFICATIONS,
                prefKey = KEY_NOTIFICATIONS_REQUESTED,
                launcher = notificationPermissionLauncher::launch,
            )
        }

        requestPermissionOnce(
            prefs = prefs,
            permission = Manifest.permission.CAMERA,
            prefKey = KEY_CAMERA_REQUESTED,
            launcher = cameraPermissionLauncher::launch,
        )
    }

    private fun requestPermissionOnce(
        prefs: android.content.SharedPreferences,
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
            launcher(permission)
            prefs.edit().putBoolean(prefKey, true).apply()
        }
    }

    companion object {
        private const val KEY_NOTIFICATIONS_REQUESTED = "notifications_requested"
        private const val KEY_CAMERA_REQUESTED = "camera_requested"
    }
}

@Composable
private fun AppContent() {
    FreshKeeperRoot()
}
