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
        // no-op: результат будет учтен системой, дополнительно ничего делать не нужно
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationsOnFirstLaunch()
        setContent {
            AppContent()
        }
    }

    private fun requestNotificationsOnFirstLaunch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = getSharedPreferences("freshkeeper_prefs", Context.MODE_PRIVATE)
        val alreadyRequested = prefs.getBoolean(KEY_NOTIFICATIONS_REQUESTED, false)

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyRequested && !granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_REQUESTED, true).apply()
        }
    }

    companion object {
        private const val KEY_NOTIFICATIONS_REQUESTED = "notifications_requested"
    }
}

@Composable
private fun AppContent() {
    FreshKeeperRoot()
}
