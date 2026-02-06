package com.kihongan.raidsystem.controller;

import com.kihongan.raidsystem.scheduler.RaidScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for manual scheduler triggers.
 * Allows external cron services to trigger scheduled tasks.
 */
@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    
    private final RaidScheduler raidScheduler;
    
    @Value("${app.scheduler.secret:}")
    private String schedulerSecret;
    
    public SchedulerController(RaidScheduler raidScheduler) {
        this.raidScheduler = raidScheduler;
    }
    
    /**
     * Manually trigger weekly raid cleanup.
     * Requires secret token for security.
     * 
     * Usage: POST /api/scheduler/cleanup?secret=YOUR_SECRET
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> triggerCleanup(@RequestParam(required = false) String secret) {
        // Verify secret if configured
        if (schedulerSecret != null && !schedulerSecret.isEmpty()) {
            if (secret == null || !secret.equals(schedulerSecret)) {
                return ResponseEntity.status(403).body("Forbidden: Invalid secret");
            }
        }
        
        try {
            raidScheduler.triggerWeeklyCleanup();
            return ResponseEntity.ok("Weekly cleanup triggered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Cleanup failed: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for scheduler.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Scheduler is running");
    }
}
