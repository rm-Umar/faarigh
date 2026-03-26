package com.faarigh.app.widget

import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.faarigh.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Base widget provider for all Faarigh widget sizes.
 * Uses pure RemoteViews for maximum device compatibility (no Glance dependency).
 */
abstract class FaarighWidget : AppWidgetProvider() {

    enum class WidgetSize { SMALL, MEDIUM, LARGE }

    abstract val widgetSize: WidgetSize

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        try {
            val data = loadWidgetData(context)
            for (appWidgetId in appWidgetIds) {
                try {
                    val views = buildRemoteViews(context, data)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    android.util.Log.e("FaarighWidget", "buildRemoteViews failed for $appWidgetId", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FaarighWidget", "loadWidgetData failed", e)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget first placed on home screen
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed
    }

    private fun buildRemoteViews(context: Context, data: WidgetData): RemoteViews {
        val layoutId = when (widgetSize) {
            WidgetSize.SMALL -> R.layout.widget_small
            WidgetSize.MEDIUM -> R.layout.widget_medium
            WidgetSize.LARGE -> R.layout.widget_large
        }

        val views = RemoteViews(context.packageName, layoutId)

        // Format screen time
        val hours = data.screenTimeMin / 60
        val mins = data.screenTimeMin % 60
        val screenTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        // Set open-app intent on root
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent != null) {
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        }

        when (widgetSize) {
            WidgetSize.SMALL -> {
                views.setTextViewText(R.id.tv_screen_time, screenTimeStr)
                views.setTextViewText(R.id.tv_choices, "${data.consciousChoices}")
            }

            WidgetSize.MEDIUM -> {
                views.setTextViewText(R.id.tv_screen_time, screenTimeStr)
                views.setTextViewText(R.id.tv_choices, "${data.consciousChoices}")
                views.setTextViewText(R.id.tv_intentional, "${data.intentionalPercent}%")
                val dateStr = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
                views.setTextViewText(R.id.tv_date, dateStr)
            }

            WidgetSize.LARGE -> {
                views.setTextViewText(R.id.tv_screen_time, screenTimeStr)
                views.setTextViewText(R.id.tv_choices, "${data.consciousChoices}")
                views.setTextViewText(R.id.tv_intentional, "${data.intentionalPercent}%")

                // Module status dots (green = active, gray = inactive)
                val activeColor = 0xFF6B9E5B.toInt()
                val inactiveColor = 0xFFBDBDBD.toInt()

                views.setTextColor(
                    R.id.tv_dot_app_pause,
                    if (data.appPauseEnabled) activeColor else inactiveColor,
                )
                views.setTextColor(
                    R.id.tv_dot_shorts,
                    if (data.shortsBlockerEnabled) activeColor else inactiveColor,
                )
                views.setTextColor(
                    R.id.tv_dot_dns,
                    if (data.dnsFilterEnabled) activeColor else inactiveColor,
                )
            }
        }

        return views
    }

    private fun loadWidgetData(context: Context): WidgetData {
        val screenTimeMin = getScreenTimeMinutes(context)
        val (consciousChoices, intentionalPercent) = getInterventionStats(context)
        val moduleStates = getModuleStates(context)

        return WidgetData(
            screenTimeMin = screenTimeMin,
            consciousChoices = consciousChoices,
            intentionalPercent = intentionalPercent,
            appPauseEnabled = moduleStates.appPause,
            shortsBlockerEnabled = moduleStates.shorts,
            dnsFilterEnabled = moduleStates.dns,
        )
    }

    private fun getScreenTimeMinutes(context: Context): Long {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            val totalMs = stats?.sumOf { it.totalTimeInForeground } ?: 0L
            totalMs / 60_000
        } catch (_: Exception) {
            0L
        }
    }

    private fun getInterventionStats(context: Context): Pair<Int, Int> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L

        var consciousChoices = 0
        var intentionalPercent = 0
        try {
            val dbFile = File(context.getDatabasePath("faarigh.db").absolutePath)
            if (dbFile.exists()) {
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
                )
                val cursor = db.rawQuery(
                    "SELECT " +
                        "SUM(CASE WHEN action = 'blocked' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN action = 'allowed' THEN 1 ELSE 0 END) " +
                        "FROM intervention_events WHERE timestamp >= ? AND timestamp <= ?",
                    arrayOf(todayStart.toString(), todayEnd.toString()),
                )
                if (cursor.moveToFirst()) {
                    consciousChoices = cursor.getInt(0)
                    val allowed = cursor.getInt(1)
                    val total = consciousChoices + allowed
                    intentionalPercent = if (total > 0) {
                        (consciousChoices.toFloat() / total * 100f).toInt()
                    } else 0
                }
                cursor.close()
                db.close()
            }
        } catch (_: Exception) {
            // DB might not exist yet
        }

        return Pair(consciousChoices, intentionalPercent)
    }

    /**
     * Read module enabled states from the DataStore preferences protobuf file.
     * Since DataStore uses protobuf internally, we read from SharedPreferences
     * that are synced by the app, or fall back to reading the DataStore file.
     *
     * For simplicity and compatibility, we read the underlying preferences XML
     * generated by AndroidX DataStore (stored under datastore/ directory as protobuf).
     * We use a lightweight approach: read the SharedPreferences "faarigh_prefs" which
     * the app maintains, or try DataStore directly.
     */
    private fun getModuleStates(context: Context): ModuleStates {
        // Try reading from the DataStore preferences protobuf file
        try {
            val prefsFile = File(context.filesDir, "datastore/module_prefs.preferences_pb")
            if (prefsFile.exists()) {
                val bytes = prefsFile.readBytes()
                // Parse the protobuf to extract boolean preferences
                // DataStore preferences protobuf format:
                // Each preference is stored as a field with the key name and value
                val content = String(bytes, Charsets.UTF_8) // rough parse for key detection
                val appPause = containsEnabledPref(bytes, "app_pause_enabled")
                val shorts = containsEnabledPref(bytes, "shorts_blocker_enabled")
                val dns = containsEnabledPref(bytes, "dns_filter_enabled")
                return ModuleStates(appPause, shorts, dns)
            }
        } catch (_: Exception) {
            // Fall through to defaults
        }

        return ModuleStates(appPause = false, shorts = false, dns = false)
    }

    /**
     * Parse the DataStore preferences protobuf to check if a boolean preference is true.
     *
     * AndroidX DataStore Preferences protobuf schema:
     * - Field 1 (preferences): repeated PreferencesEntry
     *   - Field 1 (key): string
     *   - Field 6 (boolean): bool
     *
     * We search for the key string followed by a true boolean value.
     */
    private fun containsEnabledPref(bytes: ByteArray, key: String): Boolean {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        // Find the key in the protobuf bytes
        var i = 0
        while (i < bytes.size - keyBytes.size) {
            // Check if we found the key string
            var found = true
            for (j in keyBytes.indices) {
                if (bytes[i + j] != keyBytes[j]) {
                    found = false
                    break
                }
            }
            if (found) {
                // Look for a boolean true value (0x01) within the next ~10 bytes
                // In protobuf, boolean field 6 is wire type 0, so tag = (6 << 3) | 0 = 0x30
                // followed by 0x01 for true
                val searchEnd = minOf(i + keyBytes.size + 10, bytes.size)
                for (k in (i + keyBytes.size) until searchEnd - 1) {
                    if (bytes[k] == 0x30.toByte() && bytes[k + 1] == 0x01.toByte()) {
                        return true
                    }
                }
                return false // Found key but value is not true
            }
            i++
        }
        return false
    }

    companion object {
        /**
         * Trigger an update for all widget sizes. Call this from anywhere in the app
         * when data changes (e.g., after an intervention event).
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetClasses = listOf(
                FaarighWidgetSmall::class.java,
                FaarighWidgetMedium::class.java,
                FaarighWidgetLarge::class.java,
            )
            for (cls in widgetClasses) {
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, cls).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            }
        }
    }
}

data class WidgetData(
    val screenTimeMin: Long,
    val consciousChoices: Int,
    val intentionalPercent: Int,
    val appPauseEnabled: Boolean = false,
    val shortsBlockerEnabled: Boolean = false,
    val dnsFilterEnabled: Boolean = false,
)

data class ModuleStates(
    val appPause: Boolean,
    val shorts: Boolean,
    val dns: Boolean,
)
