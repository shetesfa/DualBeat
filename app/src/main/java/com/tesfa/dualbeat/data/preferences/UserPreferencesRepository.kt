package com.tesfa.dualbeat.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.tesfa.dualbeat.core.theme.AccentChoice
import com.tesfa.dualbeat.core.theme.ThemeMode
import com.tesfa.dualbeat.domain.model.SortDirection
import com.tesfa.dualbeat.domain.model.SortField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dualbeat_preferences")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val accentChoice: AccentChoice = AccentChoice.NEON_CYAN,
    val isResumePlaybackEnabled: Boolean = true,
    val isAutoPlayEnabled: Boolean = false,
    val isBackgroundAudioEnabled: Boolean = true,
    val areGesturesEnabled: Boolean = true,
    val defaultVideoSpeed: Float = 1.0f,
    val defaultSortField: SortField = SortField.DATE_ADDED,
    val defaultSortDirection: SortDirection = SortDirection.DESCENDING,
    val lastOpenedTab: String = "home"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_CHOICE = stringPreferencesKey("accent_choice")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val BACKGROUND_AUDIO = booleanPreferencesKey("background_audio")
        val GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        val DEFAULT_VIDEO_SPEED = floatPreferencesKey("default_video_speed")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val LAST_OPENED_TAB = stringPreferencesKey("last_opened_tab")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.AMOLED.name
            val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.AMOLED)

            val accentStr = preferences[PreferencesKeys.ACCENT_CHOICE] ?: AccentChoice.NEON_CYAN.name
            val accentChoice = runCatching { AccentChoice.valueOf(accentStr) }.getOrDefault(AccentChoice.NEON_CYAN)

            val resumePlayback = preferences[PreferencesKeys.RESUME_PLAYBACK] ?: true
            val autoPlay = preferences[PreferencesKeys.AUTO_PLAY] ?: false
            val backgroundAudio = preferences[PreferencesKeys.BACKGROUND_AUDIO] ?: true
            val gesturesEnabled = preferences[PreferencesKeys.GESTURES_ENABLED] ?: true
            val videoSpeed = preferences[PreferencesKeys.DEFAULT_VIDEO_SPEED] ?: 1.0f

            val sortFieldStr = preferences[PreferencesKeys.SORT_FIELD] ?: SortField.DATE_ADDED.name
            val sortField = runCatching { SortField.valueOf(sortFieldStr) }.getOrDefault(SortField.DATE_ADDED)

            val sortDirStr = preferences[PreferencesKeys.SORT_DIRECTION] ?: SortDirection.DESCENDING.name
            val sortDir = runCatching { SortDirection.valueOf(sortDirStr) }.getOrDefault(SortDirection.DESCENDING)

            val lastTab = preferences[PreferencesKeys.LAST_OPENED_TAB] ?: "home"

            UserPreferences(
                themeMode = themeMode,
                accentChoice = accentChoice,
                isResumePlaybackEnabled = resumePlayback,
                isAutoPlayEnabled = autoPlay,
                isBackgroundAudioEnabled = backgroundAudio,
                areGesturesEnabled = gesturesEnabled,
                defaultVideoSpeed = videoSpeed,
                defaultSortField = sortField,
                defaultSortDirection = sortDir,
                lastOpenedTab = lastTab
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentChoice(accent: AccentChoice) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_CHOICE] = accent.name }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_PLAY] = enabled }
    }

    suspend fun setBackgroundAudio(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BACKGROUND_AUDIO] = enabled }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.GESTURES_ENABLED] = enabled }
    }

    suspend fun setDefaultVideoSpeed(speed: Float) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_VIDEO_SPEED] = speed }
    }

    suspend fun setSortPreferences(field: SortField, direction: SortDirection) {
        context.dataStore.edit {
            it[PreferencesKeys.SORT_FIELD] = field.name
            it[PreferencesKeys.SORT_DIRECTION] = direction.name
        }
    }

    suspend fun setLastOpenedTab(tab: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_OPENED_TAB] = tab }
    }
}
