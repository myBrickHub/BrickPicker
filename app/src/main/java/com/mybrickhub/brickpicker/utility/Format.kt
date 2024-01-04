package com.mybrickhub.brickpicker.utility

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

object Format {
    fun convertUtcToLocal(dateTimeString: String, dateFormat: String): String {
        return try {
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val utcDate = utcDateFormat.parse(dateTimeString)
            val localDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
            localDateFormat.timeZone = TimeZone.getDefault()

            localDateFormat.format(utcDate as Date)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun convertTimestamp(dateTime: Long, dateFormat: String): String {
        return try {
            val date = Date(dateTime)
            val localDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
            localDateFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun toDecimal (text: String) : BigDecimal? {
        return BigDecimal(text).setScale(
            2,
            RoundingMode.HALF_EVEN
        )
    }
}
