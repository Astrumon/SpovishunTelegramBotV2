package com.ua.astrumon.data.db.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Members : LongIdTable("members") {
    val userId = long("user_id").index()
    val username = varchar("username", 64).uniqueIndex()
    val firstname = varchar("firstname", 128)
    val joinedAt = timestamp("joined_at").nullable()
}