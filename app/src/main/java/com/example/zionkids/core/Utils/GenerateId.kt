package com.example.zionkids.core.Utils

import java.util.UUID

object GenerateId {
    fun generateId(type: String): String {
        val prefix = when (type.lowercase()) {
            "child" -> "ch"
            "volunteer" -> "vo"
            "admin" -> "ad"
            "event" -> "ev"
            "other" -> "ot"
            else -> "gen"
        }
        val timePart = System.currentTimeMillis().toString()       // full millis
        val randomPart = UUID.randomUUID().toString()
            .replace("-", "")
            .take(8)
            .uppercase()
        return "$prefix$timePart$randomPart"
    }
}
