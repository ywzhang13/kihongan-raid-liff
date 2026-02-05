package com.kihongan.raidsystem.domain.character;

import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.domain.character.dto.CreateCharacterRequest;
import com.kihongan.raidsystem.domain.character.dto.UpdateCharacterRequest;
import com.kihongan.raidsystem.exception.AuthorizationException;
import com.kihongan.raidsystem.exception.ValidationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for CharacterService.
 * Tests universal correctness properties with random inputs.
 */
@JqwikSpringSupport
class CharacterServicePropertyTest extends BaseIntegrationTest {
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    // Feature: kihongan-raid-system, Property 1: Character creation round-trip
    @Property(tries = 100)
    void characterCreationRoundTrip(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String job,
            @ForAll @IntRange(min = 1, max = 100) int level) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // AND a valid character request
        CreateCharacterRequest request = new CreateCharacterRequest(
                name, job, level, "GAME" + userId, "Test note", false
        );
        
        // WHEN creating the character
        Character created = characterService.createCharacter(userId, request);
        
        // THEN querying it should return the same data
        List<Character> characters = characterService.getCharactersByUserId(userId);
        assertThat(characters).hasSize(1);
        
        Character retrieved = characters.get(0);
        assertThat(retrieved.getName()).isEqualTo(name);
        assertThat(retrieved.getJob()).isEqualTo(job);
        assertThat(retrieved.getLevel()).isEqualTo(level);
        assertThat(retrieved.getUserId()).isEqualTo(userId);
    }
    
    // Feature: kihongan-raid-system, Property 2: Character list isolation
    @Property(tries = 100)
    void characterListIsolation(
            @ForAll @LongRange(min = 1, max = 5000) Long user1Id,
            @ForAll @LongRange(min = 5001, max = 10000) Long user2Id,
            @ForAll @IntRange(min = 1, max = 5) int user1CharCount,
            @ForAll @IntRange(min = 1, max = 5) int user2CharCount) {
        
        // GIVEN two users exist
        createTestUser(user1Id, "U" + user1Id);
        createTestUser(user2Id, "U" + user2Id);
        
        // AND each user has multiple characters
        for (int i = 0; i < user1CharCount; i++) {
            CreateCharacterRequest req = new CreateCharacterRequest(
                    "User1Char" + i, "Job", 50, null, null, false
            );
            characterService.createCharacter(user1Id, req);
        }
        
        for (int i = 0; i < user2CharCount; i++) {
            CreateCharacterRequest req = new CreateCharacterRequest(
                    "User2Char" + i, "Job", 50, null, null, false
            );
            characterService.createCharacter(user2Id, req);
        }
        
        // WHEN querying each user's characters
        List<Character> user1Chars = characterService.getCharactersByUserId(user1Id);
        List<Character> user2Chars = characterService.getCharactersByUserId(user2Id);
        
        // THEN each user should only see their own characters
        assertThat(user1Chars).hasSize(user1CharCount);
        assertThat(user2Chars).hasSize(user2CharCount);
        assertThat(user1Chars).allMatch(c -> c.getUserId().equals(user1Id));
        assertThat(user2Chars).allMatch(c -> c.getUserId().equals(user2Id));
    }
    
    // Feature: kihongan-raid-system, Property 3: Partial update preservation
    @Property(tries = 100)
    void partialUpdatePreservation(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String originalName,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String originalJob,
            @ForAll @IntRange(min = 1, max = 100) int originalLevel,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String newName) {
        
        // GIVEN a user with a character
        createTestUser(userId, "U" + userId);
        CreateCharacterRequest createReq = new CreateCharacterRequest(
                originalName, originalJob, originalLevel, "GAME123", "Note", false
        );
        Character created = characterService.createCharacter(userId, createReq);
        
        // WHEN updating only the name
        UpdateCharacterRequest updateReq = new UpdateCharacterRequest();
        updateReq.setName(newName);
        Character updated = characterService.updateCharacter(userId, created.getId(), updateReq);
        
        // THEN only the name should change, other fields preserved
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getJob()).isEqualTo(originalJob);
        assertThat(updated.getLevel()).isEqualTo(originalLevel);
        assertThat(updated.getGameId()).isEqualTo("GAME123");
        assertThat(updated.getNote()).isEqualTo("Note");
    }
    
    // Feature: kihongan-raid-system, Property 4: Character deletion removes access
    @Property(tries = 100)
    void characterDeletionRemovesAccess(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name) {
        
        // GIVEN a user with a character
        createTestUser(userId, "U" + userId);
        CreateCharacterRequest req = new CreateCharacterRequest(name, "Job", 50, null, null, false);
        Character created = characterService.createCharacter(userId, req);
        
        // WHEN deleting the character
        characterService.deleteCharacter(userId, created.getId());
        
        // THEN it should not appear in character lists
        List<Character> characters = characterService.getCharactersByUserId(userId);
        assertThat(characters).isEmpty();
        
        // AND querying by ID should return empty
        assertThat(characterRepository.findById(created.getId())).isEmpty();
    }
    
    // Feature: kihongan-raid-system, Property 5: Default character uniqueness
    @Property(tries = 100)
    void defaultCharacterUniqueness(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @IntRange(min = 2, max = 5) int characterCount) {
        
        // GIVEN a user with multiple characters
        createTestUser(userId, "U" + userId);
        
        for (int i = 0; i < characterCount; i++) {
            CreateCharacterRequest req = new CreateCharacterRequest(
                    "Char" + i, "Job", 50, null, null, false
            );
            characterService.createCharacter(userId, req);
        }
        
        List<Character> characters = characterService.getCharactersByUserId(userId);
        
        // WHEN setting one as default
        Character toSetDefault = characters.get(0);
        characterService.setAsDefault(userId, toSetDefault.getId());
        
        // THEN exactly one character should be marked as default
        List<Character> afterUpdate = characterService.getCharactersByUserId(userId);
        long defaultCount = afterUpdate.stream().filter(Character::getIsDefault).count();
        assertThat(defaultCount).isEqualTo(1);
        
        // AND it should be the one we set
        Character defaultChar = afterUpdate.stream()
                .filter(Character::getIsDefault)
                .findFirst()
                .orElseThrow();
        assertThat(defaultChar.getId()).isEqualTo(toSetDefault.getId());
    }
    
    // Feature: kihongan-raid-system, Property 6: Cross-user character access denied
    @Property(tries = 100)
    void crossUserCharacterAccessDenied(
            @ForAll @LongRange(min = 1, max = 5000) Long owner,
            @ForAll @LongRange(min = 5001, max = 10000) Long otherUser,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name) {
        
        // GIVEN two users and a character owned by one
        createTestUser(owner, "U" + owner);
        createTestUser(otherUser, "U" + otherUser);
        
        CreateCharacterRequest req = new CreateCharacterRequest(name, "Job", 50, null, null, false);
        Character created = characterService.createCharacter(owner, req);
        
        // WHEN the other user tries to update the character
        UpdateCharacterRequest updateReq = new UpdateCharacterRequest();
        updateReq.setName("Hacked");
        
        // THEN it should throw AuthorizationException
        assertThatThrownBy(() -> 
                characterService.updateCharacter(otherUser, created.getId(), updateReq))
                .isInstanceOf(AuthorizationException.class);
        
        // WHEN the other user tries to delete the character
        // THEN it should throw AuthorizationException
        assertThatThrownBy(() -> 
                characterService.deleteCharacter(otherUser, created.getId()))
                .isInstanceOf(AuthorizationException.class);
    }
    
    // Feature: kihongan-raid-system, Property 7: Character ownership assignment
    @Property(tries = 100)
    void characterOwnershipAssignment(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name) {
        
        // GIVEN an authenticated user
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a character
        CreateCharacterRequest req = new CreateCharacterRequest(name, "Job", 50, null, null, false);
        Character created = characterService.createCharacter(userId, req);
        
        // THEN the character's userId should match the authenticated user
        assertThat(created.getUserId()).isEqualTo(userId);
        
        // AND it should be retrievable by that user
        List<Character> characters = characterService.getCharactersByUserId(userId);
        assertThat(characters).anyMatch(c -> c.getId().equals(created.getId()));
    }
    
    // Helper method to create test user
    private void createTestUser(Long userId, String lineUserId) {
        jdbcTemplate.update(
                "MERGE INTO users (id, line_user_id, name) KEY(id) VALUES (?, ?, ?)",
                userId, lineUserId, "User" + userId
        );
    }
}
