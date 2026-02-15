package com.kihongan.raidsystem.domain.signup;

import com.kihongan.raidsystem.domain.character.Character;
import com.kihongan.raidsystem.domain.character.CharacterRepository;
import com.kihongan.raidsystem.domain.raid.RaidRepository;
import com.kihongan.raidsystem.exception.AuthorizationException;
import com.kihongan.raidsystem.exception.NotFoundException;
import com.kihongan.raidsystem.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for Signup business logic.
 * Handles validation and authorization for raid signups.
 */
@Service
public class SignupService {
    
    private final SignupRepository signupRepository;
    private final CharacterRepository characterRepository;
    private final RaidRepository raidRepository;
    
    public SignupService(SignupRepository signupRepository,
                        CharacterRepository characterRepository,
                        RaidRepository raidRepository) {
        this.signupRepository = signupRepository;
        this.characterRepository = characterRepository;
        this.raidRepository = raidRepository;
    }
    
    /**
     * Creates a signup with validation.
     */
    public Signup createSignup(Long userId, Long raidId, Long characterId) {
        return createSignupInternal(userId, raidId, characterId, true);
    }
    
    /**
     * Creates a signup without notification (for auto-signup when creating raid).
     */
    public Signup createSignupWithoutNotification(Long userId, Long raidId, Long characterId) {
        return createSignupInternal(userId, raidId, characterId, false);
    }
    
    /**
     * Internal method to create signup with optional notification.
     */
    private Signup createSignupInternal(Long userId, Long raidId, Long characterId, boolean sendNotification) {
        // Validate raid exists
        validateRaidExists(raidId);
        
        // Validate character exists
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NotFoundException("Character not found"));
        
        // Validate character ownership
        validateCharacterOwnership(userId, characterId);
        
        // Validate no duplicate signup
        validateNoDuplicateSignup(raidId, characterId);
        
        // Validate raid capacity (max 6 people)
        validateRaidCapacity(raidId);
        
        // Create signup
        Signup signup = new Signup();
        signup.setRaidId(raidId);
        signup.setCharacterId(characterId);
        signup.setStatus("confirmed");
        
        Signup savedSignup = signupRepository.save(signup);
        
        return savedSignup;
    }
    
    /**
     * Gets all signups for a raid with complete details.
     */
    public List<SignupWithDetails> getRaidSignups(Long raidId) {
        return signupRepository.findByRaidIdWithDetails(raidId);
    }
    
    /**
     * Cancels a signup for a raid.
     */
    public void cancelSignup(Long userId, Long raidId) {
        // Validate raid exists
        validateRaidExists(raidId);
        
        // Find user's signup for this raid
        List<SignupWithDetails> signups = signupRepository.findByRaidIdWithDetails(raidId);
        SignupWithDetails userSignup = signups.stream()
                .filter(s -> s.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("You have not signed up for this raid"));
        
        // Delete the signup
        signupRepository.deleteById(userSignup.getSignupId());
    }
    
    // Validation helpers
    
    private void validateCharacterOwnership(Long userId, Long characterId) {
        if (!characterRepository.existsByIdAndUserId(characterId, userId)) {
            throw new AuthorizationException("You can only sign up with your own characters");
        }
    }
    
    private void validateNoDuplicateSignup(Long raidId, Long characterId) {
        if (signupRepository.existsByRaidIdAndCharacterId(raidId, characterId)) {
            throw new ValidationException("Character is already signed up for this raid");
        }
    }
    
    private void validateRaidExists(Long raidId) {
        if (!raidRepository.findById(raidId).isPresent()) {
            throw new NotFoundException("Raid not found");
        }
    }
    
    private void validateRaidCapacity(Long raidId) {
        List<SignupWithDetails> currentSignups = signupRepository.findByRaidIdWithDetails(raidId);
        if (currentSignups.size() >= 6) {
            throw new ValidationException("Raid is full (maximum 6 participants)");
        }
    }
}
