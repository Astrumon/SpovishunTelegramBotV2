package com.ua.astrumon.data.db.table

import org.jetbrains.exposed.sql.Table

object GroupMembers: Table("group_members") {
    val group = reference("group_id", Groups)
    val member = reference("member_id", Members)

    override val primaryKey = PrimaryKey(group, member)
}