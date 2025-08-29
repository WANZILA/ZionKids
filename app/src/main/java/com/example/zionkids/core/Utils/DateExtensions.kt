package com.example.zionkids.core.Utils

import com.google.firebase.Timestamp
import java.util.Date

fun Long.toTimestamp(): Timestamp = Timestamp(Date(this))
fun Timestamp.toMillis(): Long = this.toDate().time