package com.kihongan.raidsystem.controller;

import com.kihongan.raidsystem.domain.character.Character;
import com.kihongan.raidsystem.domain.character.CharacterService;
import com.kihongan.raidsystem.domain.character.dto.CharacterDTO;
import com.kihongan.raidsystem.domain.character.dto.CreateCharacterRequest;
import com.kihongan.raidsystem.domain.character.dto.UpdateCharacterRequest;
import com.kihongan.raidsystem.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for character management endpoints.
 * Handles character CRUD operations with JWT authentication.
 */
@RestController
@RequestMapping("/me/characters")
public class CharacterController {
    
    private final CharacterService characterService;
    
    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }
    
    /**
     * GET /me/characters - List all characters for authenticated user
     */
    @GetMapping
    public ResponseEntity<List<CharacterDTO>> getMyCharacters(@AuthUser Long userId) {
        List<Character> characters = characterService.getCharactersByUserId(userId);
        List<CharacterDTO> dtos = characters.stream()
                .map(CharacterDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * POST /me/characters - Create a new character
     */
    @PostMapping
    public ResponseEntity<CharacterDTO> createCharacter(
            @AuthUser Long userId,
            @Valid @RequestBody CreateCharacterRequest request) {
        
        Character created = characterService.createCharacter(userId, request);
        CharacterDTO dto = CharacterDTO.fromEntity(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    
    /**
     * PUT /me/characters/{id} - Update an existing character
     */
    @PutMapping("/{id}")
    public ResponseEntity<CharacterDTO> updateCharacter(
            @AuthUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCharacterRequest request) {
        
        Character updated = characterService.updateCharacter(userId, id, request);
        CharacterDTO dto = CharacterDTO.fromEntity(updated);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * DELETE /me/characters/{id} - Delete a character
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacter(
            @AuthUser Long userId,
            @PathVariable Long id) {
        
        characterService.deleteCharacter(userId, id);
        return ResponseEntity.noContent().build();
    }
}
