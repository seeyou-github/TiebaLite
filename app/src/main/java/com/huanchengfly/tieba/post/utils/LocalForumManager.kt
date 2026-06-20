package com.huanchengfly.tieba.post.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.App

object LocalForumManager {
    private const val PREFS_NAME = "local_followed_forums"
    private const val KEY_FORUMS = "followed_list"

    data class LocalForumItem(
        val forumName: String,
        val avatar: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    private val gson = Gson()

    private fun getPrefs(): android.content.SharedPreferences {
        return App.INSTANCE.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFollowedForums(): List<LocalForumItem> {
        val json = getPrefs().getString(KEY_FORUMS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalForumItem>>() {}.type
            gson.fromJson<List<LocalForumItem>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isFollowed(forumName: String): Boolean {
        return getFollowedForums().any { it.forumName.equals(forumName, ignoreCase = true) }
    }

    fun follow(forumName: String, avatar: String) {
        if (isFollowed(forumName)) return
        val list = getFollowedForums().toMutableList()
        list.add(LocalForumItem(forumName = forumName, avatar = avatar))
        saveList(list)
    }

    fun unfollow(forumName: String) {
        val list = getFollowedForums().toMutableList()
        list.removeAll { it.forumName.equals(forumName, ignoreCase = true) }
        saveList(list)
    }

    private fun saveList(list: List<LocalForumItem>) {
        val json = gson.toJson(list)
        getPrefs().edit().putString(KEY_FORUMS, json).apply()
    }
}
