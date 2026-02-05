package com.kihongan.raidsystem.domain.signup;

import com.kihongan.raidsystem.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SignupRepository.
 * Tests signup creation, queries, and duplicate detection.
 */
class SignupRepositoryTest extends BaseIntegrationTest {
    
    @Autowired
    private SignupRepository signupRepository;
    
    private Long testUserId;
    private Long raidId;
    private Long characterId;
    
    @BeforeEach
    void setUpTestData() {
        // Create test user
        testUserId = jdbcTemplate.queryForObject(
                "INSERT INTO users (line_user_id, name, picture) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "U123456789",
                "Test User",
                "http://example.com/pic.jpg"
        );
        
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
    void createSignup_shouldInsertAndReturnWithId() {
        // GIVEN a new signup
        Signup signup = new Signup();
        signup.setRaidId(raidId);
        signup.setCharacterId(characterId);
        signup.setStatus("confirmed");
        
        // WHEN saving the signup
        Signup saved = signupRepository.save(signup);
        
        // THEN it should have an ID and timestamp
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("confirmed");
    }
    
    @Test
    void findByRaidIdWithDetails_shouldReturnCompleteInformation() {
        // GIVEN a signup exists
        Signup signup = new Signup();
        signup.setRaidId(raidId);
        signup.setCharacterId(characterId);
        signup.setStatus("confirmed");
        signupRepository.save(signup);
        
        // WHEN querying signups with details
        List<SignupWithDetails> signups = signupRepository.findByRaidIdWithDetails(raidId);
        
        // THEN it should return complete character and user information
        assertThat(signups).hasSize(1);
        
        SignupWithDetails details = signups.get(0);
        assertThat(details.getSignupId()).isNotNull();
        assertThat(details.getCharacterId()).isEqualTo(characterId);
        assertThat(details.getCharacterName()).isEqualTo("TestChar");
        assertThat(details.getJob()).isEqualTo("Warrior");
        assertThat(details.getLevel()).isEqualTo(50);
        assertThat(details.getUserId()).isEqualTo(testUserId);
        assertThat(details.getUserName()).isEqualTo("Test User");
        assertThat(details.getUserPicture()).isEqualTo("http://example.com/pic.jpg");
        assertThat(details.getStatus()).isEqualTo("confirmed");
    }
    
    @Test
    void findByRaidIdWithDetails_shouldReturnMultipleSignups() {
        // GIVEN multiple signups for a raid
        Long char2Id = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "Char2", "Mage", 45
        );
        
        Signup signup1 = new Signup();
        signup1.setRaidId(raidId);
        signup1.setCharacterId(characterId);
        signup1.setStatus("confirmed");
        signupRepository.save(signup1);
        
        Signup signup2 = new Signup();
        signup2.setRaidId(raidId);
        signup2.setCharacterId(char2Id);
        signup2.setStatus("confirmed");
        signupRepository.save(signup2);
        
        // WHEN querying signups
        List<SignupWithDetails> signups = signupRepository.findByRaidIdWithDetails(raidId);
        
        // THEN all signups should be returned
        assertThat(signups).hasSize(2);
        assertThat(signups).extracting(SignupWithDetails::getCharacterName)
                .containsExactly("TestChar", "Char2"); // Ordered by created_at
    }
    
    @Test
    void existsByRaidIdAndCharacterId_shouldReturnTrueWhenExists() {
        // GIVEN a signup exists
        Signup signup = new Signup();
        signup.setRaidId(raidId);
        signup.setCharacterId(characterId);
        signup.setStatus("confirmed");
        signupRepository.save(signup);
        
        // WHEN checking if signup exists
        boolean exists = signupRepository.existsByRaidIdAndCharacterId(raidId, characterId);
        
        // THEN it should return true
        assertThat(exists).isTrue();
    }
    
    @Test
    void existsByRaidIdAndCharacterId_shouldReturnFalseWhenNotExists() {
        // WHEN checking if non-existent signup exists
        boolean exists = signupRepository.existsByRaidIdAndCharacterId(raidId, characterId);
        
        // THEN it should return false
        assertThat(exists).isFalse();
    }
    
    @Test
    void deleteByRaidId_shouldRemoveAllSignupsForRaid() {
        // GIVEN multiple signups for a raid
        Long char2Id = jdbcTemplate.queryForObject(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                testUserId, "Char2", "Mage", 45
        );
        
        Signup signup1 = new Signup();
        signup1.setRaidId(raidId);
        signup1.setCharacterId(characterId);
        signup1.setStatus("confirmed");
        signupRepository.save(signup1);
        
        Signup signup2 = new Signup();
        signup2.setRaidId(raidId);
        signup2.setCharacterId(char2Id);
        signup2.setStatus("confirmed");
        signupRepository.save(signup2);
        
        // WHEN deleting all signups for the raid
        signupRepository.deleteByRaidId(raidId);
        
        // THEN all signups should be removed
        List<SignupWithDetails> signups = signupRepository.findByRaidIdWithDetails(raidId);
        assertThat(signups).isEmpty();
    }
}
