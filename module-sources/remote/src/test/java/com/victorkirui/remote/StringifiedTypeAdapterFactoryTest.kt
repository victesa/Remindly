package com.victorkirui.remote

import com.google.gson.GsonBuilder
import com.victorkirui.core.model.GetItemsResponse
import com.victorkirui.core.model.QuotaResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StringifiedTypeAdapterFactoryTest {

    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(StringifiedTypeAdapterFactory())
        .create()

    @Test
    fun testNormalJsonObject() {
        val json = """{"success":true,"userId":"user1","userTier":"premium","count":1,"items":[]}"""
        val response = gson.fromJson(json, GetItemsResponse::class.java)

        assertTrue(response.success)
        assertEquals("user1", response.userId)
        assertEquals("premium", response.userTier)
        assertEquals(0, response.items?.size)
    }

    @Test
    fun testDoubleStringifiedJsonObject() {
        val jsonString = """"{\"success\":true,\"userId\":\"user123\",\"userTier\":\"free\",\"count\":0,\"items\":[]}""""
        val response = gson.fromJson(jsonString, GetItemsResponse::class.java)

        assertNotNull(response)
        assertTrue(response.success)
        assertEquals("user123", response.userId)
        assertEquals("free", response.userTier)
    }

    @Test
    fun testStringifiedNestedDataField() {
        val json = """
            {
              "success": true,
              "userId": "user1",
              "userTier": "premium",
              "count": 1,
              "items": [
                {
                  "id": "item1",
                  "userId": "user1",
                  "data": "{\"title\":\"Buy Coffee\",\"category\":\"SHOPPING\",\"strategy\":\"BACKEND\",\"tier\":\"premium\"}"
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetItemsResponse::class.java)

        assertNotNull(response)
        assertEquals(1, response.items?.size)
        val item = response.items!![0]
        assertEquals("item1", item.id)
        assertNotNull(item.data)
        assertEquals("Buy Coffee", item.data?.title)
        assertEquals("SHOPPING", item.data?.category)
    }

    @Test
    fun testQuotaResponseStringified() {
        val jsonString = """"{\"success\":true,\"quota\":{\"tier\":\"premium\",\"limit\":100,\"remaining\":95,\"resetInSeconds\":3600}}""""
        val response = gson.fromJson(jsonString, QuotaResponse::class.java)

        assertNotNull(response)
        assertTrue(response.success)
        assertEquals("premium", response.quota?.tier)
        assertEquals(100, response.quota?.limit)
        assertEquals(95, response.quota?.remaining)
    }
}
