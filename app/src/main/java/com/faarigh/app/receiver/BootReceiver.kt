package com.faarigh.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.faarigh.app.service.vpn.FaarighVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restarts services after device reboot.
 * The accessibility service auto-restarts (Android handles it),
 * but VPN must be explicitly restarted.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot completed — checking which services to restart")

        // Check if VPN was enabled before reboot
        val prefs = context.getSharedPreferences("faarigh_prefs", Context.MODE_PRIVATE)
        val vpnWasEnabled = prefs.getBoolean("vpn_enabled", false)

        if (vpnWasEnabled) {
            Log.i(TAG, "VPN was enabled before reboot — restarting")
            try {
                FaarighVpnService.start(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart VPN: ${e.message}")
            }
        }

        // Accessibility service is auto-restarted by Android if enabled in settings
        Log.i(TAG, "Boot receiver done")
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
