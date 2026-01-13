package com.example.zionkids.data.local.db

import androidx.room.TypeConverter
import com.google.firebase.Timestamp

object TimestampConverters {

    private const val NANOS_PER_SECOND = 1_000_000_000L

    @TypeConverter
    @JvmStatic
    fun fromTimestamp(ts: Timestamp?): Long? {
        if (ts == null) return null
        // nanosSinceEpoch = seconds * 1e9 + nanos
        return ts.seconds * NANOS_PER_SECOND + ts.nanoseconds.toLong()
    }

    @TypeConverter
    @JvmStatic
    fun toTimestamp(value: Long?): Timestamp? {
        if (value == null) return null
        val seconds = value / NANOS_PER_SECOND
        val nanos = (value % NANOS_PER_SECOND).toInt()
        return Timestamp(seconds, nanos)
    }
}
