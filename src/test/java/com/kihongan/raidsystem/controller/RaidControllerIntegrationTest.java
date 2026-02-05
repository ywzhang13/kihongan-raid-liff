package com.kihongan.raidsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.domain.raid.dto.CreateRaidRequest;
import com.kihongan.raidsystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RaidController.
 * Tests full request/response cycle with authentication.
 */
@AutoConfigureMockMvc
class RaidControllerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Long testUserId;
    private String testUserToken;
    
    @BeforeEach
    void setUpTestUsers() {
        // Create test user
        testUserId = jdbcTemplate.queryForObject(
                "INSERT INTO users (line_user_id, name) VALUES (?, ?) RETURNING id",
                Long.class,
                "U123456789",
                "Test User"
        );
        
        // Generate JWT token
        testUserToken = jwtService.generateToken(testUserId, "U123456789", 3600000);
    }
    
    @Test
    void listRaids_returnsRaidsOrderedByStartTime() throws Exception {
        // GIVEN multiple raids with different start times
        Instant now = Instant.now();
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid 3", now.plus(3, ChronoUnit.DAYS), testUserId
        );
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid 1", now.plus(1, ChronoUnit.DAYS), testUserId
        );
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid 2", now.plus(2, ChronoUnit.DAYS), testUserId
        );
        
        // WHEN requesting raid list
        mockMvc.perform(get("/raids"))
                // THEN should return 200 with raids ordered by start_time
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Raid 1"))
                .andExpect(jsonPath("$[1].title").value("Raid 2"))
                .andExpect(jsonPath("$[2].title").value("Raid 3"));
    }
    
    @Test
    void createRaid_withValidRequest_returns201() throws Exception {
        // GIVEN a valid raid request
        Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(
                "Dragon Raid", "Hard Mode", "Ancient Dragon", futureTime
        );
        
        // WHEN creating raid
        mockMvc.perform(post("/raids")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 201 with created raid
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Dragon Raid"))
                .andExpect(jsonPath("$.subtitle").value("Hard Mode"))
                .andExpect(jsonPath("$.boss").value("Ancient Dragon"))
                .andExpect(jsonPath("$.createdBy").value(testUserId));
    }
    
    @Test
    void createRaid_withoutToken_returns401() throws Exception {
        // GIVEN a valid raid request
        Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(
                "Dragon Raid", "Hard Mode", "Ancient Dragon", futureTime
        );
        
        // WHEN creating raid without token
        mockMvc.perform(post("/raids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 401
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void createRaid_withEmptyTitle_returns400() throws Exception {
        // GIVEN a request with empty title
        Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(
                "", "Subtitle", "Boss", futureTime
        );
        
        // WHEN creating raid
        mockMvc.perform(post("/raids")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 400
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void createRaid_withPastStartTime_returns400() throws Exception {
        // GIVEN a request with past start time
        Instant pastTime = Instant.now().minus(1, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(
                "Past Raid", "Subtitle", "Boss", pastTime
        );
        
        // WHEN creating raid
        mockMvc.perform(post("/raids")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 400
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("past")));
    }
    
    @Test
    void deleteRaid_withValidRequest_returns204() throws Exception {
        // GIVEN a raid exists
        Long raidId = jdbcTemplate.queryForObject(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "To Delete", Instant.now().plus(1, ChronoUnit.DAYS), testUserId
        );
        
        // WHEN deleting raid
        mockMvc.perform(delete("/raids/" + raidId)
                        .header("Authorization", "Bearer " + testUserToken))
                // THEN should return 204
                .andExpect(status().isNoContent());
        
        // AND raid should be deleted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM raids WHERE id = ?",
                Integer.class,
                raidId
        );
        assert count != null && count == 0;
    }
    
    @Test
    void deleteRaid_cascadesSignups() throws Exception {
        // GIVEN a raid with signups
        Long raidId = jdbcTemplate.queryForObject(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "With Signups", Instant.now().plus(1, ChronoUnit.DAYS), testUserId
        );
        
        Long charId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "TestChar", "Job", 50
        );
        
        jdbcTemplate.update(
                "INSERT INTO raid_signups (raid_id, character_id, status) VALUES (?, ?, ?)",
                raidId, charId, "confirmed"
        );
        
        // WHEN deleting raid
        mockMvc.perform(delete("/raids/" + raidId)
                        .header("Authorization", "Bearer " + testUserToken))
                // THEN should return 204
                .andExpect(status().isNoContent());
        
        // AND signups should also be deleted
        Integer signupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM raid_signups WHERE raid_id = ?",
                Integer.class,
                raidId
        );
        assert signupCount != null && signupCount == 0;
    }
}
