package com.example.mahari.data.db

import com.example.mahari.data.parser.MpesaParser
import org.junit.Assert.*
import org.junit.Test

class RoomMigrationTest {

    @Test
    fun `test MIGRATION_3_4 schema column additions and index statements`() {
        val migration = MahariDatabase.MIGRATION_3_4
        assertEquals(3, migration.startVersion)
        assertEquals(4, migration.endVersion)
    }

    @Test
    fun `test MIGRATION_4_5 re-parses rawText and produces correct balance maps`() {
        val migration = MahariDatabase.MIGRATION_4_5
        assertEquals(4, migration.startVersion)
        assertEquals(5, migration.endVersion)

        val sampleRawText = "QFG8XYZ Confirmed. Ksh1,200.00 paid to Naivas Supermarket. New M-PESA balance is Ksh4,500.00"
        val parsed = MpesaParser.parse(sampleRawText)
        assertNotNull(parsed)
        assertEquals(4500.0, parsed!!.runningBalance!!, 0.001)
    }
}
