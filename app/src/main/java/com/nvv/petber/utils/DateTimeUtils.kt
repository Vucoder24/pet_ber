package com.nvv.petber.utils

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private const val DISPLAY_DATE_FORMAT = "dd/MM/yyyy"

    private val displayFormatter = DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT)

    fun formatToDisplay(supabaseDate: String?): String {
        if (supabaseDate.isNullOrEmpty()) return ""
        return try {
            val zonedDateTime = ZonedDateTime.parse(supabaseDate)
            zonedDateTime.format(displayFormatter)
        } catch (_: Exception) {
            try {
                val localDate = LocalDate.parse(supabaseDate)
                localDate.format(displayFormatter)
            } catch (_: Exception) {
                ""
            }
        }
    }

    fun formatToSupabase(displayDate: String?): String? {
        if (displayDate.isNullOrEmpty()) return null
        return try {
            val localDate = LocalDate.parse(displayDate, displayFormatter)
            localDate.toString()
        } catch (_: Exception) {
            null
        }
    }
}