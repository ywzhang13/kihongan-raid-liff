package com.kihongan.raidsystem.domain.character;

import com.kihongan.raidsystem.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CharacterRepository.
 * Tests CRUD operations and query methods with specific examples.
 */
class CharacterRepositoryTest extends BaseIntegrationTest {
    
    @Autowired
    private CharacterRepository characterRepository;
    
    private Long testUserId;
    private Long otherUserId;
    
    @BeforeEach
    void setUpTestData() {
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
    }
    
    @Test
    void createCharacter_shouldInsertAndReturnWithId() {
        // GIVEN a new character
        Character character = new Character();
        character.setUserId(testUserId);
        character.setName("Warrior");
        character.setJob("Tank");
        character.setLevel(50);
        character.setGameId("GAME123");
        character.setNote("Main tank");
        character.setIsDefault(true);
        
        // WHEN saving the character
        Character saved = characterRepository.save(character);
        
        // THEN it should have an ID and timestamps
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Warrior");
        assertThat(saved.getJob()).isEqualTo("Tank");
        assertThat(saved.getLevel()).isEqualTo(50);
        assertThat(saved.getIsDefault()).isTrue();
    }
    
    @Test
    void findByUserId_shouldReturnOnlyUserCharacters() {
        // GIVEN characters for two different users
        Character char1 = createCharacter(testUserId, "Char1", "Job1");
        Character char2 = createCharacter(testUserId, "Char2", "Job2");
        Character char3 = createCharacter(otherUserId, "Char3", "Job3");
        
        characterRepository.save(char1);
        characterRepository.save(char2);
        characterRepository.save(char3);
        
        // WHEN querying for test user's characters
        List<Character> characters = characterRepository.findByUserId(testUserId);
        
        // THEN only test user's characters should be returned
        assertThat(characters).hasSize(2);
        assertThat(characters).extracting(Character::getName)
                .containsExactlyInAnyOrder("Char1", "Char2");
    }
    
    @Test
    void findById_shouldReturnCharacterWhenExists() {
        // GIVEN a saved character
        Character character = createCharacter(testUserId, "TestChar", "TestJob");
        Character saved = characterRepository.save(character);
        
        // WHEN finding by ID
        Optional<Character> found = characterRepository.findById(saved.getId());
        
        // THEN the character should be found
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TestChar");
        assertThat(found.get().getJob()).isEqualTo("TestJob");
    }
    
    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // WHEN finding a non-existent character
        Optional<Character> found = characterRepository.findById(99999L);
        
        // THEN it should return empty
        assertThat(found).isEmpty();
    }
    
    @Test
    void updateCharacter_shouldModifyExistingCharacter() {
        // GIVEN a saved character
        Character character = createCharacter(testUserId, "OldName", "OldJob");
        Character saved = characterRepository.save(character);
        
        // WHEN updating the character
        saved.setName("NewName");
        saved.setJob("NewJob");
        saved.setLevel(99);
        Character updated = characterRepository.save(saved);
        
        // THEN the changes should be persisted
        Optional<Character> found = characterRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("NewName");
        assertThat(found.get().getJob()).isEqualTo("NewJob");
        assertThat(found.get().getLevel()).isEqualTo(99);
        assertThat(found.get().getUpdatedAt()).isAfter(found.get().getCreatedAt());
    }
    
    @Test
    void deleteById_shouldRemoveCharacter() {
        // GIVEN a saved character
        Character character = createCharacter(testUserId, "ToDelete", "Job");
        Character saved = characterRepository.save(character);
        
        // WHEN deleting the character
        characterRepository.deleteById(saved.getId());
        
        // THEN it should no longer exist
        Optional<Character> found = characterRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
    
    @Test
    void unsetDefaultForUser_shouldUnsetAllDefaults() {
        // GIVEN multiple characters with one default
        Character char1 = createCharacter(testUserId, "Char1", "Job1");
        char1.setIsDefault(true);
        Character char2 = createCharacter(testUserId, "Char2", "Job2");
        char2.setIsDefault(false);
        
        characterRepository.save(char1);
        characterRepository.save(char2);
        
        // WHEN unsetting defaults for user
        characterRepository.unsetDefaultForUser(testUserId);
        
        // THEN all characters should have isDefault = false
        List<Character> characters = characterRepository.findByUserId(testUserId);
        assertThat(characters).allMatch(c -> !c.getIsDefault());
    }
    
    @Test
    void existsByIdAndUserId_shouldReturnTrueWhenOwned() {
        // GIVEN a character owned by test user
        Character character = createCharacter(testUserId, "Owned", "Job");
        Character saved = characterRepository.save(character);
        
        // WHEN checking ownership
        boolean exists = characterRepository.existsByIdAndUserId(saved.getId(), testUserId);
        
        // THEN it should return true
        assertThat(exists).isTrue();
    }
    
    @Test
    void existsByIdAndUserId_shouldReturnFalseWhenNotOwned() {
        // GIVEN a character owned by test user
        Character character = createCharacter(testUserId, "NotOwned", "Job");
        Character saved = characterRepository.save(character);
        
        // WHEN checking ownership by other user
        boolean exists = characterRepository.existsByIdAndUserId(saved.getId(), otherUserId);
        
        // THEN it should return false
        assertThat(exists).isFalse();
    }
    
    @Test
    void hasActiveSignups_shouldReturnTrueWhenSignupsExist() {
        // GIVEN a character with a signup
        Character character = createCharacter(testUserId, "WithSignup", "Job");
        Character saved = characterRepository.save(character);
        
        // Create a raid and signup
        Long raidId = jdbcTemplate.queryForObject(
                "INSERT INTO raids (title, start_time, created_by) VALUES (?, NOW() + INTERVAL '1 day', ?) RETURNING id",
                Long.class,
                "Test Raid",
                testUserId
        );
        
        jdbcTemplate.update(
                "INSERT INTO raid_signups (raid_id, character_id, status) VALUES (?, ?, ?)",
                raidId,
                saved.getId(),
                "confirmed"
        );
        
        // WHEN checking for active signups
        boolean hasSignups = characterRepository.hasActiveSignups(saved.getId());
        
        // THEN it should return true
        assertThat(hasSignups).isTrue();
    }
    
    @Test
    void hasActiveSignups_shouldReturnFalseWhenNoSignups() {
        // GIVEN a character without signups
        Character character = createCharacter(testUserId, "NoSignup", "Job");
        Character saved = characterRepository.save(character);
        
        // WHEN checking for active signups
        boolean hasSignups = characterRepository.hasActiveSignups(saved.getId());
        
        // THEN it should return false
        assertThat(hasSignups).isFalse();
    }
    
    @Test
    void createCharacter_withNullOptionalFields_shouldSucceed() {
        // GIVEN a character with only required fields
        Character character = new Character();
        character.setUserId(testUserId);
        character.setName("MinimalChar");
        character.setJob(null);
        character.setLevel(null);
        character.setGameId(null);
        character.setNote(null);
        character.setIsDefault(false);
        
        // WHEN saving the character
        Character saved = characterRepository.save(character);
        
        // THEN it should succeed with null optional fields
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("MinimalChar");
        assertThat(saved.getJob()).isNull();
        assertThat(saved.getLevel()).isNull();
        assertThat(saved.getGameId()).isNull();
        assertThat(saved.getNote()).isNull();
    }
    
    // Helper method
    private Character createCharacter(Long userId, String name, String job) {
        Character character = new Character();
        character.setUserId(userId);
        character.setName(name);
        character.setJob(job);
        character.setLevel(50);
        character.setIsDefault(false);
        return character;
    }
}
