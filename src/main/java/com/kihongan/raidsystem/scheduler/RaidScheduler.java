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
     * 
     * Note: This may not run reliably on Render free tier due to cold starts.
     * Use the manual trigger endpoint with external cron service instead.
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
     * Manual trigger for weekly cleanup.
     * Called by external cron service (e.g., cron-job.org).
     */
    public void triggerWeeklyCleanup() {
        logger.info("Manual weekly cleanup triggered via HTTP endpoint");
        clearWeeklyRaids();
    }
}
