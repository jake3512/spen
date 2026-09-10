package com.jake.spenhotkey.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "spen_hotkey_prefs")

/**
 * Stores the default (global) button mapping plus optional per-app overrides.
 * An app only has its own [Mapping] once the user explicitly customizes it
 * (tracked in [overrideAppsFlow]); until then it inherits the default mapping.
 */
class MappingRepository(private val context: Context) {

    private val dataStore get() = context.dataStore

    private val overrideAppsKey = stringSetPreferencesKey("override_apps")
    private val longPressMsKey = longPreferencesKey("long_press_ms")
    private val doubleClickMsKey = longPreferencesKey("double_click_ms")

    private fun key(scope: String, gesture: String) = stringPreferencesKey("$scope.$gesture")

    private fun mappingFromPrefs(
        prefs: Preferences,
        scope: String,
        resolveAppLabel: (String) -> String
    ): Mapping {
        val defaults = if (scope == DEFAULT_SCOPE) Mapping() else Mapping(ActionSpec.None, ActionSpec.None, ActionSpec.None)
        return Mapping(
            singleClick = prefs[key(scope, "single")]?.let { ActionSpec.fromCode(it, resolveAppLabel) }
                ?: defaults.singleClick,
            doubleClick = prefs[key(scope, "double")]?.let { ActionSpec.fromCode(it, resolveAppLabel) }
                ?: defaults.doubleClick,
            longPress = prefs[key(scope, "long")]?.let { ActionSpec.fromCode(it, resolveAppLabel) }
                ?: defaults.longPress
        )
    }

    fun defaultMappingFlow(resolveAppLabel: (String) -> String): Flow<Mapping> =
        dataStore.data.map { mappingFromPrefs(it, DEFAULT_SCOPE, resolveAppLabel) }

    fun appMappingFlow(packageName: String, resolveAppLabel: (String) -> String): Flow<Mapping> =
        dataStore.data.map { mappingFromPrefs(it, packageName, resolveAppLabel) }

    val overrideAppsFlow: Flow<Set<String>> = dataStore.data.map { it[overrideAppsKey] ?: emptySet() }

    /** The mapping that actually applies to [packageName] right now: its own override, or the default. */
    suspend fun effectiveMappingForPackage(packageName: String, resolveAppLabel: (String) -> String): Mapping {
        val prefs = dataStore.data.first()
        val overrides = prefs[overrideAppsKey] ?: emptySet()
        val scope = if (packageName in overrides) packageName else DEFAULT_SCOPE
        return mappingFromPrefs(prefs, scope, resolveAppLabel)
    }

    suspend fun saveDefaultMapping(mapping: Mapping) = saveMapping(DEFAULT_SCOPE, mapping)

    suspend fun saveAppMapping(packageName: String, mapping: Mapping) {
        saveMapping(packageName, mapping)
        dataStore.edit { prefs ->
            val current = prefs[overrideAppsKey] ?: emptySet()
            prefs[overrideAppsKey] = current + packageName
        }
    }

    suspend fun clearAppOverride(packageName: String) {
        dataStore.edit { prefs ->
            prefs.remove(key(packageName, "single"))
            prefs.remove(key(packageName, "double"))
            prefs.remove(key(packageName, "long"))
            val current = prefs[overrideAppsKey] ?: emptySet()
            prefs[overrideAppsKey] = current - packageName
        }
    }

    private suspend fun saveMapping(scope: String, mapping: Mapping) {
        dataStore.edit { prefs ->
            prefs[key(scope, "single")] = mapping.singleClick.code
            prefs[key(scope, "double")] = mapping.doubleClick.code
            prefs[key(scope, "long")] = mapping.longPress.code
        }
    }

    val longPressMsFlow: Flow<Long> = dataStore.data.map { it[longPressMsKey] ?: DEFAULT_LONG_PRESS_MS }
    val doubleClickMsFlow: Flow<Long> = dataStore.data.map { it[doubleClickMsKey] ?: DEFAULT_DOUBLE_CLICK_MS }

    suspend fun setTimings(longPressMs: Long, doubleClickMs: Long) {
        dataStore.edit { prefs ->
            prefs[longPressMsKey] = longPressMs
            prefs[doubleClickMsKey] = doubleClickMs
        }
    }

    companion object {
        private const val DEFAULT_SCOPE = "__default__"
        const val DEFAULT_LONG_PRESS_MS = 500L
        const val DEFAULT_DOUBLE_CLICK_MS = 300L
    }
}
