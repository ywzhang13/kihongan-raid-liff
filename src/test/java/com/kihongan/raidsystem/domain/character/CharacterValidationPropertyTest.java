package com.kihongan.raidsystem.domain.character;

import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.domain.character.dto.CreateCharacterRequest;
import com.kihongan.raidsystem.domain.character.dto.UpdateCharacterRequest;
import com.kihongan.raidsystem.exception.ValidationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Character validation rules.
 */
class CharacterValidationPropertyTest extends BaseIntegrationTest {
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    // Feature: kihongan-raid-system, Property 8: Empty required fields rejected
    @Property(tries = 100)
    void emptyRequiredFieldsRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll("emptyOrWhitespaceStrings") String emptyName) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a character with empty name
        CreateCharacterRequest req = new CreateCharacterRequest(
                emptyName, "Job", 50, null, null, false
        );
        
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> characterService.createCharacter(userId, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name");
    }
    
    // Feature: kihongan-raid-system, Property 9: Invalid numeric values rejected
    @Property(tries = 100)
    void invalidNumericValuesRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name,
            @ForAll @IntRange(min = -1000, max = -1) int negativeLevel) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a character with negative level
        CreateCharacterRequest req = new CreateCharacterRequest(
                name, "Job", negativeLevel, null, null, false
        );
        
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> characterService.createCharacter(userId, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("level");
    }
    
    @Property(tries = 100)
    void updateWithNegativeLevelRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name,
            @ForAll @IntRange(min = -1000, max = -1) int negativeLevel) {
        
        // GIVEN a user with a character
        createTestUser(userId, "U" + userId);
        CreateCharacterRequest createReq = new CreateCharacterRequest(
                name, "Job", 50, null, null, false
        );
        Character created = characterService.createCharacter(userId, createReq);
        
        // WHEN updating with negative level
        UpdateCharacterRequest updateReq = new UpdateCharacterRequest();
        updateReq.setLevel(negativeLevel);
        
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> 
                characterService.updateCharacter(userId, created.getId(), updateReq))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("level");
    }
    
    // Feature: kihongan-raid-system, Property 10: Optional fields accepted
    @Property(tries = 100)
    void optionalFieldsAccepted(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a character with only required fields (name)
        CreateCharacterRequest req = new CreateCharacterRequest(
                name, null, null, null, null, false
        );
        
        // THEN it should succeed
        Character created = characterService.createCharacter(userId, req);
        
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(name);
        assertThat(created.getJob()).isNull();
        assertThat(created.getLevel()).isNull();
        assertThat(created.getGameId()).isNull();
        assertThat(created.getNote()).isNull();
    }
    
    // Feature: kihongan-raid-system, Property 21: Character with signups cannot be deleted
    @Property(tries = 50)
    void characterWithSignupsCannotBeDeleted(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String charName,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String raidTitle) {
        
        // GIVEN a user with a character
        createTestUser(userId, "U" + userId);
        CreateCharacterRequest req = new CreateCharacterRequest(
                charName, "Job", 50, null, null, false
        );
        Character character = characterService.createCharacter(userId, req);
        
        // AND the character has a signup
        jdbcTemplate.update(
                "INSERT INTO raids (title, start_time, created_by, created_at) VALUES (?, DATEADD('DAY', 1, CURRENT_TIMESTAMP), ?, CURRENT_TIMESTAMP)",
                raidTitle,
                userId
        );
        
        Long raidId = jdbcTemplate.queryForObject(
                "SELECT id FROM raids WHERE title = ?",
                Long.class,
                raidTitle
        );
        
        jdbcTemplate.update(
                "INSERT INTO raid_signups (raid_id, character_id, status) VALUES (?, ?, ?)",
                raidId,
                character.getId(),
                "confirmed"
        );
        
        // WHEN attempting to delete the character
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> 
                characterService.deleteCharacter(userId, character.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("signup");
    }
    
    // Feature: kihongan-raid-system, Property 30: Default character transaction atomicity
    @Property(tries = 50)
    void defaultCharacterTransactionAtomicity(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @IntRange(min = 2, max = 4) int characterCount) {
        
        // GIVEN a user with multiple characters, one already default
        createTestUser(userId, "U" + userId);
        
        Character firstChar = null;
        Character secondChar = null;
        
        for (int i = 0; i < characterCount; i++) {
            CreateCharacterRequest req = new CreateCharacterRequest(
                    "Char" + i, "Job", 50, null, null, i == 0
            );
            Character created = characterService.createCharacter(userId, req);
            if (i == 0) firstChar = created;
            if (i == 1) secondChar = created;
        }
        
        // Verify first is default
        Character first = characterRepository.findById(firstChar.getId()).orElseThrow();
        assertThat(first.getIsDefault()).isTrue();
        
        // WHEN setting another as default
        characterService.setAsDefault(userId, secondChar.getId());
        
        // THEN exactly one should be default (atomicity)
        var allChars = characterService.getCharactersByUserId(userId);
        long defaultCount = allChars.stream().filter(Character::getIsDefault).count();
        assertThat(defaultCount).isEqualTo(1);
        
        // AND the new one should be default
        Character second = characterRepository.findById(secondChar.getId()).orElseThrow();
        assertThat(second.getIsDefault()).isTrue();
        
        // AND the old one should not be default
        Character firstUpdated = characterRepository.findById(firstChar.getId()).orElseThrow();
        assertThat(firstUpdated.getIsDefault()).isFalse();
    }
    
    // Arbitraries
    
    @Provide
    Arbitrary<String> emptyOrWhitespaceStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n"),
                Arbitraries.just("  \t  \n  ")
        );
    }
    
    // Helper method
    private void createTestUser(Long userId, String lineUserId) {
        jdbcTemplate.update(
                "MERGE INTO users (id, line_user_id, name) KEY(id) VALUES (?, ?, ?)",
                userId, lineUserId, "User" + userId
        );
    }
}
