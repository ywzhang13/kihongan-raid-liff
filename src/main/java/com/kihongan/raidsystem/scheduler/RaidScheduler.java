package com.kihongan.raidsystem.scheduler;

import com.kihongan.raidsystem.domain.raid.RaidRepository;
import com.kihongan.raidsystem.domain.signup.SignupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduler for raid-related tasks.
 * Handles weekly cleanup of raids and signups.
 */
@Component
public class RaidScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(RaidScheduler.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    public RaidScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Clears all raids and signups every Thursday at 8:00 AM.
     * Cron expression: "0 0 8 * * THU" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: 8
     * - Day of month: * (any)
     * - Month: * (any)
     * - Day of week: THU (Thursday)
     */
    @Scheduled(cron = "0 0 8 * * THU", zone = "Asia/Taipei")
    @Transactional
    public void clearWeeklyRaids() {
        logger.info("Starting weekly raid cleanup...");
        
        try {
            // Delete all signups first (foreign key constraint)
            int signupsDeleted = jdbcTemplate.update("DELETE FROM raid_signups");
            logger.info("Deleted {} signups", signupsDeleted);
            
            // Delete all raids
            int raidsDeleted = jdbcTemplate.update("DELETE FROM raids");
            logger.info("Deleted {} raids", raidsDeleted);
            
            logger.info("Weekly raid cleanup completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during weekly raid cleanup", e);
            throw e;
        }
    }
    
    /**
     * Test method - runs every minute for testing purposes.
     * Comment out or remove in production.
     */
    // @Scheduled(cron = "0 * * * * *")
    // public void testScheduler() {
    //     logger.info("Test scheduler running at: {}", java.time.LocalDateTime.now());
    // }
    
    /**
     * Keep-alive task to prevent Render from sleeping.
     * Runs every 10 minutes to keep the application active.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void keepAlive() {
        try {
            // Simple database query to keep connection alive
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            logger.debug("Keep-alive ping successful");
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
    
    /**
     * Manual trigger for weekly cleanup.
     * Can be called via HTTP endpoint.
     */
    public void triggerWeeklyCleanup() {
        logger.info("Manual weekly cleanup triggered");
        clearWeeklyRaids();
    }
}
