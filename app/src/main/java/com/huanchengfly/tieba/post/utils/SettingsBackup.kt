package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Block
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

object SettingsBackup {
    private val gson = Gson()

    data class Backup(
        val prefs: Map<String, Any?> = emptyMap(),
        val blocks: List<Block> = emptyList(),
        val localFollowedForums: List<LocalForumManager.LocalForumItem> = emptyList(),
    )

    suspend fun export(context: Context): String {
        val prefsSnapshot = context.dataStore.data.first()
        val prefs = prefsSnapshot.asJsonCompatMap()
        val blocks = withContext(Dispatchers.IO) { DatabaseUtil.getAllBlocks() }
        val localForums = LocalForumManager.getFollowedForums()

        return gson.toJson(
            Backup(
                prefs = prefs,
                blocks = blocks,
                localFollowedForums = localForums,
            )
        )
    }

    suspend fun importAndOverwrite(context: Context, json: String) {
        val type = object : TypeToken<Backup>() {}.type
        val backup = gson.fromJson<Backup>(json, type) ?: return

        // 1) Preferences (DataStore)
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs.clear()
            backup.prefs.forEach { (key, value) ->
                when (value) {
                    null -> Unit
                    is Boolean -> mutablePrefs[booleanPreferencesKey(key)] = value
                    is Double -> {
                        // Gson decodes all JSON numbers as Double.
                        // Persist as Int/Long/Float when possible, otherwise String.
                        val asLong = value.toLong()
                        when {
                            value % 1.0 == 0.0 && asLong in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                                mutablePrefs[intPreferencesKey(key)] = asLong.toInt()

                            value % 1.0 == 0.0 ->
                                mutablePrefs[longPreferencesKey(key)] = asLong

                            else ->
                                mutablePrefs[floatPreferencesKey(key)] = value.toFloat()
                        }
                    }
                    is String -> mutablePrefs[stringPreferencesKey(key)] = value
                    else -> mutablePrefs[stringPreferencesKey(key)] = gson.toJson(value)
                }
            }
        }

        // 2) Block list (Room)
        withContext(Dispatchers.IO) {
            DatabaseUtil.deleteAllBlocks()
            backup.blocks.forEach { block ->
                // Keep ids stable in export, but Room insert might ignore it; ok.
                DatabaseUtil.insertBlock(block)
            }
            BlockManager.init()
        }

        // 3) Local followed forums (SharedPreferences)
        LocalForumManager.overwriteFollowedForums(backup.localFollowedForums)
    }

    private fun Preferences.asJsonCompatMap(): Map<String, Any?> {
        return asMap().entries.associate { (k, v) ->
            k.name to when (v) {
                is String,
                is Boolean,
                is Int,
                is Long,
                is Float,
                is Double -> v
                else -> v?.toString()
            }
        }
    }
}
