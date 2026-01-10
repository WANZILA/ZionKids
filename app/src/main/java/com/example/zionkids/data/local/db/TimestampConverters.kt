package com.example.zionkids.data.local.db

import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import java.util.Date

object TimestampConverters {

    @TypeConverter
    @JvmStatic
    fun fromTimestamp(ts: Timestamp?): Long? = ts?.toDate()?.time

    @TypeConverter
    @JvmStatic
    fun toTimestamp(ms: Long?): Timestamp? = ms?.let { Timestamp(Date(it)) }
}
