package com.example.mahari.data.parser

import java.util.Calendar
import java.util.regex.Pattern

object MpesaParser {

    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    fun extractPaybillTillDetails(smsBody: String): String? {
        if (smsBody.isBlank()) return null
        val clean = smsBody.trim()

        val accPattern = Pattern.compile("""for\s+account\s+([A-Za-z0-9\-_]+)""", Pattern.CASE_INSENSITIVE)
        val accMatcher = accPattern.matcher(clean)
        val accountNo = if (accMatcher.find()) accMatcher.group(1)?.trim() else null

        val numPattern = Pattern.compile("""(?:paid\s+to|sent\s+to|from)\s+(?:PAYBILL|TILL)?\s*(\d{4,8})""", Pattern.CASE_INSENSITIVE)
        val numMatcher = numPattern.matcher(clean)
        val numberStr = if (numMatcher.find()) numMatcher.group(1)?.trim() else null

        return when {
            numberStr != null && accountNo != null -> "Paybill: $numberStr • Acc: $accountNo"
            accountNo != null -> "Acc Reference: $accountNo"
            numberStr != null -> "Till / Paybill: $numberStr"
            else -> null
        }
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
                    val p0 = dateParts[0].toInt()
                    val p1 = dateParts[1].toInt()
                    var year = dateParts[2].toInt()
                    if (year < 100) year += 2000

                    var day: Int
                    var month: Int

                    if (p0 > 12) {
                        day = p0
                        month = p1 - 1
                    } else if (p1 > 12) {
                        month = p0 - 1
                        day = p1
                    } else {
                        day = p0
                        month = p1 - 1
                    }

                    month = month.coerceIn(0, 11)
                    day = day.coerceIn(1, 31)

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

        // 1. Paybill & Buy Goods (Till):
        val paybillPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\s+(?:paid|sent)\s+to\s+(.+?)(?:\s+for\s+account\s+(.+?))?(?:\s+on\s+.+?)?\.\s*(?:New\s+M-PESA\s+balance\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2}))?""",
            Pattern.CASE_INSENSITIVE
        )
        val paybillMatcher = paybillPattern.matcher(cleanSms)
        if (paybillMatcher.find()) {
            val code = paybillMatcher.group(1) ?: ""
            val amount = parseAmount(paybillMatcher.group(2) ?: "0")
            var merchant = paybillMatcher.group(3)?.trim() ?: "Merchant"
            val balanceStr = paybillMatcher.group(5)
            val balance = if (balanceStr != null) parseAmount(balanceStr) else 0.0

            if (merchant.contains(" on ", ignoreCase = true)) {
                merchant = merchant.substringBefore(" on ", merchant).trim()
            }

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

        // 2. Send Money to Person:
        val sendPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\s+sent\s+to\s+(.+?)(?:\s+on\s+.+?)?\.\s*(?:New\s+M-PESA\s+balance\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2}))?""",
            Pattern.CASE_INSENSITIVE
        )
        val sendMatcher = sendPattern.matcher(cleanSms)
        if (sendMatcher.find()) {
            val code = sendMatcher.group(1) ?: ""
            val amount = parseAmount(sendMatcher.group(2) ?: "0")
            var recipient = sendMatcher.group(3)?.trim() ?: "Recipient"
            val balanceStr = sendMatcher.group(4)
            val balance = if (balanceStr != null) parseAmount(balanceStr) else 0.0

            if (recipient.contains(" on ", ignoreCase = true)) {
                recipient = recipient.substringBefore(" on ", recipient).trim()
            }

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

        // 3. Receive Money:
        val receivePattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+You\s+have\s+received\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\s+from\s+(.+?)(?:\s+on\s+.+?)?\.\s*(?:New\s+M-PESA\s+balance\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2}))?""",
            Pattern.CASE_INSENSITIVE
        )
        val receiveMatcher = receivePattern.matcher(cleanSms)
        if (receiveMatcher.find()) {
            val code = receiveMatcher.group(1) ?: ""
            val amount = parseAmount(receiveMatcher.group(2) ?: "0")
            var sender = receiveMatcher.group(3)?.trim() ?: "Sender"
            val balanceStr = receiveMatcher.group(4)
            val balance = if (balanceStr != null) parseAmount(balanceStr) else 0.0

            if (sender.contains(" on ", ignoreCase = true)) {
                sender = sender.substringBefore(" on ", sender).trim()
            }

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

        // 4. Withdraw:
        val withdrawPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\s+withdrawn\s+from\s+(.+?)(?:\s+on\s+.+?)?\.\s*(?:New\s+M-PESA\s+balance\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2}))?""",
            Pattern.CASE_INSENSITIVE
        )
        val withdrawMatcher = withdrawPattern.matcher(cleanSms)
        if (withdrawMatcher.find()) {
            val code = withdrawMatcher.group(1) ?: ""
            val amount = parseAmount(withdrawMatcher.group(2) ?: "0")
            val agent = withdrawMatcher.group(3)?.trim() ?: "Agent"
            val balanceStr = withdrawMatcher.group(4)
            val balance = if (balanceStr != null) parseAmount(balanceStr) else 0.0

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

        // 5. Deposit:
        val depositPattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+Give\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\s+to\s+(.+?)\s+for\s+deposit(?:\s+on\s+.+?)?\.\s*(?:New\s+M-PESA\s+balance\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2}))?""",
            Pattern.CASE_INSENSITIVE
        )
        val depositMatcher = depositPattern.matcher(cleanSms)
        if (depositMatcher.find()) {
            val code = depositMatcher.group(1) ?: ""
            val amount = parseAmount(depositMatcher.group(2) ?: "0")
            val agent = depositMatcher.group(3)?.trim() ?: "Agent Deposit"
            val balanceStr = depositMatcher.group(4)
            val balance = if (balanceStr != null) parseAmount(balanceStr) else 0.0

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

        // 6. Airtime:
        val airtimePattern = Pattern.compile(
            """^([A-Z0-9]+)\s+Confirmed\.\s+You\s+bought\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\s+of\s+airtime(?:\s+on\s+.+?)?\.\s*(?:New\s+M-PESA\s+balance\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2}))?""",
            Pattern.CASE_INSENSITIVE
        )
        val airtimeMatcher = airtimePattern.matcher(cleanSms)
        if (airtimeMatcher.find()) {
            val code = airtimeMatcher.group(1) ?: ""
            val amount = parseAmount(airtimeMatcher.group(2) ?: "0")
            val balanceStr = airtimeMatcher.group(3)
            val balance = if (balanceStr != null) parseAmount(balanceStr) else 0.0

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

        // 7. Fuliza Overdraft:
        val fulizaPattern = Pattern.compile(
            """Fuliza\s+M-PESA\s+amount\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})\.\s+Outstanding\s+Fuliza\s+M-PESA\s+amount\s+is\s+(?:Ksh|KES)\s*([\d,]+\.\d{2})""",
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
