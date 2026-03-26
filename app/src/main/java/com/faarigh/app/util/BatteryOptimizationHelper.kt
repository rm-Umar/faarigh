package com.faarigh.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helps request battery optimization exemption so our services
 * (Accessibility, VPN) don't get killed by Android's battery saver.
 */
object BatteryOptimizationHelper {

    /**
     * Returns true if the app is already exempt from battery optimization.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system dialog asking the user to disable battery optimization.
     * This is a direct intent — no Play Store policy issues for personal apps.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback: open battery optimization settings list
            openBatteryOptimizationSettings(context)
        }
    }

    /**
     * Opens the battery optimization settings list (fallback).
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Last resort: general battery settings
            try {
                context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {}
        }
    }

    /**
     * Some OEMs (Xiaomi, Samsung, Huawei, OnePlus, Oppo, Vivo) have their own
     * aggressive battery killers on top of Android's. This opens the
     * manufacturer-specific autostart/background permission screen.
     *
     * Returns true if a manufacturer-specific intent was found and launched.
     */
    fun openManufacturerBatterySettings(context: Context): Boolean {
        val intents = listOf(
            // Xiaomi / MIUI
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            // Samsung
            Intent().setClassName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            ),
            // Huawei / EMUI
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            // OnePlus / OxygenOS
            Intent().setClassName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            ),
            // Oppo / ColorOS
            Intent().setClassName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            // Vivo / Funtouch
            Intent().setClassName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            // Realme
            Intent().setClassName(
                "com.heytap.mcs",
                "com.oplus.safe.permission.startup.StartupAppListActivity"
            ),
        )

        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    continue
                }
            }
        }
        return false
    }

    /**
     * Get the device manufacturer for showing specific instructions.
     */
    fun getManufacturer(): String = Build.MANUFACTURER.lowercase()

    /**
     * Returns human-readable instructions for the user's device brand.
     */
    fun getManufacturerInstructions(): String? {
        return when (getManufacturer()) {
            "xiaomi", "redmi", "poco" ->
                "On Xiaomi devices: Go to Settings → Apps → Manage apps → Faarigh → Autostart (enable) and Battery saver → No restrictions"
            "samsung" ->
                "On Samsung: Go to Settings → Battery → Background usage limits → Never sleeping apps → Add Faarigh"
            "huawei", "honor" ->
                "On Huawei: Go to Settings → Battery → App launch → Faarigh → Manage manually → Enable all three toggles"
            "oneplus" ->
                "On OnePlus: Go to Settings → Battery → Battery optimization → Faarigh → Don't optimize"
            "oppo", "realme" ->
                "On OPPO/Realme: Go to Settings → Battery → More battery settings → Optimize battery use → Faarigh → Don't optimize"
            "vivo" ->
                "On Vivo: Go to Settings → Battery → Background power consumption → Faarigh → Don't restrict"
            "google" ->
                "On Pixel: Go to Settings → Battery → Battery optimization → Faarigh → Don't optimize"
            else -> null
        }
    }
}
