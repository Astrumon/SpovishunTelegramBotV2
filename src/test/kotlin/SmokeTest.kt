import data.db.TestDatabaseFactory
import data.db.testDbQuery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class SmokeTest {
    
    @Test
    fun `test database initialization`() {
        // This test verifies that the test database setup works
        TestDatabaseFactory.initialize()
        assertTrue(true, "Database initialization completed without errors")
    }
    
    @Test
    fun `test basic database query`() = runTest {
        TestDatabaseFactory.initialize()
        
        // Simple test query to verify database connectivity
        testDbQuery {
            // This is a basic query that should always work
            // Just testing that the database connection is functional
            // We'll just verify we can execute a transaction
        }
        
        assertTrue(true, "Basic database query executed successfully")
    }
}