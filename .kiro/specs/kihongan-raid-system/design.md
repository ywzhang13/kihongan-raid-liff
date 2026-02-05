# Design Document: KiHongan Raid System Phase 2

## Overview

The KiHongan Raid System Phase 2 builds upon the existing LINE authentication foundation to provide character management, raid event creation, and signup functionality. The system follows a RESTful API architecture with JWT-based authentication, using Spring Boot for the backend and LIFF SDK for the frontend.

The design emphasizes:
- Clear separation between authentication, business logic, and data access layers
- Transactional consistency for multi-step operations
- Authorization checks to ensure users can only access their own resources
- Comprehensive validation at both API and database levels

## Architecture

### System Components

```
┌─────────────────┐
│  LINE Platform  │
│   (LIFF SDK)    │
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────┐
│   Frontend      │
│  (Vercel)       │
│  - Vue3/JS      │
│  - LIFF SDK v2  │
└────────┬────────┘
         │ REST API (JWT)
         ▼
┌─────────────────┐
│   Backend       │
│  (Spring Boot)  │
│  - Controllers  │
│  - Services     │
│  - Repositories │
│  - JWT Filter   │
└────────┬────────┘
         │ JDBC
         ▼
┌─────────────────┐
│   PostgreSQL    │
│   (Supabase)    │
└─────────────────┘
```

### Layer Responsibilities

**Controller Layer**:
- HTTP request/response handling
- Input validation (basic format checks)
- JWT token extraction
- HTTP status code mapping

**Service Layer**:
- Business logic implementation
- Authorization checks (user owns resource)
- Transaction management
- Complex validation rules
- Coordination between repositories

**Repository Layer**:
- Database operations (CRUD)
- SQL query execution
- Result set mapping to domain objects
- No business logic

**Security Layer**:
- JWT token validation (signature, expiration)
- User identity extraction from token
- Request filtering and authentication

## Components and Interfaces

### 1. Character Management

#### CharacterController
```java
@RestController
@RequestMapping("/me/characters")
public class CharacterController {
    
    @GetMapping
    ResponseEntity<List<CharacterDTO>> getMyCharacters(@AuthUser Long userId);
    
    @PostMapping
    ResponseEntity<CharacterDTO> createCharacter(
        @AuthUser Long userId, 
        @RequestBody CreateCharacterRequest request
    );
    
    @PutMapping("/{id}")
    ResponseEntity<CharacterDTO> updateCharacter(
        @AuthUser Long userId,
        @PathVariable Long id,
        @RequestBody UpdateCharacterRequest request
    );
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCharacter(
        @AuthUser Long userId,
        @PathVariable Long id
    );
}
```

#### CharacterService
```java
@Service
public class CharacterService {
    
    List<Character> getCharactersByUserId(Long userId);
    
    Character createCharacter(Long userId, CreateCharacterRequest request);
    
    Character updateCharacter(Long userId, Long characterId, UpdateCharacterRequest request);
    
    void deleteCharacter(Long userId, Long characterId);
    
    // Authorization helper
    private void validateOwnership(Long userId, Long characterId);
    
    // Default character management
    @Transactional
    private void setAsDefault(Long userId, Long characterId);
}
```

#### CharacterRepository
```java
@Repository
public class CharacterRepository {
    
    List<Character> findByUserId(Long userId);
    
    Optional<Character> findById(Long id);
    
    Character save(Character character);
    
    void deleteById(Long id);
    
    void unsetDefaultForUser(Long userId);
    
    boolean existsByIdAndUserId(Long id, Long userId);
    
    boolean hasActiveSignups(Long characterId);
}
```

### 2. Raid Management

#### RaidController
```java
@RestController
@RequestMapping("/raids")
public class RaidController {
    
    @GetMapping
    ResponseEntity<List<RaidDTO>> listRaids();
    
    @PostMapping
    ResponseEntity<RaidDTO> createRaid(
        @AuthUser Long userId,
        @RequestBody CreateRaidRequest request
    );
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteRaid(
        @AuthUser Long userId,
        @PathVariable Long id
    );
    
    @GetMapping("/{id}/signups")
    ResponseEntity<List<SignupDTO>> getRaidSignups(@PathVariable Long id);
}
```

#### RaidService
```java
@Service
public class RaidService {
    
    List<Raid> listRaids();
    
    Raid createRaid(Long creatorUserId, CreateRaidRequest request);
    
    @Transactional
    void deleteRaid(Long raidId);
    
    List<SignupWithDetails> getRaidSignups(Long raidId);
    
    // Validation helpers
    private void validateRaidData(CreateRaidRequest request);
}
```

#### RaidRepository
```java
@Repository
public class RaidRepository {
    
    List<Raid> findAllOrderByStartTime();
    
    Optional<Raid> findById(Long id);
    
    Raid save(Raid raid);
    
    void deleteById(Long id);
}
```

### 3. Signup Management

#### SignupController
```java
@RestController
@RequestMapping("/raids/{raidId}/signup")
public class SignupController {
    
    @PostMapping
    ResponseEntity<SignupDTO> signupForRaid(
        @AuthUser Long userId,
        @PathVariable Long raidId,
        @RequestBody SignupRequest request
    );
}
```

#### SignupService
```java
@Service
public class SignupService {
    
    Signup createSignup(Long userId, Long raidId, Long characterId);
    
    // Validation helpers
    private void validateCharacterOwnership(Long userId, Long characterId);
    private void validateNoDuplicateSignup(Long raidId, Long characterId);
    private void validateRaidExists(Long raidId);
}
```

#### SignupRepository
```java
@Repository
public class SignupRepository {
    
    Signup save(Signup signup);
    
    List<SignupWithDetails> findByRaidIdWithDetails(Long raidId);
    
    boolean existsByRaidIdAndCharacterId(Long raidId, Long characterId);
    
    @Transactional
    void deleteByRaidId(Long raidId);
}
```

### 4. Security Components

#### JwtAuthenticationFilter
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) {
        // Extract Bearer token from Authorization header
        // Validate JWT signature and expiration
        // Extract userId and lineUserId from claims
        // Set authentication in SecurityContext
        // Continue filter chain
    }
}
```

#### JwtService
```java
@Service
public class JwtService {
    
    Claims validateToken(String token);
    
    Long extractUserId(Claims claims);
    
    String extractLineUserId(Claims claims);
}
```

#### @AuthUser Annotation
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
```

#### AuthUserArgumentResolver
```java
@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class);
    }
    
    @Override
    public Object resolveArgument(...) {
        // Extract userId from SecurityContext
        // Return userId for injection into controller methods
    }
}
```

## Data Models

### Character
```java
public class Character {
    private Long id;
    private Long userId;
    private String name;
    private String job;
    private Integer level;
    private String gameId;
    private String note;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Raid
```java
public class Raid {
    private Long id;
    private String title;
    private String subtitle;
    private String boss;
    private Instant startTime;
    private Long createdBy;
    private Instant createdAt;
}
```

### Signup
```java
public class Signup {
    private Long id;
    private Long raidId;
    private Long characterId;
    private String status;
    private Instant createdAt;
}
```

### SignupWithDetails (Query Result)
```java
public class SignupWithDetails {
    private Long signupId;
    private Long characterId;
    private String characterName;
    private String job;
    private Integer level;
    private Long userId;
    private String userName;
    private String userPicture;
    private String status;
}
```

### DTOs

**CreateCharacterRequest**:
```java
{
    "name": "string (required)",
    "job": "string (optional)",
    "level": "integer (optional)",
    "gameId": "string (optional)",
    "note": "string (optional)",
    "isDefault": "boolean (optional, default: false)"
}
```

**UpdateCharacterRequest**:
```java
{
    "name": "string (optional)",
    "job": "string (optional)",
    "level": "integer (optional)",
    "gameId": "string (optional)",
    "note": "string (optional)",
    "isDefault": "boolean (optional)"
}
```

**CharacterDTO**:
```java
{
    "id": "long",
    "name": "string",
    "job": "string",
    "level": "integer",
    "gameId": "string",
    "note": "string",
    "isDefault": "boolean",
    "createdAt": "ISO-8601 timestamp",
    "updatedAt": "ISO-8601 timestamp"
}
```

**CreateRaidRequest**:
```java
{
    "title": "string (required)",
    "subtitle": "string (optional)",
    "boss": "string (optional)",
    "startTime": "ISO-8601 timestamp (required)"
}
```

**RaidDTO**:
```java
{
    "id": "long",
    "title": "string",
    "subtitle": "string",
    "boss": "string",
    "startTime": "ISO-8601 timestamp",
    "createdBy": "long",
    "createdAt": "ISO-8601 timestamp"
}
```

**SignupRequest**:
```java
{
    "characterId": "long (required)"
}
```

**SignupDTO**:
```java
{
    "id": "long",
    "characterId": "long",
    "characterName": "string",
    "job": "string",
    "level": "integer",
    "userId": "long",
    "userName": "string",
    "userPicture": "string",
    "status": "string"
}
```

## Correctness Properties


A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Character Management Properties

**Property 1: Character creation round-trip**
*For any* valid character data (name, job, level, gameId, note, isDefault), creating a character and then querying it should return the same data.
**Validates: Requirements 1.1**

**Property 2: Character list isolation**
*For any* set of users with characters, each user's character list should contain only their own characters and all of their characters.
**Validates: Requirements 1.2**

**Property 3: Partial update preservation**
*For any* character and any subset of updatable fields, updating only those fields should preserve all other field values unchanged.
**Validates: Requirements 1.3**

**Property 4: Character deletion removes access**
*For any* character, after deletion, querying for that character should return not found and the character should not appear in any character lists.
**Validates: Requirements 1.4**

**Property 5: Default character uniqueness**
*For any* user with multiple characters, setting one character as default should result in exactly one character being marked as default for that user.
**Validates: Requirements 1.5**

**Property 6: Cross-user character access denied**
*For any* two different users, attempting to update or delete a character owned by one user while authenticated as the other user should be rejected with authorization error.
**Validates: Requirements 1.6**

**Property 7: Character ownership assignment**
*For any* authenticated user creating a character, the created character's userId should match the authenticated user's ID from the JWT token.
**Validates: Requirements 2.3**

### Validation Properties

**Property 8: Empty required fields rejected**
*For any* character or raid creation/update with empty or whitespace-only required string fields (character name, raid title), the request should be rejected with validation error.
**Validates: Requirements 2.1, 4.1**

**Property 9: Invalid numeric values rejected**
*For any* character with a negative level value, the creation or update request should be rejected with validation error.
**Validates: Requirements 2.2**

**Property 10: Optional fields accepted**
*For any* character or raid, omitting optional fields (character: job, gameId, note; raid: subtitle, boss) should result in successful creation with those fields as null.
**Validates: Requirements 2.4, 4.4**

**Property 11: Past raid times rejected**
*For any* raid creation request with a start_time before the current time, the request should be rejected with validation error.
**Validates: Requirements 4.3**

**Property 12: Required raid fields enforced**
*For any* raid creation request missing title or start_time, the request should be rejected with validation error.
**Validates: Requirements 4.2**

### Raid Management Properties

**Property 13: Raid creation round-trip**
*For any* valid raid data (title, subtitle, boss, start_time), creating a raid and then querying it should return the same data with the creator's userId.
**Validates: Requirements 3.1, 3.5**

**Property 14: Raid list chronological ordering**
*For any* set of raids with different start times, the raid list should be ordered by start_time in ascending chronological order.
**Validates: Requirements 3.2**

**Property 15: Raid deletion cascades to signups**
*For any* raid with associated signups, deleting the raid should remove both the raid and all its signups in a single atomic operation.
**Validates: Requirements 3.3, 6.2**

### Signup Properties

**Property 16: Signup creates valid association**
*For any* valid user, character, and raid, creating a signup should result in a record linking that character to that raid with status 'confirmed'.
**Validates: Requirements 5.1, 5.7**

**Property 17: Owned character signup accepted**
*For any* user with a character, signing up for a raid with their own character should succeed.
**Validates: Requirements 5.2**

**Property 18: Unowned character signup rejected**
*For any* two different users, attempting to signup for a raid using a character owned by one user while authenticated as the other user should be rejected with authorization error.
**Validates: Requirements 5.3**

**Property 19: Duplicate signup rejected**
*For any* character and raid, if a signup already exists for that character-raid pair, attempting to create another signup with the same pair should be rejected.
**Validates: Requirements 5.4**

**Property 20: Signup list completeness**
*For any* raid with signups, querying the signup list should return all signups with complete character information (name, job, level) and user information (name, picture).
**Validates: Requirements 5.5**

### Referential Integrity Properties

**Property 21: Character with signups cannot be deleted**
*For any* character that has one or more active signups, attempting to delete that character should be rejected with an error indicating active signups exist.
**Validates: Requirements 6.1**

**Property 22: Signup requires valid references**
*For any* signup creation request with a non-existent raid ID or non-existent character ID, the request should be rejected with a not found error.
**Validates: Requirements 6.3, 6.4**

### Authentication and Authorization Properties

**Property 23: Protected endpoints require authentication**
*For any* protected endpoint (character operations, raid creation/deletion, signup operations), requests without a valid JWT token should be rejected with 401 Unauthorized.
**Validates: Requirements 1.7, 3.4, 5.6, 7.1**

**Property 24: Invalid tokens rejected**
*For any* request with a malformed, invalid signature, or expired JWT token, the request should be rejected with 401 Unauthorized.
**Validates: Requirements 7.2, 7.3**

**Property 25: Token claims extraction**
*For any* valid JWT token, the system should correctly extract user_id and line_user_id from the token payload and use them for authorization.
**Validates: Requirements 7.4**

### API Response Properties

**Property 26: Success responses well-formed**
*For any* successful API operation, the response should be valid JSON with an appropriate 2xx HTTP status code.
**Validates: Requirements 8.1**

**Property 27: Validation error responses**
*For any* API operation that fails validation, the response should be a 400 Bad Request with a JSON body containing error details.
**Validates: Requirements 8.2**

**Property 28: Authorization error responses**
*For any* API operation that fails authorization checks, the response should be a 403 Forbidden with a JSON body containing an error message.
**Validates: Requirements 8.4**

**Property 29: Not found error responses**
*For any* API operation requesting a non-existent resource, the response should be a 404 Not Found with a JSON body containing an error message.
**Validates: Requirements 8.5**

### Transaction Properties

**Property 30: Default character transaction atomicity**
*For any* user setting a character as default, both unsetting the previous default and setting the new default should occur atomically - either both succeed or both fail.
**Validates: Requirements 9.1**

### Timestamp Properties

**Property 31: Creation timestamps set automatically**
*For any* newly created character, raid, or signup, the created_at timestamp should be set to a value within a few seconds of the current time.
**Validates: Requirements 10.1, 10.3, 10.4**

**Property 32: Update timestamps refresh automatically**
*For any* character update operation, the updated_at timestamp should be changed to a value greater than the previous updated_at and within a few seconds of the current time.
**Validates: Requirements 10.2**

## Error Handling

### Validation Errors (400 Bad Request)
- Empty or whitespace-only required fields
- Negative numeric values where positive expected
- Missing required fields
- Invalid data formats (e.g., malformed timestamps)
- Past timestamps for future events
- Duplicate signups

**Response Format**:
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "name",
      "message": "Name cannot be empty"
    }
  ]
}
```

### Authentication Errors (401 Unauthorized)
- Missing Authorization header
- Missing Bearer token
- Invalid token signature
- Expired token
- Malformed token

**Response Format**:
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### Authorization Errors (403 Forbidden)
- Attempting to access another user's character
- Attempting to signup with another user's character
- Attempting to delete a character with active signups

**Response Format**:
```json
{
  "error": "Forbidden",
  "message": "You do not have permission to access this resource"
}
```

### Not Found Errors (404 Not Found)
- Requesting non-existent character
- Requesting non-existent raid
- Attempting signup with non-existent raid or character

**Response Format**:
```json
{
  "error": "Not found",
  "message": "Character not found"
}
```

### Server Errors (500 Internal Server Error)
- Database connection failures
- Unexpected exceptions
- Transaction rollback failures

**Response Format**:
```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred"
}
```

### Error Handling Strategy

1. **Controller Layer**: Catch exceptions and map to appropriate HTTP status codes
2. **Service Layer**: Throw domain-specific exceptions (ValidationException, AuthorizationException, NotFoundException)
3. **Global Exception Handler**: Use @ControllerAdvice to handle exceptions consistently
4. **Logging**: Log all errors with appropriate severity levels
5. **User-Friendly Messages**: Return clear error messages without exposing internal details

## Testing Strategy

### Dual Testing Approach

The testing strategy employs both unit tests and property-based tests as complementary approaches:

**Unit Tests**:
- Verify specific examples and edge cases
- Test integration points between components
- Test error conditions with specific inputs
- Validate API response formats
- Test database query correctness with known data

**Property-Based Tests**:
- Verify universal properties across all inputs
- Generate random valid and invalid inputs
- Test with minimum 100 iterations per property
- Catch edge cases that manual testing might miss
- Validate correctness properties from the design document

### Property-Based Testing Configuration

**Library**: Use **jqwik** for Java property-based testing (https://jqwik.net/)

**Configuration**:
- Minimum 100 iterations per property test (`@Property(tries = 100)`)
- Each test must reference its design document property in a comment
- Tag format: `// Feature: kihongan-raid-system, Property N: [property text]`

**Example**:
```java
@Property(tries = 100)
// Feature: kihongan-raid-system, Property 1: Character creation round-trip
void characterCreationRoundTrip(@ForAll("validCharacters") Character character) {
    // Test implementation
}
```

### Test Coverage Requirements

**Character Management**:
- Unit tests: Create, read, update, delete with specific examples
- Unit tests: Edge cases (empty strings, null values, boundary values)
- Property tests: Properties 1-7 (round-trip, isolation, preservation, deletion, uniqueness, authorization, ownership)

**Validation**:
- Unit tests: Specific validation failures with known inputs
- Property tests: Properties 8-12 (empty fields, invalid numbers, optional fields, past times, required fields)

**Raid Management**:
- Unit tests: CRUD operations with specific examples
- Unit tests: Cascade deletion scenarios
- Property tests: Properties 13-15 (round-trip, ordering, cascade deletion)

**Signup Management**:
- Unit tests: Signup creation and retrieval with specific examples
- Unit tests: Duplicate signup scenarios
- Property tests: Properties 16-22 (association, authorization, duplicates, completeness, referential integrity)

**Authentication/Authorization**:
- Unit tests: Token validation with specific valid/invalid tokens
- Unit tests: Endpoint protection with and without tokens
- Property tests: Properties 23-25 (authentication requirements, token validation, claims extraction)

**API Responses**:
- Unit tests: Specific error scenarios and response formats
- Property tests: Properties 26-29 (success responses, error responses)

**Transactions**:
- Unit tests: Specific transaction scenarios
- Property tests: Properties 30 (atomicity)

**Timestamps**:
- Unit tests: Specific timestamp scenarios
- Property tests: Properties 31-32 (automatic timestamps)

### Integration Testing

**Database Integration**:
- Test with actual PostgreSQL database (Testcontainers)
- Verify transaction behavior
- Test cascade operations
- Verify foreign key constraints

**API Integration**:
- Test full request/response cycle
- Verify JWT filter integration
- Test error handling end-to-end
- Verify CORS configuration

### Test Data Generation

**For Property-Based Tests**:
- Generate random valid characters (non-empty names, positive levels)
- Generate random valid raids (non-empty titles, future timestamps)
- Generate random invalid inputs (empty strings, negative numbers, past dates)
- Generate random user IDs for authorization testing
- Generate random JWT tokens (valid and invalid)

**For Unit Tests**:
- Use fixed test data for reproducibility
- Include edge cases (empty strings, null values, boundary values)
- Include specific error scenarios

### Testing Best Practices

1. **Isolation**: Each test should be independent and not rely on other tests
2. **Cleanup**: Use transactions or cleanup methods to reset database state
3. **Assertions**: Use clear, specific assertions with meaningful error messages
4. **Naming**: Use descriptive test names that explain what is being tested
5. **Documentation**: Comment complex test scenarios and property tests
6. **Performance**: Keep tests fast by using in-memory databases where appropriate
7. **Coverage**: Aim for high code coverage but focus on meaningful tests
