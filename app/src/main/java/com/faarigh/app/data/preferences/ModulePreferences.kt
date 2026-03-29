package com.faarigh.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.moduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "module_prefs")

@Singleton
class ModulePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.moduleDataStore

    // ── Keys ────────────────────────────────────────────────────────

    private object Keys {
        // App Pause
        val BREATHING_DURATION_SEC = intPreferencesKey("breathing_duration_sec") // LIGHT level
        val MEDIUM_BREATHING_SEC = intPreferencesKey("medium_breathing_sec")
        val DEEP_BREATHING_SEC = intPreferencesKey("deep_breathing_sec")
        val APP_PAUSE_COOLDOWN_MIN = intPreferencesKey("app_pause_cooldown_min")
        val APP_PAUSE_ENABLED = booleanPreferencesKey("app_pause_enabled")

        // NSFW Detection
        val NSFW_THRESHOLD = floatPreferencesKey("nsfw_threshold")
        val NSFW_SCAN_INTERVAL_MS = longPreferencesKey("nsfw_scan_interval_ms")
        val NSFW_ENABLED = booleanPreferencesKey("nsfw_enabled")

        // Shorts Blocker
        val SHORTS_BLOCKER_ENABLED = booleanPreferencesKey("shorts_blocker_enabled")
        val SHORTS_BLOCKER_APPS = stringPreferencesKey("shorts_blocker_apps")

        // DNS Filter
        val DNS_FILTER_ENABLED = booleanPreferencesKey("dns_filter_enabled")

        // VPN state (for BootReceiver)
        val VPN_ENABLED = booleanPreferencesKey("vpn_enabled")

        // Theme: "auto", "light", "dark"
        val THEME_MODE = stringPreferencesKey("theme_mode")

        // Wind-down schedule
        val WIND_DOWN_ENABLED = booleanPreferencesKey("wind_down_enabled")
        val WIND_DOWN_START_HOUR = intPreferencesKey("wind_down_start_hour")
        val WIND_DOWN_START_MIN = intPreferencesKey("wind_down_start_min")
        val WIND_DOWN_END_HOUR = intPreferencesKey("wind_down_end_hour")
        val WIND_DOWN_END_MIN = intPreferencesKey("wind_down_end_min")

        // Escalation thresholds
        val MEDIUM_THRESHOLD_MIN = intPreferencesKey("escalation_medium_threshold_min")
        val DEEP_THRESHOLD_MIN = intPreferencesKey("escalation_deep_threshold_min")

        // Breathing pattern: "simple", "sigh", "box"
        val PREFERRED_BREATHING_PATTERN = stringPreferencesKey("preferred_breathing_pattern")

        // Intervention technique: "simple_pause", "sigh", "defusion", "surfing"
        val PREFERRED_TECHNIQUE = stringPreferencesKey("preferred_technique")

        // NSFW allow cooldown
        val NSFW_ALLOW_COOLDOWN_MIN = intPreferencesKey("nsfw_allow_cooldown_min")
    }

    // ── App Pause ───────────────────────────────────────────────────

    val breathingDurationSec: Flow<Int> = dataStore.data.map { it[Keys.BREATHING_DURATION_SEC] ?: 3 }
    val mediumBreathingSec: Flow<Int> = dataStore.data.map { it[Keys.MEDIUM_BREATHING_SEC] ?: 10 }
    val deepBreathingSec: Flow<Int> = dataStore.data.map { it[Keys.DEEP_BREATHING_SEC] ?: 16 }
    val appPauseCooldownMin: Flow<Int> = dataStore.data.map { it[Keys.APP_PAUSE_COOLDOWN_MIN] ?: 30 }
    val appPauseEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.APP_PAUSE_ENABLED] ?: false }

    suspend fun setBreathingDurationSec(value: Int) {
        dataStore.edit { it[Keys.BREATHING_DURATION_SEC] = value.coerceIn(1, 10) }
    }
    suspend fun setMediumBreathingSec(value: Int) {
        dataStore.edit { it[Keys.MEDIUM_BREATHING_SEC] = value.coerceIn(5, 30) }
    }
    suspend fun setDeepBreathingSec(value: Int) {
        dataStore.edit { it[Keys.DEEP_BREATHING_SEC] = value.coerceIn(10, 30) }
    }

    suspend fun setAppPauseCooldownMin(value: Int) {
        dataStore.edit { it[Keys.APP_PAUSE_COOLDOWN_MIN] = value.coerceIn(5, 120) }
    }

    suspend fun setAppPauseEnabled(value: Boolean) {
        dataStore.edit { it[Keys.APP_PAUSE_ENABLED] = value }
    }

    // ── NSFW Detection ──────────────────────────────────────────────

    val nsfwThreshold: Flow<Float> = dataStore.data.map { it[Keys.NSFW_THRESHOLD] ?: 0.70f }
    val nsfwScanIntervalMs: Flow<Long> = dataStore.data.map { it[Keys.NSFW_SCAN_INTERVAL_MS] ?: 2000L }
    val nsfwEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.NSFW_ENABLED] ?: false }

    suspend fun setNsfwThreshold(value: Float) {
        dataStore.edit { it[Keys.NSFW_THRESHOLD] = value.coerceIn(0.50f, 0.95f) }
    }

    suspend fun setNsfwScanIntervalMs(value: Long) {
        dataStore.edit { it[Keys.NSFW_SCAN_INTERVAL_MS] = value.coerceIn(1000L, 5000L) }
    }

    suspend fun setNsfwEnabled(value: Boolean) {
        dataStore.edit { it[Keys.NSFW_ENABLED] = value }
    }

    // ── Shorts Blocker ──────────────────────────────────────────────

    val shortsBlockerEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SHORTS_BLOCKER_ENABLED] ?: false }

    suspend fun setShortsBlockerEnabled(value: Boolean) {
        dataStore.edit { it[Keys.SHORTS_BLOCKER_ENABLED] = value }
    }

    val shortsBlockerApps: Flow<Set<String>> = dataStore.data.map {
        it[Keys.SHORTS_BLOCKER_APPS]?.split(",")?.filter { s -> s.isNotBlank() }?.toSet()
            ?: setOf(
                "com.google.android.youtube", "com.instagram.android", "com.snapchat.android",
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme",
            )
    }

    suspend fun setShortsBlockerApps(apps: Set<String>) {
        dataStore.edit { it[Keys.SHORTS_BLOCKER_APPS] = apps.joinToString(",") }
    }

    suspend fun getShortsBlockerAppsSnapshot(): Set<String> {
        return shortsBlockerApps.first()
    }

    // ── DNS Filter ──────────────────────────────────────────────────

    val dnsFilterEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.DNS_FILTER_ENABLED] ?: false }

    suspend fun setDnsFilterEnabled(value: Boolean) {
        dataStore.edit { it[Keys.DNS_FILTER_ENABLED] = value }
    }

    // ── VPN state ───────────────────────────────────────────────────

    val vpnEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.VPN_ENABLED] ?: false }

    suspend fun setVpnEnabled(value: Boolean) {
        dataStore.edit { it[Keys.VPN_ENABLED] = value }
    }

    // ── Wind-down Schedule ─────────────────────────────────────────────

    val windDownEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.WIND_DOWN_ENABLED] ?: false }
    val windDownStartHour: Flow<Int> = dataStore.data.map { it[Keys.WIND_DOWN_START_HOUR] ?: 22 }
    val windDownStartMin: Flow<Int> = dataStore.data.map { it[Keys.WIND_DOWN_START_MIN] ?: 0 }
    val windDownEndHour: Flow<Int> = dataStore.data.map { it[Keys.WIND_DOWN_END_HOUR] ?: 7 }
    val windDownEndMin: Flow<Int> = dataStore.data.map { it[Keys.WIND_DOWN_END_MIN] ?: 0 }

    suspend fun setWindDownEnabled(value: Boolean) {
        dataStore.edit { it[Keys.WIND_DOWN_ENABLED] = value }
    }

    suspend fun setWindDownStartHour(value: Int) {
        dataStore.edit { it[Keys.WIND_DOWN_START_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun setWindDownStartMin(value: Int) {
        dataStore.edit { it[Keys.WIND_DOWN_START_MIN] = value.coerceIn(0, 59) }
    }

    suspend fun setWindDownEndHour(value: Int) {
        dataStore.edit { it[Keys.WIND_DOWN_END_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun setWindDownEndMin(value: Int) {
        dataStore.edit { it[Keys.WIND_DOWN_END_MIN] = value.coerceIn(0, 59) }
    }

    // ── Breathing Pattern ──────────────────────────────────────────────

    val preferredBreathingPattern: Flow<String> = dataStore.data.map { it[Keys.PREFERRED_BREATHING_PATTERN] ?: "simple" }

    suspend fun setPreferredBreathingPattern(value: String) {
        val valid = value.takeIf { it in setOf("simple", "sigh", "box") } ?: "simple"
        dataStore.edit { it[Keys.PREFERRED_BREATHING_PATTERN] = valid }
    }

    // ── Intervention Technique ───────────────────────────────────────

    val preferredTechnique: Flow<String> = dataStore.data.map { it[Keys.PREFERRED_TECHNIQUE] ?: "sigh" }

    suspend fun setPreferredTechnique(technique: String) {
        val valid = technique.takeIf { it in setOf("simple_pause", "sigh", "defusion", "surfing") } ?: "sigh"
        dataStore.edit { it[Keys.PREFERRED_TECHNIQUE] = valid }
    }

    // ── NSFW Allow Cooldown ─────────────────────────────────────────

    val nsfwAllowCooldownMin: Flow<Int> = dataStore.data.map { it[Keys.NSFW_ALLOW_COOLDOWN_MIN] ?: 30 }

    suspend fun setNsfwAllowCooldownMin(value: Int) {
        dataStore.edit { it[Keys.NSFW_ALLOW_COOLDOWN_MIN] = value.coerceIn(5, 120) }
    }

    // ── Escalation Thresholds ────────────────────────────────────────

    val mediumThresholdMin: Flow<Int> = dataStore.data.map { it[Keys.MEDIUM_THRESHOLD_MIN] ?: 20 }
    val deepThresholdMin: Flow<Int> = dataStore.data.map { it[Keys.DEEP_THRESHOLD_MIN] ?: 60 }

    suspend fun setMediumThresholdMin(value: Int) {
        dataStore.edit { it[Keys.MEDIUM_THRESHOLD_MIN] = value.coerceIn(5, 120) }
    }

    suspend fun setDeepThresholdMin(value: Int) {
        dataStore.edit { it[Keys.DEEP_THRESHOLD_MIN] = value.coerceIn(15, 240) }
    }

    // ── Theme ────────────────────────────────────────────────────────

    val themeMode: Flow<String> = dataStore.data.map { it[Keys.THEME_MODE] ?: "auto" }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { it[Keys.THEME_MODE] = value }
    }
}
