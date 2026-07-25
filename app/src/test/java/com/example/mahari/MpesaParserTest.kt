package com.example.mahari

import com.example.mahari.data.categorizer.Categorizer
import com.example.mahari.data.parser.MpesaParser
import com.example.mahari.data.parser.MpesaTransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MpesaParserTest {

    @Test
    fun testParsePaybillBuyGoods() {
        val sms = "QFG8XYZ Confirmed. Ksh350.00 paid to Java House. New M-PESA balance is Ksh1,200.45"
        val result = MpesaParser.parse(sms)

        assertNotNull(result)
        assertEquals("QFG8XYZ", result?.code)
        assertEquals(350.00, result?.amount ?: 0.0, 0.001)
        assertEquals("Java House", result?.merchantOrParty)
        assertEquals(MpesaTransactionType.PAYBILL_BUY_GOODS, result?.type)
        assertEquals(1200.45, result?.runningBalance ?: 0.0, 0.001)
        assertTrue(result?.isExpense == true)
    }

    @Test
    fun testDynamicCafeteriaCategorization() = runBlocking {
        val sms1 = "QFG8CAF Confirmed. Ksh150.00 paid to UoN Campus Cafeteria. New M-PESA balance is Ksh1,050.00"
        val result1 = MpesaParser.parse(sms1)
        assertNotNull(result1)

        val cat1 = Categorizer.categorize(result1!!.merchantOrParty, result1.type)
        assertEquals("Food & Dining", cat1)

        val sms2 = "QFG8CAN Confirmed. Ksh220.00 paid to Strathmore Main Canteen. New M-PESA balance is Ksh830.00"
        val result2 = MpesaParser.parse(sms2)
        assertNotNull(result2)

        val cat2 = Categorizer.categorize(result2!!.merchantOrParty, result2.type)
        assertEquals("Food & Dining", cat2)
    }

    @Test
    fun testDynamicTransportAndGroceriesCategorization() = runBlocking {
        val catBolt = Categorizer.categorize("Bolt Ride Nairobi", MpesaTransactionType.PAYBILL_BUY_GOODS)
        assertEquals("Transport", catBolt)

        val catMart = Categorizer.categorize("Karen Mini Market", MpesaTransactionType.PAYBILL_BUY_GOODS)
        assertEquals("Groceries", catMart)
    }
}
