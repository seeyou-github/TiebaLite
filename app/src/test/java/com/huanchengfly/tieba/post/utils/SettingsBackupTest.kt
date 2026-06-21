package com.huanchengfly.tieba.post.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class SettingsBackupTest {

    @Test
    fun testParseReflection() {
        // Since SettingsBackup.parse is private and relies on Gson, we can test it using reflection to make sure it runs correctly
        // and doesn't crash with NullPointerException when blocks/localFollowedForums are missing or present.
        val backupClass = SettingsBackup::class.java
        val parseMethod: Method = backupClass.getDeclaredMethod("parse", String::class.java)
        parseMethod.isAccessible = true

        // 1. JSON without blocks and localFollowedForums (simulating configuration containing block words / missing fields)
        val jsonNoBlocks = """
            {
                "prefs": {
                    "some_key": "some_value"
                }
            }
        """.trimIndent()

        val parsedBackup1 = parseMethod.invoke(SettingsBackup, jsonNoBlocks)
        assertNotNull(parsedBackup1)

        // Validate the structure of ParsedBackup via reflection
        val parsedBackupClass = parsedBackup1.javaClass
        val prefsField = parsedBackupClass.getDeclaredField("prefs")
        prefsField.isAccessible = true
        val prefs = prefsField.get(parsedBackup1) as Map<*, *>
        assertEquals("some_value", prefs["some_key"])

        val blocksField = parsedBackupClass.getDeclaredField("blocks")
        blocksField.isAccessible = true
        val blocks = blocksField.get(parsedBackup1) as List<*>
        assertTrue(blocks.isEmpty())

        val localFollowedForumsField = parsedBackupClass.getDeclaredField("localFollowedForums")
        localFollowedForumsField.isAccessible = true
        val localFollowedForums = localFollowedForumsField.get(parsedBackup1) as List<*>
        assertTrue(localFollowedForums.isEmpty())

        // 2. JSON with empty blocks list and empty localFollowedForums
        val jsonEmptyLists = """
            {
                "prefs": {
                    "another_key": true
                },
                "blocks": [],
                "localFollowedForums": []
            }
        """.trimIndent()

        val parsedBackup2 = parseMethod.invoke(SettingsBackup, jsonEmptyLists)
        assertNotNull(parsedBackup2)
        val prefs2 = prefsField.get(parsedBackup2) as Map<*, *>
        assertEquals(true, prefs2["another_key"])
        
        val blocks2 = blocksField.get(parsedBackup2) as List<*>
        assertTrue(blocks2.isEmpty())

        // 3. Test old compressed format with "a", "b", "c" as keys
        val jsonOldCompressed = """
            {
                "a": {
                    "hideExplore": false,
                    "defaultStart": 0
                },
                "b": [
                    {
                        "category": 10,
                        "id": 1,
                        "isRegex": false,
                        "keywords": "[\"xcvfvf\"]",
                        "type": 0
                    }
                ],
                "c": [
                    {
                        "a": "地方",
                        "b": "https://tiebapic.baidu.com/avatar.jpg",
                        "c": 1782044583323
                    }
                ]
            }
        """.trimIndent()

        val parsedBackup3 = parseMethod.invoke(SettingsBackup, jsonOldCompressed)
        assertNotNull(parsedBackup3)

        val prefs3 = prefsField.get(parsedBackup3) as Map<*, *>
        assertEquals(false, prefs3["hideExplore"])
        assertEquals(0.0, prefs3["defaultStart"]) // parsed as Double via Gson element mapping

        val blocks3 = blocksField.get(parsedBackup3) as List<*>
        assertEquals(1, blocks3.size)
        val blockObj = blocks3[0] as com.huanchengfly.tieba.post.models.database.Block
        assertEquals(10, blockObj.category)
        assertEquals("[\"xcvfvf\"]", blockObj.keywords)

        val localFollowedForums3 = localFollowedForumsField.get(parsedBackup3) as List<*>
        assertEquals(1, localFollowedForums3.size)
        val forumItem = localFollowedForums3[0] as LocalForumManager.LocalForumItem
        assertEquals("地方", forumItem.forumName)
        assertEquals("https://tiebapic.baidu.com/avatar.jpg", forumItem.avatar)
        assertEquals(1782044583323L, forumItem.timestamp)
    }
}
