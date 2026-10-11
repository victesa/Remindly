package com.victorkirui.core

import com.victorkirui.core.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DateUtilsTest {

    @Test
    fun testGetTimeAgo_currentUtcInstant_returnsJustNow() {
        val nowIso = Instant.now().toString()
        val result = DateUtils.getTimeAgo(nowIso)
        assertEquals("Just now", result)
    }

    @Test
    fun testGetTimeAgo_fiveMinutesAgoUtc_returnsFiveMinutesAgo() {
        val fiveMinsAgoIso = Instant.now().minusSeconds(300).toString()
        val result = DateUtils.getTimeAgo(fiveMinsAgoIso)
        assertEquals("5m ago", result)
    }

    @Test
    fun testGetTimeAgo_twoHoursAgoUtc_returnsTwoHoursAgo() {
        val twoHoursAgoIso = Instant.now().minusSeconds(7200).toString()
        val result = DateUtils.getTimeAgo(twoHoursAgoIso)
        assertEquals("2h ago", result)
    }
}
