package com.kihongan.raidsystem.domain.character;

import com.kihongan.raidsystem.domain.character.dto.CreateCharacterRequest;
import com.kihongan.raidsystem.domain.character.dto.UpdateCharacterRequest;
import com.kihongan.raidsystem.exception.AuthorizationException;
import com.kihongan.raidsystem.exception.NotFoundException;
import com.kihongan.raidsystem.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Character business logic.
 * Handles validation, authorization, and transaction management.
 */
@Service
public class CharacterService {
    
    private final CharacterRepository characterRepository;
    
    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }
    
    /**
     * Gets all characters belonging to a user.
     */
    public List<Character> getCharactersByUserId(Long userId) {
        return characterRepository.findByUserId(userId);
    }
    
    /**
     * Creates a new character with validation.
     */
    @Transactional
    public Character createCharacter(Long userId, CreateCharacterRequest request) {
        // Validate required fields
        validateCharacterName(request.getName());
        validateLevel(request.getLevel());
        
        // Create character entity
        Character character = new Character();
        character.setUserId(userId);
        character.setName(request.getName().trim());
        character.setJob(request.getJob());
        character.setLevel(request.getLevel());
        character.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        
        // If setting as default, unset other defaults first
        if (character.getIsDefault()) {
            characterRepository.unsetDefaultForUser(userId);
        }
        
        return characterRepository.save(character);
    }
    
    /**
     * Updates an existing character with ownership validation.
     */
    @Transactional
    public Character updateCharacter(Long userId, Long characterId, UpdateCharacterRequest request) {
        // Validate ownership
        validateOwnership(userId, characterId);
        
        // Get existing character
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NotFoundException("Character not found"));
        
        // Validate and update fields if provided
        if (request.getName() != null) {
            validateCharacterName(request.getName());
            character.setName(request.getName().trim());
        }
        
        if (request.getLevel() != null) {
            validateLevel(request.getLevel());
            character.setLevel(request.getLevel());
        }
        
        if (request.getJob() != null) {
            character.setJob(request.getJob());
        }
        
        // Handle default flag change
        if (request.getIsDefault() != null) {
            if (request.getIsDefault() && !character.getIsDefault()) {
                // Setting as default - unset others first
                characterRepository.unsetDefaultForUser(userId);
            }
            character.setIsDefault(request.getIsDefault());
        }
        
        return characterRepository.save(character);
    }
    
    /**
     * Deletes a character with signup check.
     */
    @Transactional
    public void deleteCharacter(Long userId, Long characterId) {
        // Validate ownership
        validateOwnership(userId, characterId);
        
        // Check for active signups
        if (characterRepository.hasActiveSignups(characterId)) {
            throw new ValidationException("Cannot delete character with active raid signups");
        }
        
        characterRepository.deleteById(characterId);
    }
    
    /**
     * Sets a character as default with transaction management.
     */
    @Transactional
    public void setAsDefault(Long userId, Long characterId) {
        // Validate ownership
        validateOwnership(userId, characterId);
        
        // Unset all defaults for user
        characterRepository.unsetDefaultForUser(userId);
        
        // Set the specified character as default
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NotFoundException("Character not found"));
        character.setIsDefault(true);
        characterRepository.save(character);
    }
    
    // Validation helpers
    
    private void validateOwnership(Long userId, Long characterId) {
        if (!characterRepository.existsByIdAndUserId(characterId, userId)) {
            throw new AuthorizationException("You do not have permission to access this character");
        }
    }
    
    private void validateCharacterName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Character name cannot be empty");
        }
    }
    
    private void validateLevel(Integer level) {
        if (level != null && level < 0) {
            throw new ValidationException("Character level cannot be negative");
        }
    }
}
