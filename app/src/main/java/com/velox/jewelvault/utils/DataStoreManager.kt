package com.velox.jewelvault.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
        }
    }

    val userName: Flow<String> = dataStore.data.map { prefs -> prefs[USER_NAME_KEY] ?: "" }

    suspend fun <T> setValue(key:Preferences.Key<T>, value:T)  {
        dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    fun <T> getValue(key: Preferences.Key<T>, default: T?=null): Flow<T?> {
        return dataStore.data.map { prefs ->
            prefs[key] ?: default
        }
    }
}
