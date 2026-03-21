package data.db

import com.ua.astrumon.common.exception.DatabaseException
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object TestDatabaseFactory {
    private val logger = LoggerFactory.getLogger(TestDatabaseFactory::class.java)
    
    fun initialize() {
        try {
            logger.info("Initializing test database connection...")
            
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 1
                minimumIdle = 1
            }
            
            val dataSource = HikariDataSource(hikariConfig)
            Database.connect(dataSource)
            
            logger.info("Test database connection established successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize test database", e)
            throw DatabaseException("Test database initialization failed", e)
        }
    }
}

suspend fun <T> testDbQuery(block: () -> T): T =
    withContext(Dispatchers.IO) { 
        try {
            transaction { block() }
        } catch (e: Exception) {
            throw DatabaseException("Test database query failed", e)
        }
    }
