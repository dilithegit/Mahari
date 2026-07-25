package com.example.mahari.data.parser

import java.util.Calendar
import java.util.regex.Pattern

object MpesaParser {

    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    fun extractTransactionTimestamp(smsBody: String, fallbackDate: Long): Long {
        try {
            val datePattern = Pattern.compile(
                """on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+(?:at\s+)?(\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?)""",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = datePattern.matcher(smsBody)
            if (matcher.find()) {
                val dateStr = matcher.group(1) ?: ""
                val timeStr = matcher.group(2) ?: ""

                val dateParts = dateStr.split("/")
                if (dateParts.size == 3) {
                    val day = dateParts[0].toInt()
                    val month = dateParts[1].toInt() - 1
                    var year = dateParts[2].toInt()
                    if (year < 100) year += 2000

                    var hour = 0
                    var minute = 0
                    val cleanTime = timeStr.trim().uppercase()
                    val isPm = cleanTime.endsWith("PM")
                    val isAm = cleanTime.endsWith("AM")
                    val rawTime = cleanTime.replace("AM", "").replace("PM", "").trim()
                    val timeParts = rawTime.split(":")
                    if (timeParts.isNotEmpty()) hour = timeParts[0].toInt()
                    if (timeParts.size >= 2) minute = timeParts[1].toInt()

                    if (isPm && hour < 12) hour += 12
                    if (isAm && hour == 12) hour = 0

                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return cal.timeInMillis
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (fallbackDate > 0) fallbackDate else System.currentTimeMillis()
    }

    fun parse(smsBody: String): ParsedMpesaTransaction? {
        val cleanSms = smsBody.trim()

        // 1. Paybill / Buy Goods: QFG8XYZ Confirmed. Ksh350.00 paid to Java House. New M-PESA balance is Ksh1,200.45
        val paybillPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+Ksh([\d,]+\.\d{2})\s+paid\s+to\s+(.+?)\.\s+New\s+M-PESA\s+balance\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val paybillMatcher = paybillPattern.matcher(cleanSms)
        if (paybillMatcher.find()) {
            val code = paybillMatcher.group(1) ?: ""
            val amount = parseAmount(paybillMatcher.group(2) ?: "0")
            val merchant = paybillMatcher.group(3)?.trim() ?: "Unknown Merchant"
            val balance = parseAmount(paybillMatcher.group(4) ?: "0")

            return ParsedMpesaTransaction(
                code = code,
                amount = amount,
                merchantOrParty = merchant,
                type = MpesaTransactionType.PAYBILL_BUY_GOODS,
                runningBalance = balance,
                rawText = cleanSms,
                isExpense = true
            )
        }

        // 2. Send Money: QFG8XYZ Confirmed. Ksh350.00 sent to John Doe 0712345678 on 25/7/26 at 12:45 PM. New M-PESA balance is Ksh1,200.45
        val sendPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+Ksh([\d,]+\.\d{2})\s+sent\s+to\s+(.+?)(?:\s+on\s+.+?)?\.\s+New\s+M-PESA\s+balance\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val sendMatcher = sendPattern.matcher(cleanSms)
        if (sendMatcher.find()) {
            val code = sendMatcher.group(1) ?: ""
            val amount = parseAmount(sendMatcher.group(2) ?: "0")
            val recipient = sendMatcher.group(3)?.trim() ?: "Recipient"
            val balance = parseAmount(sendMatcher.group(4) ?: "0")

            return ParsedMpesaTransaction(
                code = code,
                amount = amount,
                merchantOrParty = recipient,
                type = MpesaTransactionType.SEND_MONEY,
                runningBalance = balance,
                rawText = cleanSms,
                isExpense = true
            )
        }

        // 3. Receive Money: QFG8XYZ Confirmed. You have received Ksh1,500.00 from Jane Smith on 25/7/26 at 9:00 AM. New M-PESA balance is Ksh5,200.45
        val receivePattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+You\s+have\s+received\s+Ksh([\d,]+\.\d{2})\s+from\s+(.+?)(?:\s+on\s+.+?)?\.\s+New\s+M-PESA\s+balance\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val receiveMatcher = receivePattern.matcher(cleanSms)
        if (receiveMatcher.find()) {
            val code = receiveMatcher.group(1) ?: ""
            val amount = parseAmount(receiveMatcher.group(2) ?: "0")
            val sender = receiveMatcher.group(3)?.trim() ?: "Sender"
            val balance = parseAmount(receiveMatcher.group(4) ?: "0")

            return ParsedMpesaTransaction(
                code = code,
                amount = amount,
                merchantOrParty = sender,
                type = MpesaTransactionType.RECEIVE_MONEY,
                runningBalance = balance,
                rawText = cleanSms,
                isExpense = false
            )
        }

        // 4. Withdraw: QFG8XYZ Confirmed. Ksh2,000.00 withdrawn from 12345 - Agent Name. New M-PESA balance is Ksh1,200.00
        val withdrawPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+Ksh([\d,]+\.\d{2})\s+withdrawn\s+from\s+(.+?)\.\s+New\s+M-PESA\s+balance\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val withdrawMatcher = withdrawPattern.matcher(cleanSms)
        if (withdrawMatcher.find()) {
            val code = withdrawMatcher.group(1) ?: ""
            val amount = parseAmount(withdrawMatcher.group(2) ?: "0")
            val agent = withdrawMatcher.group(3)?.trim() ?: "Agent"
            val balance = parseAmount(withdrawMatcher.group(4) ?: "0")

            return ParsedMpesaTransaction(
                code = code,
                amount = amount,
                merchantOrParty = agent,
                type = MpesaTransactionType.WITHDRAW,
                runningBalance = balance,
                rawText = cleanSms,
                isExpense = true
            )
        }

        // 5. Deposit: QFG8XYZ Confirmed. Give Ksh5,000.00 to 12345 - Agent Name for deposit. New M-PESA balance is Ksh7,200.00
        val depositPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+Give\s+Ksh([\d,]+\.\d{2})\s+to\s+(.+?)\s+for\s+deposit\.\s+New\s+M-PESA\s+balance\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val depositMatcher = depositPattern.matcher(cleanSms)
        if (depositMatcher.find()) {
            val code = depositMatcher.group(1) ?: ""
            val amount = parseAmount(depositMatcher.group(2) ?: "0")
            val agent = depositMatcher.group(3)?.trim() ?: "Agent Deposit"
            val balance = parseAmount(depositMatcher.group(4) ?: "0")

            return ParsedMpesaTransaction(
                code = code,
                amount = amount,
                merchantOrParty = agent,
                type = MpesaTransactionType.DEPOSIT,
                runningBalance = balance,
                rawText = cleanSms,
                isExpense = false
            )
        }

        // 6. Airtime: QFG8XYZ Confirmed. You bought Ksh100.00 of airtime on 25/7/26 at 10:00 AM. New M-PESA balance is Ksh1,100.00
        val airtimePattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+You\s+bought\s+Ksh([\d,]+\.\d{2})\s+of\s+airtime(?:\s+on\s+.+?)?\.\s+New\s+M-PESA\s+balance\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val airtimeMatcher = airtimePattern.matcher(cleanSms)
        if (airtimeMatcher.find()) {
            val code = airtimeMatcher.group(1) ?: ""
            val amount = parseAmount(airtimeMatcher.group(2) ?: "0")
            val balance = parseAmount(airtimeMatcher.group(3) ?: "0")

            return ParsedMpesaTransaction(
                code = code,
                amount = amount,
                merchantOrParty = "Safaricom Airtime",
                type = MpesaTransactionType.AIRTIME,
                runningBalance = balance,
                rawText = cleanSms,
                isExpense = true
            )
        }

        // 7. Fuliza Overdraft: Fuliza M-PESA amount is Ksh500.00. Outstanding Fuliza M-PESA amount is Ksh500.00
        val fulizaPattern = Pattern.compile(
            """Fuliza\s+M-PESA\s+amount\s+is\s+Ksh([\d,]+\.\d{2})\.\s+Outstanding\s+Fuliza\s+M-PESA\s+amount\s+is\s+Ksh([\d,]+\.\d{2})""",
            Pattern.CASE_INSENSITIVE
        )
        val fulizaMatcher = fulizaPattern.matcher(cleanSms)
        if (fulizaMatcher.find()) {
            val amount = parseAmount(fulizaMatcher.group(1) ?: "0")
            val outstanding = parseAmount(fulizaMatcher.group(2) ?: "0")

            return ParsedMpesaTransaction(
                code = "FULIZA_" + System.currentTimeMillis(),
                amount = amount,
                merchantOrParty = "Fuliza Overdraft",
                type = MpesaTransactionType.FULIZA_DEBT,
                runningBalance = 0.0,
                rawText = cleanSms,
                isExpense = true,
                fulizaOutstanding = outstanding
            )
        }

        return null
    }
}
