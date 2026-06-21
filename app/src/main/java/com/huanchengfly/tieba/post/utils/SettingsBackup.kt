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
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Block
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

object SettingsBackup {
    private val gson = Gson()

    private data class ParsedBackup(
        val prefs: Map<String, Any?>,
        val blocks: List<Block>,
        val localFollowedForums: List<LocalForumManager.LocalForumItem>,
    )

    data class Backup(
        val prefs: Map<String, Any?> = emptyMap(),
        val blocks: List<Block> = emptyList(),
        val localFollowedForums: List<LocalForumManager.LocalForumItem> = emptyList(),
    )

    suspend fun export(context: Context): String {
        Test21Log.d(context, "export: start")
        val prefsSnapshot = context.dataStore.data.first()
        val prefs = prefsSnapshot.asJsonCompatMap()
        val blocks = withContext(Dispatchers.IO) {
            Test21Log.d(context, "export: loading blocks from DB")
            DatabaseUtil.getAllBlocks()
        }
        val localForums = LocalForumManager.getFollowedForums()

        Test21Log.d(
            context,
            "export: prefsKeys=${prefs.size} blocks=${blocks.size} localForums=${localForums.size}"
        )

        val out = gson.toJson(
            Backup(
                prefs = prefs,
                blocks = blocks,
                localFollowedForums = localForums,
            )
        )

        Test21Log.d(context, "export: jsonBytes=${out.toByteArray(Charsets.UTF_8).size}")
        Test21Log.d(context, "export: done")
        return out
    }

    suspend fun importAndOverwrite(context: Context, json: String) {
        Test21Log.d(context, "import: start jsonBytes=${json.toByteArray(Charsets.UTF_8).size}")
        val backup = runCatching { parse(json) }
            .onFailure { Test21Log.e(context, "import: parse failed", it) }
            .getOrNull() ?: throw IllegalArgumentException("invalid json")

        Test21Log.d(
            context,
            "import: parsed prefsKeys=${backup.prefs.size} blocks=${backup.blocks.size} localForums=${backup.localFollowedForums.size}"
        )

        // 1) Preferences (DataStore)
        Test21Log.d(context, "import: writing prefs")
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
        Test21Log.d(context, "import: prefs written")

        // 2) Block list (Room)
        Test21Log.d(context, "import: writing blocks")
        withContext(Dispatchers.IO) {
            try {
                DatabaseUtil.deleteAllBlocks()
                backup.blocks.forEachIndexed { index, block ->
                    DatabaseUtil.insertBlock(block.copy(id = 0L))
                    if (index % 50 == 0) {
                        Test21Log.d(context, "import: inserted blocks index=$index")
                    }
                }
                BlockManager.init()
            } catch (t: Throwable) {
                Test21Log.e(context, "import: blocks write failed", t)
                throw t
            }
        }
        Test21Log.d(context, "import: blocks written")

        // 3) Local followed forums (SharedPreferences)
        Test21Log.d(context, "import: writing local forums")
        runCatching {
            LocalForumManager.overwriteFollowedForums(backup.localFollowedForums)
        }.onFailure {
            Test21Log.e(context, "import: local forums write failed", it)
            throw it
        }
        Test21Log.d(context, "import: done")
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

    /**
     * Parse without relying on generic signatures (R8 may strip/obfuscate them in release).
     */
    private fun parse(json: String): ParsedBackup {
        val root = JsonParser.parseString(json).asJsonObject

        val prefsObj = root.getAsJsonObject("prefs") ?: JsonObject()
        val prefs = prefsObj.entrySet().associate { (k, v) ->
            k to jsonElementToAny(v)
        }

        val blocks = root.getAsJsonArray("blocks").decodeList(Block::class.java)
        val localForums = root.getAsJsonArray("localFollowedForums")
            .decodeList(LocalForumManager.LocalForumItem::class.java)

        return ParsedBackup(
            prefs = prefs,
            blocks = blocks,
            localFollowedForums = localForums,
        )
    }

    private fun JsonObject.getAsJsonObject(name: String): JsonObject? {
        val el = get(name) ?: return null
        return el.takeIf { it.isJsonObject }?.asJsonObject
    }

    private fun JsonObject.getAsJsonArray(name: String): JsonArray {
        val el = get(name)
        return el?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
    }

    private fun <T> JsonArray.decodeList(clazz: Class<T>): List<T> {
        if (size() == 0) return emptyList()
        val out = ArrayList<T>(size())
        for (el in this) {
            out.add(gson.fromJson(el, clazz))
        }
        return out
    }

    private fun jsonElementToAny(el: JsonElement?): Any? {
        if (el == null || el.isJsonNull) return null
        if (el.isJsonPrimitive) {
            val p = el.asJsonPrimitive
            return when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> p.asDouble
                p.isString -> p.asString
                else -> p.toString()
            }
        }
        // Keep non-primitives stable as json strings.
        return el.toString()
    }
}
