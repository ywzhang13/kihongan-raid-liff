package com.kihongan.raidsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.domain.character.dto.CreateCharacterRequest;
import com.kihongan.raidsystem.domain.character.dto.UpdateCharacterRequest;
import com.kihongan.raidsystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CharacterController.
 * Tests full request/response cycle with authentication.
 */
@AutoConfigureMockMvc
class CharacterControllerIntegrationTest extends BaseIntegrationTest {
    
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
    
    @BeforeEach
    void setUpTestUsers() {
        // Create test users
        testUserId = jdbcTemplate.queryForObject(
                "INSERT INTO users (line_user_id, name) VALUES (?, ?) RETURNING id",
                Long.class,
                "U123456789",
                "Test User"
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
    }
    
    @Test
    void getMyCharacters_withValidToken_returnsCharacterList() throws Exception {
        // GIVEN user has characters
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                testUserId, "Warrior", "Tank", 50
        );
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                testUserId, "Mage", "DPS", 45
        );
        
        // WHEN requesting character list
        mockMvc.perform(get("/me/characters")
                        .header("Authorization", "Bearer " + testUserToken))
                // THEN should return 200 with character list
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Warrior", "Mage")));
    }
    
    @Test
    void getMyCharacters_withoutToken_returns401() throws Exception {
        // WHEN requesting without token
        mockMvc.perform(get("/me/characters"))
                // THEN should return 401
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void createCharacter_withValidRequest_returns201() throws Exception {
        // GIVEN a valid character request
        CreateCharacterRequest request = new CreateCharacterRequest(
                "NewChar", "Healer", 60, "GAME123", "My main", true
        );
        
        // WHEN creating character
        mockMvc.perform(post("/me/characters")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 201 with created character
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("NewChar"))
                .andExpect(jsonPath("$.job").value("Healer"))
                .andExpect(jsonPath("$.level").value(60))
                .andExpect(jsonPath("$.isDefault").value(true));
    }
    
    @Test
    void createCharacter_withEmptyName_returns400() throws Exception {
        // GIVEN a request with empty name
        CreateCharacterRequest request = new CreateCharacterRequest(
                "", "Job", 50, null, null, false
        );
        
        // WHEN creating character
        mockMvc.perform(post("/me/characters")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 400
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void updateCharacter_withValidRequest_returns200() throws Exception {
        // GIVEN user has a character
        Long charId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "OldName", "OldJob", 50
        );
        
        // AND an update request
        UpdateCharacterRequest request = new UpdateCharacterRequest();
        request.setName("NewName");
        request.setLevel(99);
        
        // WHEN updating character
        mockMvc.perform(put("/me/characters/" + charId)
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 200 with updated character
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.level").value(99))
                .andExpect(jsonPath("$.job").value("OldJob")); // Preserved
    }
    
    @Test
    void updateCharacter_ofOtherUser_returns403() throws Exception {
        // GIVEN other user has a character
        Long charId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                otherUserId, "OtherChar", "Job", 50
        );
        
        // AND test user tries to update it
        UpdateCharacterRequest request = new UpdateCharacterRequest();
        request.setName("Hacked");
        
        // WHEN updating other user's character
        mockMvc.perform(put("/me/characters/" + charId)
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN should return 403
                .andExpect(status().isForbidden());
    }
    
    @Test
    void deleteCharacter_withValidRequest_returns204() throws Exception {
        // GIVEN user has a character
        Long charId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "ToDelete", "Job", 50
        );
        
        // WHEN deleting character
        mockMvc.perform(delete("/me/characters/" + charId)
                        .header("Authorization", "Bearer " + testUserToken))
                // THEN should return 204
                .andExpect(status().isNoContent());
        
        // AND character should be deleted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM characters WHERE id = ?",
                Integer.class,
                charId
        );
        assert count != null && count == 0;
    }
    
    @Test
    void deleteCharacter_ofOtherUser_returns403() throws Exception {
        // GIVEN other user has a character
        Long charId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                otherUserId, "OtherChar", "Job", 50
        );
        
        // WHEN test user tries to delete it
        mockMvc.perform(delete("/me/characters/" + charId)
                        .header("Authorization", "Bearer " + testUserToken))
                // THEN should return 403
                .andExpect(status().isForbidden());
    }
    
    @Test
    void deleteCharacter_withActiveSignups_returns400() throws Exception {
        // GIVEN user has a character with a signup
        Long charId = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "WithSignup", "Job", 50
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, NOW() + INTERVAL '1 day', ?) RETURNING id",
                Long.class,
                "Test Raid",
                testUserId
        );
        
        jdbcTemplate.update(
                "INSERT INTO raid_signups (raid_id, character_id, status) VALUES (?, ?, ?)",
                raidId, charId, "confirmed"
        );
        
        // WHEN trying to delete character
        mockMvc.perform(delete("/me/characters/" + charId)
                        .header("Authorization", "Bearer " + testUserToken))
                // THEN should return 400
                .andExpect(status().isBadRequest());
    }
}
