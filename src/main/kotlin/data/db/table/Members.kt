package com.ua.astrumon.data.db.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Members : LongIdTable("members") {
    val userId = long("user_id").index()
    val username = varchar("username", 64).uniqueIndex()
    val firstname = varchar("firstname", 128)
}