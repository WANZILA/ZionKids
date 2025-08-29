package com.example.zionkids.core.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DatesUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    private val DISPLAY_DATE_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(millis).atZone(zone).format(DISPLAY_DATE_FMT)
    }
}
