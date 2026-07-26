package com.example.mahari.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MpesaDateParserTest {

    @Test
    fun testExplicitDayFirst_25_7_26() {
        val sms = "QK1234567 Confirmed. Ksh1,500.00 paid to KPLC PREPAID for account 84930291039 on 25/7/26 at 2:34 PM."
        val ts = MpesaParser.extractTransactionTimestamp(sms, 0L)

        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, cal.get(Calendar.MONTH))
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(34, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testExplicitMonthFirst_7_26_26() {
        val sms = "QK8901234 Confirmed. Ksh450.00 paid to JAVA HOUSE KILIMANI on 7/26/26 at 10:15 AM."
        val ts = MpesaParser.extractTransactionTimestamp(sms, 0L)

        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, cal.get(Calendar.MONTH))
        assertEquals(26, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testAmbiguousSafaricomStandard_7_8_26() {
        // Safaricom English standard is DD/MM/YY (7th August 2026)
        val sms = "QK9990001 Confirmed. Ksh1,000.00 sent to JOHN DOE on 7/8/26 at 1:00 PM."
        val ts = MpesaParser.extractTransactionTimestamp(sms, 0L)

        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, cal.get(Calendar.MONTH))
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAmbiguousSafaricomStandard_12_1_26() {
        // 12th January 2026
        val sms = "QK1112223 Confirmed. You bought Ksh100.00 of airtime on 12/1/26 at 9:00 AM."
        val ts = MpesaParser.extractTransactionTimestamp(sms, 0L)

        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(12, cal.get(Calendar.DAY_OF_MONTH))
    }
}
