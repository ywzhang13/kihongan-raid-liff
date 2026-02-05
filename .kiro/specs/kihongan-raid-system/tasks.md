# Implementation Plan: KiHongan Raid System Phase 2

## Overview

This implementation plan builds upon the existing Phase 1 foundation (LINE login, JWT authentication, users table) to add character management, raid creation/management, and signup functionality. The implementation follows a layered architecture with clear separation between controllers, services, and repositories. Each task builds incrementally, with testing integrated throughout to validate correctness early.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Add jqwik dependency for property-based testing
  - Add Testcontainers for database integration tests
  - Configure test database connection
  - Set up base test classes and utilities
  - _Requirements: All (foundation for testing)_

- [x] 2. Implement JWT authentication infrastructure
  - [x] 2.1 Create JwtService for token validation and claims extraction
    - Implement validateToken() method to verify signature and expiration
    - Implement extractUserId() and extractLineUserId() methods
    - _Requirements: 7.2, 7.3, 7.4_
  
  - [x] 2.2 Write property test for JWT token validation
    - **Property 24: Invalid tokens rejected**
    - **Property 25: Token claims extraction**
    - **Validates: Requirements 7.2, 7.3, 7.4**
  
  - [x] 2.3 Create JwtAuthenticationFilter for request filtering
    - Extract Bearer token from Authorization header
    - Validate token and set SecurityContext
    - Handle authentication errors with 401 responses
    - _Requirements: 7.1, 7.2, 7.3_
  
  - [x] 2.4 Create @AuthUser annotation and ArgumentResolver
    - Define @AuthUser annotation for controller parameters
    - Implement AuthUserArgumentResolver to extract userId from SecurityContext
    - Register resolver in WebMvcConfigurer
    - _Requirements: 7.4, 7.5_
  
  - [x] 2.5 Write unit tests for authentication filter
    - Test requests without tokens return 401
    - Test requests with invalid tokens return 401
    - Test requests with valid tokens pass through
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 3. Checkpoint - Verify authentication infrastructure
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement Character domain layer
  - [x] 4.1 Create Character entity and DTOs
    - Define Character class with all fields
    - Create CreateCharacterRequest DTO
    - Create UpdateCharacterRequest DTO
    - Create CharacterDTO for responses
    - _Requirements: 1.1, 2.3_
  
  - [x] 4.2 Create CharacterRepository with JDBC
    - Implement findByUserId() to query characters by user
    - Implement findById() to query single character
    - Implement save() for insert and update operations
    - Implement deleteById() for character deletion
    - Implement unsetDefaultForUser() for default management
    - Implement existsByIdAndUserId() for ownership checks
    - Implement hasActiveSignups() for deletion validation
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1_
  
  - [x] 4.3 Write unit tests for CharacterRepository
    - Test CRUD operations with specific examples
    - Test query methods return correct results
    - Test ownership validation queries
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 5. Implement Character service layer
  - [x] 5.1 Create CharacterService with business logic
    - Implement getCharactersByUserId() to list user's characters
    - Implement createCharacter() with validation
    - Implement updateCharacter() with ownership validation
    - Implement deleteCharacter() with signup check
    - Implement setAsDefault() with transaction management
    - Add validation for empty names and negative levels
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 6.1, 9.1_
  
  - [x] 5.2 Write property test for character creation round-trip
    - **Property 1: Character creation round-trip**
    - **Validates: Requirements 1.1**
  
  - [x] 5.3 Write property test for character list isolation
    - **Property 2: Character list isolation**
    - **Validates: Requirements 1.2**
  
  - [x] 5.4 Write property test for partial update preservation
    - **Property 3: Partial update preservation**
    - **Validates: Requirements 1.3**
  
  - [x] 5.5 Write property test for character deletion
    - **Property 4: Character deletion removes access**
    - **Validates: Requirements 1.4**
  
  - [x] 5.6 Write property test for default character uniqueness
    - **Property 5: Default character uniqueness**
    - **Validates: Requirements 1.5**
  
  - [x] 5.7 Write property test for cross-user access denial
    - **Property 6: Cross-user character access denied**
    - **Validates: Requirements 1.6**
  
  - [x] 5.8 Write property test for character ownership assignment
    - **Property 7: Character ownership assignment**
    - **Validates: Requirements 2.3**
  
  - [x] 5.9 Write property test for validation rules
    - **Property 8: Empty required fields rejected**
    - **Property 9: Invalid numeric values rejected**
    - **Property 10: Optional fields accepted**
    - **Validates: Requirements 2.1, 2.2, 2.4**
  
  - [x] 5.10 Write property test for character with signups deletion
    - **Property 21: Character with signups cannot be deleted**
    - **Validates: Requirements 6.1**
  
  - [x] 5.11 Write property test for default character transaction atomicity
    - **Property 30: Default character transaction atomicity**
    - **Validates: Requirements 9.1**

- [x] 6. Implement Character controller layer
  - [x] 6.1 Create CharacterController with REST endpoints
    - Implement GET /me/characters to list characters
    - Implement POST /me/characters to create character
    - Implement PUT /me/characters/{id} to update character
    - Implement DELETE /me/characters/{id} to delete character
    - Use @AuthUser to inject authenticated userId
    - Add @Valid annotations for request validation
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_
  
  - [x] 6.2 Write integration tests for character endpoints
    - Test full request/response cycle for all endpoints
    - Test authentication requirements
    - Test authorization for cross-user access
    - Test validation error responses
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 1.7_

- [x] 7. Checkpoint - Verify character management
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement Raid domain layer
  - [x] 8.1 Create Raid entity and DTOs
    - Define Raid class with all fields
    - Create CreateRaidRequest DTO
    - Create RaidDTO for responses
    - _Requirements: 3.1, 3.5_
  
  - [x] 8.2 Create RaidRepository with JDBC
    - Implement findAllOrderByStartTime() to list raids
    - Implement findById() to query single raid
    - Implement save() for insert operations
    - Implement deleteById() for raid deletion
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [x] 8.3 Write unit tests for RaidRepository
    - Test CRUD operations with specific examples
    - Test ordering by start_time
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 9. Implement Raid service layer
  - [x] 9.1 Create RaidService with business logic
    - Implement listRaids() to return all raids ordered by time
    - Implement createRaid() with validation
    - Implement deleteRaid() with cascade deletion
    - Add validation for empty title, null start_time, past times
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4_
  
  - [x] 9.2 Write property test for raid creation round-trip
    - **Property 13: Raid creation round-trip**
    - **Validates: Requirements 3.1, 3.5**
  
  - [x] 9.3 Write property test for raid list ordering
    - **Property 14: Raid list chronological ordering**
    - **Validates: Requirements 3.2**
  
  - [x] 9.4 Write property test for raid validation rules
    - **Property 11: Past raid times rejected**
    - **Property 12: Required raid fields enforced**
    - **Validates: Requirements 4.1, 4.2, 4.3**

- [x] 10. Implement Raid controller layer
  - [x] 10.1 Create RaidController with REST endpoints
    - Implement GET /raids to list all raids
    - Implement POST /raids to create raid
    - Implement DELETE /raids/{id} to delete raid
    - Use @AuthUser to inject authenticated userId for creation
    - Add @Valid annotations for request validation
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [x] 10.2 Write integration tests for raid endpoints
    - Test full request/response cycle for all endpoints
    - Test authentication requirements
    - Test validation error responses
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 11. Checkpoint - Verify raid management
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement Signup domain layer
  - [x] 12.1 Create Signup entity and DTOs
    - Define Signup class with all fields
    - Create SignupRequest DTO
    - Create SignupDTO for responses
    - Create SignupWithDetails class for query results
    - _Requirements: 5.1, 5.7_
  
  - [x] 12.2 Create SignupRepository with JDBC
    - Implement save() for signup creation
    - Implement findByRaidIdWithDetails() with JOIN query
    - Implement existsByRaidIdAndCharacterId() for duplicate check
    - Implement deleteByRaidId() for cascade deletion
    - _Requirements: 5.1, 5.4, 5.5, 6.2_
  
  - [x] 12.3 Write unit tests for SignupRepository
    - Test signup creation with specific examples
    - Test JOIN query returns complete details
    - Test duplicate detection
    - _Requirements: 5.1, 5.4, 5.5_

- [x] 13. Implement Signup service layer
  - [x] 13.1 Create SignupService with business logic
    - Implement createSignup() with validation
    - Implement getRaidSignups() to return signup list
    - Add validation for character ownership
    - Add validation for duplicate signups
    - Add validation for raid and character existence
    - Set default status to 'confirmed'
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.7, 6.3, 6.4_
  
  - [x] 13.2 Write property test for signup creation
    - **Property 16: Signup creates valid association**
    - **Validates: Requirements 5.1, 5.7**
  
  - [x] 13.3 Write property test for owned character signup
    - **Property 17: Owned character signup accepted**
    - **Validates: Requirements 5.2**
  
  - [x] 13.4 Write property test for unowned character signup
    - **Property 18: Unowned character signup rejected**
    - **Validates: Requirements 5.3**
  
  - [x] 13.5 Write property test for duplicate signup
    - **Property 19: Duplicate signup rejected**
    - **Validates: Requirements 5.4**
  
  - [x] 13.6 Write property test for signup list completeness
    - **Property 20: Signup list completeness**
    - **Validates: Requirements 5.5**
  
  - [x] 13.7 Write property test for signup referential integrity
    - **Property 22: Signup requires valid references**
    - **Validates: Requirements 6.3, 6.4**

- [x] 14. Implement Signup controller layer
  - [x] 14.1 Create SignupController with REST endpoints
    - Implement POST /raids/{raidId}/signup to create signup
    - Implement GET /raids/{raidId}/signups to list signups
    - Use @AuthUser to inject authenticated userId
    - Add @Valid annotations for request validation
    - _Requirements: 5.1, 5.5, 5.6_
  
  - [x] 14.2 Write integration tests for signup endpoints
    - Test full request/response cycle for all endpoints
    - Test authentication requirements
    - Test authorization for character ownership
    - Test duplicate signup rejection
    - _Requirements: 5.1, 5.3, 5.4, 5.5, 5.6_

- [x] 15. Implement raid deletion cascade
  - [x] 15.1 Update RaidService to cascade delete signups
    - Modify deleteRaid() to call SignupRepository.deleteByRaidId()
    - Wrap in @Transactional to ensure atomicity
    - _Requirements: 3.3, 6.2, 9.2_
  
  - [x] 15.2 Write property test for cascade deletion
    - **Property 15: Raid deletion cascades to signups**
    - **Validates: Requirements 3.3, 6.2**

- [x] 16. Checkpoint - Verify signup functionality
  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. Implement global error handling
  - [x] 17.1 Create custom exception classes
    - Create ValidationException for validation errors
    - Create AuthorizationException for authorization errors
    - Create NotFoundException for missing resources
    - _Requirements: 8.2, 8.4, 8.5_
  
  - [x] 17.2 Create GlobalExceptionHandler with @ControllerAdvice
    - Handle ValidationException → 400 Bad Request
    - Handle AuthenticationException → 401 Unauthorized
    - Handle AuthorizationException → 403 Forbidden
    - Handle NotFoundException → 404 Not Found
    - Handle generic Exception → 500 Internal Server Error
    - Return consistent JSON error responses
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_
  
  - [x] 17.3 Write property tests for error responses
    - **Property 26: Success responses well-formed**
    - **Property 27: Validation error responses**
    - **Property 28: Authorization error responses**
    - **Property 29: Not found error responses**
    - **Validates: Requirements 8.1, 8.2, 8.4, 8.5**
  
  - [x] 17.4 Write unit tests for exception handler
    - Test each exception type maps to correct status code
    - Test error response format consistency
    - _Requirements: 8.2, 8.3, 8.4, 8.5_

- [x] 18. Implement timestamp management
  - [x] 18.1 Add timestamp handling to repositories
    - Set created_at on insert operations
    - Set updated_at on update operations
    - Use Instant.now() for current timestamp
    - _Requirements: 10.1, 10.2, 10.3, 10.4_
  
  - [x] 18.2 Write property tests for timestamps
    - **Property 31: Creation timestamps set automatically**
    - **Property 32: Update timestamps refresh automatically**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4**

- [x] 19. Implement authentication requirement enforcement
  - [x] 19.1 Configure security filter chain
    - Register JwtAuthenticationFilter in security configuration
    - Configure protected endpoints to require authentication
    - Configure public endpoints (if any)
    - _Requirements: 1.7, 3.4, 5.6, 7.1_
  
  - [x] 19.2 Write property test for authentication requirements
    - **Property 23: Protected endpoints require authentication**
    - **Validates: Requirements 1.7, 3.4, 5.6, 7.1**

- [x] 20. Final integration and testing
  - [x] 20.1 Run full test suite
    - Execute all unit tests
    - Execute all property-based tests
    - Execute all integration tests
    - Verify test coverage meets requirements
    - _Requirements: All_
  
  - [x] 20.2 Manual API testing
    - Test character CRUD flow with Postman/curl
    - Test raid CRUD flow
    - Test signup flow
    - Test error scenarios
    - Verify JWT authentication works end-to-end
    - _Requirements: All_

- [x] 21. Final checkpoint - Complete Phase 2
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All tasks are required for comprehensive implementation
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout implementation
- Property tests validate universal correctness properties with 100+ iterations
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end API behavior
- The implementation builds incrementally: authentication → characters → raids → signups → error handling
