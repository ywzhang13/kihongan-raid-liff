package com.kihongan.raidsystem.domain.raid;

import com.kihongan.raidsystem.domain.raid.dto.CreateRaidRequest;
import com.kihongan.raidsystem.domain.raid.dto.RaidDTO;
import com.kihongan.raidsystem.domain.signup.SignupRepository;
import com.kihongan.raidsystem.exception.ValidationException;
import com.kihongan.raidsystem.service.LineMessagingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Raid business logic.
 * Handles validation and transaction management for raids.
 */
@Service
public class RaidService {
    
    private final RaidRepository raidRepository;
    private final SignupRepository signupRepository;
    private final JdbcTemplate jdbcTemplate;
    private final LineMessagingService lineMessagingService;
    
    public RaidService(RaidRepository raidRepository, SignupRepository signupRepository, JdbcTemplate jdbcTemplate, LineMessagingService lineMessagingService) {
        this.raidRepository = raidRepository;
        this.signupRepository = signupRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.lineMessagingService = lineMessagingService;
    }
    
    // For auto-signup after raid creation
    private com.kihongan.raidsystem.domain.signup.SignupService signupService;
    
    public void setSignupService(com.kihongan.raidsystem.domain.signup.SignupService signupService) {
        this.signupService = signupService;
    }
    
    /**
     * Lists all raids ordered by start time.
     */
    public List<Raid> listRaids() {
        return raidRepository.findAllOrderByStartTime();
    }
    
    /**
     * Gets raids with creator names.
     */
    public List<RaidDTO> getRaidsWithCreatorNames(List<Raid> raids) {
        return raids.stream().map(raid -> {
            RaidDTO dto = RaidDTO.fromEntity(raid);
            
            // Get creator name
            String creatorName = jdbcTemplate.queryForObject(
                "SELECT name FROM users WHERE id = ?",
                String.class,
                raid.getCreatedBy()
            );
            dto.setCreatedByName(creatorName);
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * Creates a new raid with validation and auto-signup creator.
     */
    @Transactional
    public Raid createRaid(Long creatorUserId, CreateRaidRequest request) {
        // Validate raid data
        validateRaidData(request);
        
        // Create raid entity
        Raid raid = new Raid();
        raid.setTitle(request.getTitle().trim());
        raid.setSubtitle(request.getSubtitle());
        raid.setBoss(request.getBoss());
        raid.setStartTime(request.getStartTime());
        raid.setCreatedBy(creatorUserId);
        
        Raid savedRaid = raidRepository.save(raid);
        
        // Auto-signup creator if characterId is provided
        String characterName = null;
        String characterJob = null;
        Integer characterLevel = null;
        
        if (request.getCharacterId() != null && signupService != null) {
            try {
                // Get character info before signup
                characterName = jdbcTemplate.queryForObject(
                    "SELECT name FROM characters WHERE id = ?",
                    String.class,
                    request.getCharacterId()
                );
                characterJob = jdbcTemplate.queryForObject(
                    "SELECT job FROM characters WHERE id = ?",
                    String.class,
                    request.getCharacterId()
                );
                characterLevel = jdbcTemplate.queryForObject(
                    "SELECT level FROM characters WHERE id = ?",
                    Integer.class,
                    request.getCharacterId()
                );
                
                System.out.println("DEBUG: Character info retrieved - name: " + characterName + ", job: " + characterJob + ", level: " + characterLevel);
                
                // Signup without notification (will send combined notification below)
                signupService.createSignupWithoutNotification(creatorUserId, savedRaid.getId(), request.getCharacterId());
                
                System.out.println("DEBUG: Auto-signup successful for character: " + characterName);
            } catch (Exception e) {
                System.err.println("Failed to auto-signup creator: " + e.getMessage());
                e.printStackTrace();
                // Reset character info if signup failed
                characterName = null;
                characterJob = null;
                characterLevel = null;
            }
        }
        
        // Send combined LINE notification
        try {
            String creatorName = jdbcTemplate.queryForObject(
                "SELECT name FROM users WHERE id = ?",
                String.class,
                creatorUserId
            );
            
            LocalDateTime startTime = LocalDateTime.ofInstant(savedRaid.getStartTime(), ZoneId.of("Asia/Taipei"));
            
            System.out.println("DEBUG: Preparing to send notification - characterName: " + characterName);
            
            // Send combined notification if creator joined, otherwise just raid created
            if (characterName != null) {
                System.out.println("DEBUG: Sending combined notification");
                lineMessagingService.sendRaidCreatedWithSignupNotification(
                    savedRaid.getTitle(),
                    creatorName,
                    startTime,
                    savedRaid.getSubtitle(),
                    characterName,
                    characterJob,
                    characterLevel
                );
            } else {
                System.out.println("DEBUG: Sending raid created notification only");
                lineMessagingService.sendRaidCreatedNotification(
                    savedRaid.getTitle(),
                    creatorName,
                    startTime,
                    savedRaid.getSubtitle()
                );
            }
            System.out.println("DEBUG: Notification sent successfully");
        } catch (Exception e) {
            // Log but don't fail the operation
            System.err.println("Failed to send raid created notification: " + e.getMessage());
            e.printStackTrace();
        }
        
        return savedRaid;
    }
    
    /**
     * Deletes a raid with cascade deletion of signups.
     */
    @Transactional
    public void deleteRaid(Long raidId) {
        // Delete all signups first (cascade)
        signupRepository.deleteByRaidId(raidId);
        
        // Then delete the raid
        raidRepository.deleteById(raidId);
    }
    
    // Validation helpers
    
    private void validateRaidData(CreateRaidRequest request) {
        // Validate title
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Raid title cannot be empty");
        }
        
        // Validate start_time
        if (request.getStartTime() == null) {
            throw new ValidationException("Raid start time is required");
        }
        
        // Allow start_time up to 1 hour in the past (for testing and timezone handling)
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        if (request.getStartTime().isBefore(oneHourAgo)) {
            throw new ValidationException("Raid start time cannot be more than 1 hour in the past");
        }
    }
}
