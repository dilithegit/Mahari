package com.example.mahari.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class DateScopeMode {
    abstract val label: String
    abstract val startTimestamp: Long
    abstract val endTimestamp: Long

    data class MonthMode(val year: Int, val month: Int) : DateScopeMode() {
        override val label: String
            get() {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                }
                return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }

        override val startTimestamp: Long
            get() {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

        override val endTimestamp: Long
            get() {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                return cal.timeInMillis
            }

        fun canStepForward(): Boolean {
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)
            return !(year > currentYear || (year == currentYear && month >= currentMonth))
        }

        fun stepPrevious(): MonthMode {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                add(Calendar.MONTH, -1)
            }
            return MonthMode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }

        fun stepNext(): MonthMode {
            if (!canStepForward()) return this
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                add(Calendar.MONTH, 1)
            }
            return MonthMode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
    }

    data class CustomRangeMode(
        override val startTimestamp: Long,
        override val endTimestamp: Long
    ) : DateScopeMode() {
        override val label: String
            get() {
                val sFmt = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(startTimestamp))
                val eFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(endTimestamp))
                return "$sFmt - $eFmt"
            }
    }

    companion object {
        fun currentMonth(): MonthMode {
            val now = Calendar.getInstance()
            return MonthMode(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        }
    }
}
