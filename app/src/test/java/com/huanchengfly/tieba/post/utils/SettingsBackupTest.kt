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
    }
}
