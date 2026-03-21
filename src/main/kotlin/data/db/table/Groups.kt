package com.ua.astrumon.data.db.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Groups: LongIdTable("groups") {
    val chatId = long("chat_id")
    val name = varchar("name", 64)

    init {
        uniqueIndex(chatId, name)
    }
}