package com.ua.astrumon.data.db.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Groups: LongIdTable("groups") {
    val name = varchar("name", 64).uniqueIndex()
}