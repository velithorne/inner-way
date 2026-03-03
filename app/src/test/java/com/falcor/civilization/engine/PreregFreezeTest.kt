package com.falcor.civilization.engine

import com.falcor.civilization.engine.util.HashUtil
import org.junit.Assert.*
import org.junit.Test

class PreregFreezeTest {

    @Test
    fun planHashIsDeterministic() {
        val planJson = """{"metrics":["m1","m2"],"alpha":0.05}"""
        val hash1 = HashUtil.sha256(planJson)
        val hash2 = HashUtil.sha256(planJson)
        assertEquals(hash1, hash2)
    }

    @Test
    fun planHashChangesWithContent() {
        val plan1 = """{"metrics":["m1"]}"""
        val plan2 = """{"metrics":["m1","m2"]}"""
        assertNotEquals(HashUtil.sha256(plan1), HashUtil.sha256(plan2))
    }

    @Test
    fun sha256Produces64CharHex() {
        val hash = HashUtil.sha256("test")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
