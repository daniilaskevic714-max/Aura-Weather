package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_weather_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, SavedLocation::class.java)
    private val locationListAdapter = moshi.adapter<List<SavedLocation>>(listType)

    companion object {
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        private val KEY_WIND_UNIT = stringPreferencesKey("wind_unit")
        private val KEY_PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        private val KEY_ACTIVE_NAME = stringPreferencesKey("active_name")
        private val KEY_ACTIVE_REGION = stringPreferencesKey("active_region")
        private val KEY_ACTIVE_COUNTRY = stringPreferencesKey("active_country")
        private val KEY_ACTIVE_LAT = doublePreferencesKey("active_lat")
        private val KEY_ACTIVE_LON = doublePreferencesKey("active_lon")
        private val KEY_SAVED_LOCATIONS = stringPreferencesKey("saved_locations_json")

        val DEFAULT_LOCATIONS = listOf(
            SavedLocation("Berlin", "Berlin", "Germany", 52.52, 13.41),
            SavedLocation("London", "England", "United Kingdom", 51.5074, -0.1278),
            SavedLocation("Tokyo", "Tokyo", "Japan", 35.6762, 139.6503),
            SavedLocation("New York", "New York", "United States", 40.7128, -74.0060)
        )
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            temperatureUnit = prefs[KEY_TEMP_UNIT]
                ?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() }
                ?: TemperatureUnit.CELSIUS,
            windSpeedUnit = prefs[KEY_WIND_UNIT]
                ?.let { runCatching { WindSpeedUnit.valueOf(it) }.getOrNull() }
                ?: WindSpeedUnit.KMH,
            pressureUnit = prefs[KEY_PRESSURE_UNIT]
                ?.let { runCatching { PressureUnit.valueOf(it) }.getOrNull() }
                ?: PressureUnit.HPA
        )
    }

    val activeLocationFlow: Flow<SavedLocation> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_ACTIVE_NAME]
        val lat = prefs[KEY_ACTIVE_LAT]
        val lon = prefs[KEY_ACTIVE_LON]
        if (!name.isNullOrBlank() && lat != null && lon != null) {
            SavedLocation(
                name = name,
                region = prefs[KEY_ACTIVE_REGION].orEmpty(),
                country = prefs[KEY_ACTIVE_COUNTRY].orEmpty(),
                latitude = lat,
                longitude = lon
            )
        } else {
            DEFAULT_LOCATIONS.first()
        }
    }

    val savedLocationsFlow: Flow<List<SavedLocation>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_LOCATIONS]
        if (raw.isNullOrBlank()) {
            DEFAULT_LOCATIONS
        } else {
            runCatching { locationListAdapter.fromJson(raw) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_LOCATIONS
        }
    }

    suspend fun setActiveLocation(location: SavedLocation) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_NAME] = location.name
            prefs[KEY_ACTIVE_REGION] = location.region
            prefs[KEY_ACTIVE_COUNTRY] = location.country
            prefs[KEY_ACTIVE_LAT] = location.latitude
            prefs[KEY_ACTIVE_LON] = location.longitude
        }
    }

    suspend fun addSavedLocation(location: SavedLocation) {
        context.dataStore.edit { prefs ->
            val current = parseSavedList(prefs[KEY_SAVED_LOCATIONS])
            if (current.none { it.matchesCoordinates(location.latitude, location.longitude) }) {
                val updated = listOf(location) + current
                prefs[KEY_SAVED_LOCATIONS] = locationListAdapter.toJson(updated.take(12))
            }
        }
    }

    suspend fun removeSavedLocation(location: SavedLocation) {
        context.dataStore.edit { prefs ->
            val current = parseSavedList(prefs[KEY_SAVED_LOCATIONS])
            val updated = current.filterNot { it.matchesCoordinates(location.latitude, location.longitude) }
            if (updated.isNotEmpty()) {
                prefs[KEY_SAVED_LOCATIONS] = locationListAdapter.toJson(updated)
            }
        }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { it[KEY_TEMP_UNIT] = unit.name }
    }

    suspend fun setWindSpeedUnit(unit: WindSpeedUnit) {
        context.dataStore.edit { it[KEY_WIND_UNIT] = unit.name }
    }

    suspend fun setPressureUnit(unit: PressureUnit) {
        context.dataStore.edit { it[KEY_PRESSURE_UNIT] = unit.name }
    }

    private fun parseSavedList(raw: String?): List<SavedLocation> {
        if (raw.isNullOrBlank()) return DEFAULT_LOCATIONS
        return runCatching { locationListAdapter.fromJson(raw) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_LOCATIONS
    }
}
