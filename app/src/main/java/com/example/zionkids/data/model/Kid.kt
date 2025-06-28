package com.example.zionkids.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "kids", indices = [Index(value = ["kidId"], unique = true)])
data class Kid(
    @PrimaryKey() val kidId: Int,
    @ColumnInfo(name = "f_name") val fname: String,
)
