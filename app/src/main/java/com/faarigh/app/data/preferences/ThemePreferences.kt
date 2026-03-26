package com.faarigh.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

enum class DarkModePreference { AUTO, DARK, LIGHT }

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
    }

    val darkMode: Flow<DarkModePreference> = context.themeDataStore.data.map { prefs ->
        val name = prefs[KEY_DARK_MODE]
        DarkModePreference.entries.find { it.name == name } ?: DarkModePreference.AUTO
    }

    suspend fun setDarkMode(mode: DarkModePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = mode.name
        }
    }
}
