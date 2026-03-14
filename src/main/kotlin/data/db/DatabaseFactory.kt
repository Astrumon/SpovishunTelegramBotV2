package com.ua.astrumon.data.db

import com.ua.astrumon.config.AppConfig
import com.ua.astrumon.common.exception.DatabaseException
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import com.ua.astrumon.data.db.table.Groups
import com.ua.astrumon.data.db.table.Members
import com.ua.astrumon.data.db.table.GroupMembers

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    fun initialize(config: AppConfig) {
        try {
            logger.info("Initializing database connection...")
            
            val hikariConfig = HikariConfig().apply {
                driverClassName = config.databaseDriver
                jdbcUrl = config.databaseUrl
                username = config.databaseUsername
                password = config.databasePassword
                maximumPoolSize = 10
                minimumIdle = 5
                idleTimeout = 30000
                connectionTimeout = 30000
                maxLifetime = 1800000
            }
            
            val dataSource = HikariDataSource(hikariConfig)
            Database.connect(dataSource)
            
            logger.info("Database connection established. Creating schema...")
            
            // Auto-migrate schema with proper error handling
            transaction {
                addLogger(StdOutSqlLogger)
                try {
                    SchemaUtils.createMissingTablesAndColumns(Groups, Members, GroupMembers)
                    logger.info("Database schema migration completed successfully")
                } catch (e: Exception) {
                    logger.error("Failed to migrate database schema", e)
                    throw DatabaseException("Schema migration failed", e)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Failed to initialize database", e)
            throw DatabaseException("Database initialization failed", e)
        }
    }
}

suspend fun <T> dbQuery(block: () -> T): T =
    withContext(Dispatchers.IO) { 
        try {
            transaction { block() }
        } catch (e: Exception) {
            throw DatabaseException("Database query failed", e)
        }
    }