package com.faarigh.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.service.vpn.FaarighVpnService
import com.faarigh.app.ui.navigation.FaarighNavHost
import com.faarigh.app.ui.theme.FaarighTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var modulePreferences: ModulePreferences

    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.i("MainActivity", "VPN consent granted")
            startVpnService()
        } else {
            Log.w("MainActivity", "VPN consent denied")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* VPN notification will just be silent if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val themeMode by modulePreferences.themeMode.collectAsState(initial = "auto")
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            FaarighTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FaarighNavHost(
                        onRequestVpnConsent = ::requestVpnConsent,
                    )
                }
            }
        }
    }

    fun requestVpnConsent() {
        Log.i("MainActivity", "requestVpnConsent called")
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            Log.i("MainActivity", "VPN needs consent — launching dialog")
            vpnConsentLauncher.launch(prepareIntent)
        } else {
            Log.i("MainActivity", "VPN already consented — starting service")
            startVpnService()
        }
    }

    private fun startVpnService() {
        try {
            val intent = Intent(this, FaarighVpnService::class.java).apply {
                action = FaarighVpnService.ACTION_START
            }
            startForegroundService(intent)
            Log.i("MainActivity", "VPN service start requested")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start VPN service: ${e.message}", e)
        }
    }
}
