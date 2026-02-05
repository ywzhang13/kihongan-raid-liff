package com.kihongan.raidsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.domain.signup.dto.SignupRequest;
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
 * Integration tests for Signup endpoints in RaidController.
 * Tests full request/response cycle with authentication.
 */
@AutoConfigureMockMvc
class SignupControllerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Long testUserId;
    private Long otherUserId;
    private String testUserToken;
    private String otherUserToken;
    private Long raidId;
    private Long characterId;
    
    @BeforeEach
    void setUpTestData() {
        // Create test users
        testUserId = jdbcTemplate.queryForObject(
                "INSERT INTO users (line_user_id, name, picture) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "U123456789",
                "Test User",
                "http://example.com/pic.jpg"
        );
        
        otherUserId = jdbcTemplate.queryForObject(
                "INSERT INTO users (line_user_id, name) VALUES (?, ?) RETURNING id",
                Long.class,
                "U987654321",
                "Other User"
        );
        
        // Generate JWT tokens
        testUserToken = jwtService.generateToken(testUserId, "U123456789", 3600000);
        otherUserToken = jwtService.generateToken(otherUserId, "U987654321", 3600000);
        
        // Create test character
        characterId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "TestChar", "Warrior", 50
        );
        
        // Create test raid
        raidId = jdbcTemplate.queryForObject(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "Test Raid",
                Instant.now().plus(1, ChronoUnit.DAYS),
                testUserId
        );
    }
    
    @Test
    void signupForRaid_withValidRequest_returns201() throws Exception {
        // GIVEN a valid signup request
        SignupRequest request = new SignupRequest(characterId);
        
        // WHEN signing up for raid
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 201 with signup details
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.characterId").value(characterId))
                .andExpect(jsonPath("$.characterName").value("TestChar"))
                .andExpect(jsonPath("$.job").value("Warrior"))
                .andExpect(jsonPath("$.level").value(50))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.userPicture").value("http://example.com/pic.jpg"))
                .andExpect(jsonPath("$.status").value("confirmed"));
    }
    
    @Test
    void signupForRaid_withoutToken_returns401() throws Exception {
        // GIVEN a valid signup request
        SignupRequest request = new SignupRequest(characterId);
        
        // WHEN signing up without token
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 401
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void signupForRaid_withOtherUsersCharacter_returns403() throws Exception {
        // GIVEN other user's character
        Long otherCharId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                otherUserId, "OtherChar", "Mage", 45
        );
        
        // WHEN test user tries to signup with other user's character
        SignupRequest request = new SignupRequest(otherCharId);
        
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 403
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("own characters")));
    }
    
    @Test
    void signupForRaid_duplicate_returns400() throws Exception {
        // GIVEN user already signed up
        SignupRequest request = new SignupRequest(characterId);
        
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        // WHEN trying to signup again
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 400
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already signed up")));
    }
    
    @Test
    void signupForRaid_nonExistentRaid_returns404() throws Exception {
        // WHEN signing up for non-existent raid
        SignupRequest request = new SignupRequest(characterId);
        
        mockMvc.perform(post("/raids/99999/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 404
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Raid not found")));
    }
    
    @Test
    void getRaidSignups_returnsAllSignups() throws Exception {
        // GIVEN multiple signups for a raid
        Long char2Id = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "Char2", "Mage", 45
        );
        
        SignupRequest req1 = new SignupRequest(characterId);
        SignupRequest req2 = new SignupRequest(char2Id);
        
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());
        
        mockMvc.perform(post("/raids/" + raidId + "/signup")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());
        
        // WHEN requesting signup list
        mockMvc.perform(get("/raids/" + raidId + "/signups"))
                // THEN should return all signups with complete details
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].characterName", containsInAnyOrder("TestChar", "Char2")))
                .andExpect(jsonPath("$[*].status", everyItem(is("confirmed"))));
    }
    
    @Test
    void getRaidSignups_emptyList_returnsEmptyArray() throws Exception {
        // WHEN requesting signups for raid with no signups
        mockMvc.perform(get("/raids/" + raidId + "/signups"))
                // THEN should return empty array
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
