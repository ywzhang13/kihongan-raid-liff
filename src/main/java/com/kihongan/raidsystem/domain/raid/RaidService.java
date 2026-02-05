package com.kihongan.raidsystem.domain.raid;

import com.kihongan.raidsystem.domain.raid.dto.CreateRaidRequest;
import com.kihongan.raidsystem.domain.raid.dto.RaidDTO;
import com.kihongan.raidsystem.domain.signup.SignupRepository;
import com.kihongan.raidsystem.exception.ValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    
    public RaidService(RaidRepository raidRepository, SignupRepository signupRepository, JdbcTemplate jdbcTemplate) {
        this.raidRepository = raidRepository;
        this.signupRepository = signupRepository;
        this.jdbcTemplate = jdbcTemplate;
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
     * Creates a new raid with validation.
     */
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
        
        return raidRepository.save(raid);
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
