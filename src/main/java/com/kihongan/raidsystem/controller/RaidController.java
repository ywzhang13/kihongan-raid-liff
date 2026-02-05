package com.kihongan.raidsystem.controller;

import com.kihongan.raidsystem.domain.raid.Raid;
import com.kihongan.raidsystem.domain.raid.RaidService;
import com.kihongan.raidsystem.domain.raid.dto.CreateRaidRequest;
import com.kihongan.raidsystem.domain.raid.dto.RaidDTO;
import com.kihongan.raidsystem.domain.signup.Signup;
import com.kihongan.raidsystem.domain.signup.SignupService;
import com.kihongan.raidsystem.domain.signup.SignupWithDetails;
import com.kihongan.raidsystem.domain.signup.dto.SignupDTO;
import com.kihongan.raidsystem.domain.signup.dto.SignupRequest;
import com.kihongan.raidsystem.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for raid management endpoints.
 * Handles raid CRUD operations and signup operations with JWT authentication.
 */
@RestController
@RequestMapping("/raids")
public class RaidController {
    
    private final RaidService raidService;
    private final SignupService signupService;
    
    public RaidController(RaidService raidService, SignupService signupService) {
        this.raidService = raidService;
        this.signupService = signupService;
    }
    
    /**
     * GET /raids - List all raids ordered by start time
     */
    @GetMapping
    public ResponseEntity<List<RaidDTO>> listRaids() {
        List<Raid> raids = raidService.listRaids();
        List<RaidDTO> dtos = raidService.getRaidsWithCreatorNames(raids);
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * POST /raids - Create a new raid
     */
    @PostMapping
    public ResponseEntity<RaidDTO> createRaid(
            @AuthUser Long userId,
            @Valid @RequestBody CreateRaidRequest request) {
        
        Raid created = raidService.createRaid(userId, request);
        RaidDTO dto = RaidDTO.fromEntity(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    
    /**
     * DELETE /raids/{id} - Delete a raid
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRaid(
            @AuthUser Long userId,
            @PathVariable Long id) {
        
        raidService.deleteRaid(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * POST /raids/{raidId}/signup - Sign up for a raid
     */
    @PostMapping("/{raidId}/signup")
    public ResponseEntity<SignupDTO> signupForRaid(
            @AuthUser Long userId,
            @PathVariable Long raidId,
            @Valid @RequestBody SignupRequest request) {
        
        Signup signup = signupService.createSignup(userId, raidId, request.getCharacterId());
        
        // Get complete signup details
        List<SignupWithDetails> signups = signupService.getRaidSignups(raidId);
        SignupWithDetails details = signups.stream()
                .filter(s -> s.getSignupId().equals(signup.getId()))
                .findFirst()
                .orElseThrow();
        
        SignupDTO dto = SignupDTO.fromDetails(details);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    
    /**
     * GET /raids/{raidId}/signups - Get all signups for a raid
     */
    @GetMapping("/{raidId}/signups")
    public ResponseEntity<List<SignupDTO>> getRaidSignups(@PathVariable Long raidId) {
        List<SignupWithDetails> signups = signupService.getRaidSignups(raidId);
        List<SignupDTO> dtos = signups.stream()
                .map(SignupDTO::fromDetails)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * DELETE /raids/{raidId}/signup - Cancel signup for a raid
     */
    @DeleteMapping("/{raidId}/signup")
    public ResponseEntity<Void> cancelSignup(
            @AuthUser Long userId,
            @PathVariable Long raidId) {
        
        signupService.cancelSignup(userId, raidId);
        return ResponseEntity.noContent().build();
    }
}
