package com.kihongan.raidsystem;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests with database support.
 * Uses Testcontainers to spin up a PostgreSQL instance for testing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void cleanDatabase() {
        // Clean up tables in reverse order of dependencies
        jdbcTemplate.execute("DELETE FROM raid_signups");
        jdbcTemplate.execute("DELETE FROM raids");
        jdbcTemplate.execute("DELETE FROM characters");
        jdbcTemplate.execute("DELETE FROM users");
        
        // H2 automatically resets IDENTITY sequences when table is empty
        // No need to manually restart sequences
    }
}
