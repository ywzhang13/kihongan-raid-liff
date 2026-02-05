package com.kihongan.raidsystem.domain.signup;

import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.exception.AuthorizationException;
import com.kihongan.raidsystem.exception.NotFoundException;
import com.kihongan.raidsystem.exception.ValidationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for SignupService.
 * Tests universal correctness properties with random inputs.
 */
@JqwikSpringSupport
class SignupServicePropertyTest extends BaseIntegrationTest {
    
    @Autowired
    private SignupService signupService;
    
    // Feature: kihongan-raid-system, Property 16: Signup creates valid association
    @Property(tries = 100)
    void signupCreatesValidAssociation(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String charName,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String raidTitle) {
        
        // GIVEN a user with a character and a raid
        createTestUser(userId, "U" + userId);
        
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                userId, charName, "Job", 50
        );
        
        Long charId = jdbcTemplate.queryForObject(
                "SELECT id FROM characters WHERE user_id = ? AND name = ?",
                Long.class,
                userId, charName
        );
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                raidTitle,
                Instant.now().plus(1, ChronoUnit.DAYS),
                userId
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ? AND created_by = ?",
                Long.class,
                raidTitle, userId
        );
        
        // WHEN creating a signup
        Signup signup = signupService.createSignup(userId, raidId, charId);
        
        // THEN it should create a valid association with status 'confirmed'
        assertThat(signup.getId()).isNotNull();
        assertThat(signup.getRaidId()).isEqualTo(raidId);
        assertThat(signup.getCharacterId()).isEqualTo(charId);
        assertThat(signup.getStatus()).isEqualTo("confirmed");
        
        // AND it should be retrievable
        List<SignupWithDetails> signups = signupService.getRaidSignups(raidId);
        assertThat(signups).hasSize(1);
        assertThat(signups.get(0).getCharacterId()).isEqualTo(charId);
    }
    
    // Feature: kihongan-raid-system, Property 17: Owned character signup accepted
    @Property(tries = 100)
    void ownedCharacterSignupAccepted(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String charName) {
        
        // GIVEN a user with their own character
        createTestUser(userId, "U" + userId);
        
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                userId, charName, "Job", 50
        );
        
        Long charId = jdbcTemplate.queryForObject(
                "SELECT id FROM characters WHERE user_id = ? AND name = ?",
                Long.class,
                userId, charName
        );
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid",
                Instant.now().plus(1, ChronoUnit.DAYS),
                userId
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ? AND created_by = ?",
                Long.class,
                "Raid", userId
        );
        
        // WHEN signing up with their own character
        // THEN it should succeed
        assertThatCode(() -> signupService.createSignup(userId, raidId, charId))
                .doesNotThrowAnyException();
    }
    
    // Feature: kihongan-raid-system, Property 18: Unowned character signup rejected
    @Property(tries = 100)
    void unownedCharacterSignupRejected(
            @ForAll @LongRange(min = 1, max = 5000) Long owner,
            @ForAll @LongRange(min = 5001, max = 10000) Long otherUser,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String charName) {
        
        // GIVEN two users and a character owned by one
        createTestUser(owner, "U" + owner);
        createTestUser(otherUser, "U" + otherUser);
        
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                owner, charName, "Job", 50
        );
        
        Long charId = jdbcTemplate.queryForObject(
                "SELECT id FROM characters WHERE user_id = ? AND name = ?",
                Long.class,
                owner, charName
        );
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid",
                Instant.now().plus(1, ChronoUnit.DAYS),
                owner
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ? AND created_by = ?",
                Long.class,
                "Raid", owner
        );
        
        // WHEN the other user tries to signup with the character
        // THEN it should throw AuthorizationException
        assertThatThrownBy(() -> signupService.createSignup(otherUser, raidId, charId))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("own characters");
    }
    
    // Feature: kihongan-raid-system, Property 19: Duplicate signup rejected
    @Property(tries = 100)
    void duplicateSignupRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String charName) {
        
        // GIVEN a user with a character already signed up for a raid
        createTestUser(userId, "U" + userId);
        
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                userId, charName, "Job", 50
        );
        
        Long charId = jdbcTemplate.queryForObject(
                "SELECT id FROM characters WHERE user_id = ? AND name = ?",
                Long.class,
                userId, charName
        );
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid",
                Instant.now().plus(1, ChronoUnit.DAYS),
                userId
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ? AND created_by = ?",
                Long.class,
                "Raid", userId
        );
        
        // First signup succeeds
        signupService.createSignup(userId, raidId, charId);
        
        // WHEN attempting to signup again with the same character
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> signupService.createSignup(userId, raidId, charId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already signed up");
    }
    
    // Feature: kihongan-raid-system, Property 20: Signup list completeness
    @Property(tries = 50)
    void signupListCompleteness(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @IntRange(min = 2, max = 5) int signupCount) {
        
        // GIVEN a raid with multiple signups
        createTestUser(userId, "U" + userId);
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid",
                Instant.now().plus(1, ChronoUnit.DAYS),
                userId
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ? AND created_by = ?",
                Long.class,
                "Raid", userId
        );
        
        for (int i = 0; i < signupCount; i++) {
            jdbcTemplate.update(
                    "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                    userId, "Char" + i, "Job" + i, 50 + i
            );
            
            Long charId = jdbcTemplate.queryForObject(
                    "SELECT id FROM characters WHERE user_id = ? AND name = ?",
                    Long.class,
                    userId, "Char" + i
            );
            
            signupService.createSignup(userId, raidId, charId);
        }
        
        // WHEN querying signup list
        List<SignupWithDetails> signups = signupService.getRaidSignups(raidId);
        
        // THEN all signups should be returned with complete information
        assertThat(signups).hasSize(signupCount);
        
        for (SignupWithDetails signup : signups) {
            // Verify complete character information
            assertThat(signup.getCharacterId()).isNotNull();
            assertThat(signup.getCharacterName()).isNotBlank();
            assertThat(signup.getJob()).isNotBlank();
            assertThat(signup.getLevel()).isNotNull();
            
            // Verify complete user information
            assertThat(signup.getUserId()).isEqualTo(userId);
            assertThat(signup.getUserName()).isNotBlank();
            
            // Verify status
            assertThat(signup.getStatus()).isEqualTo("confirmed");
        }
    }
    
    // Feature: kihongan-raid-system, Property 22: Signup requires valid references
    @Property(tries = 100)
    void signupRequiresValidReferences(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 90000, max = 99999) Long nonExistentId) {
        
        // GIVEN a user with a character and a raid
        createTestUser(userId, "U" + userId);
        
        jdbcTemplate.update(
                "INSERT INTO characters (user_id, name, job, level) VALUES (?, ?, ?, ?)",
                userId, "Char", "Job", 50
        );
        
        Long charId = jdbcTemplate.queryForObject(
                "SELECT id FROM characters WHERE user_id = ? AND name = ?",
                Long.class,
                userId, "Char"
        );
        
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, ?, ?)",
                "Raid",
                Instant.now().plus(1, ChronoUnit.DAYS),
                userId
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ? AND created_by = ?",
                Long.class,
                "Raid", userId
        );
        
        // WHEN attempting signup with non-existent raid
        // THEN it should throw NotFoundException
        assertThatThrownBy(() -> signupService.createSignup(userId, nonExistentId, charId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Raid not found");
        
        // WHEN attempting signup with non-existent character
        // THEN it should throw NotFoundException
        assertThatThrownBy(() -> signupService.createSignup(userId, raidId, nonExistentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Character not found");
    }
    
    // Helper method
    private void createTestUser(Long userId, String lineUserId) {
        jdbcTemplate.update(
                "MERGE INTO users (id, line_user_id, name) KEY(id) VALUES (?, ?, ?)",
                userId, lineUserId, "User" + userId
        );
    }
}
