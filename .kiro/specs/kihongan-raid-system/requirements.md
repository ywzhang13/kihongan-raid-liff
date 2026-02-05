# Requirements Document

## Introduction

The KiHongan Raid System is a LINE-based raid signup application that enables users to manage game characters and participate in raid events. This document specifies the requirements for Phase 2, which focuses on character management, raid creation/management, and signup functionality. The system builds upon the completed Phase 1 foundation (LINE login, JWT authentication, and user management).

## Glossary

- **System**: The KiHongan Raid System backend and frontend application
- **User**: An authenticated person who has logged in via LINE LIFF
- **Admin**: A user with elevated privileges to create and manage raids
- **Character**: A game character profile belonging to a user
- **Raid**: A scheduled game event that users can sign up for
- **Signup**: A registration record linking a character to a raid event
- **JWT**: JSON Web Token used for authenticating API requests
- **LIFF**: LINE Front-end Framework for building LINE-integrated web apps
- **Default_Character**: The character marked as the user's primary character

## Requirements

### Requirement 1: Character Management

**User Story:** As a user, I want to manage multiple game characters, so that I can participate in raids with different character profiles.

#### Acceptance Criteria

1. WHEN a user creates a character, THE System SHALL store the character with name, job, level, game_id, note, and is_default flag
2. WHEN a user requests their character list, THE System SHALL return all characters belonging to that user
3. WHEN a user updates a character, THE System SHALL modify only the specified character fields and preserve other data
4. WHEN a user deletes a character, THE System SHALL remove the character and prevent it from being used in future signups
5. WHEN a user sets a character as default, THE System SHALL unset any previously default character for that user
6. WHEN a user attempts to access another user's character, THE System SHALL reject the request with an authorization error
7. THE System SHALL require valid JWT authentication for all character operations

### Requirement 2: Character Data Validation

**User Story:** As a system administrator, I want character data to be validated, so that the database maintains data integrity.

#### Acceptance Criteria

1. WHEN a character is created or updated with an empty name, THE System SHALL reject the request
2. WHEN a character is created or updated with a negative level, THE System SHALL reject the request
3. WHEN a character is created, THE System SHALL associate it with the authenticated user from the JWT token
4. THE System SHALL allow optional fields (job, game_id, note) to be null or empty

### Requirement 3: Raid Creation and Management

**User Story:** As an admin, I want to create and manage raid events, so that users can sign up for scheduled activities.

#### Acceptance Criteria

1. WHEN an admin creates a raid, THE System SHALL store the raid with title, subtitle, boss, start_time, and created_by fields
2. WHEN any user requests the raid list, THE System SHALL return all raids ordered by start_time
3. WHEN an admin deletes a raid, THE System SHALL remove the raid and all associated signups
4. THE System SHALL require valid JWT authentication for raid creation and deletion
5. WHEN a raid is created, THE System SHALL associate it with the authenticated user as the creator

### Requirement 4: Raid Data Validation

**User Story:** As a system administrator, I want raid data to be validated, so that all raids have required information.

#### Acceptance Criteria

1. WHEN a raid is created with an empty title, THE System SHALL reject the request
2. WHEN a raid is created with a null start_time, THE System SHALL reject the request
3. WHEN a raid is created with a start_time in the past, THE System SHALL reject the request
4. THE System SHALL allow optional fields (subtitle, boss) to be null or empty

### Requirement 5: Raid Signup

**User Story:** As a user, I want to sign up for raids with my characters, so that I can participate in scheduled events.

#### Acceptance Criteria

1. WHEN a user signs up for a raid, THE System SHALL create a signup record linking the specified character to the raid
2. WHEN a user signs up with a character they own, THE System SHALL accept the signup
3. WHEN a user attempts to sign up with a character they do not own, THE System SHALL reject the request
4. WHEN a user signs up for a raid with the same character twice, THE System SHALL reject the duplicate signup
5. WHEN a user requests signups for a raid, THE System SHALL return all signup records with character and user information
6. THE System SHALL require valid JWT authentication for signup operations
7. WHEN a signup is created, THE System SHALL set the status to 'confirmed' by default

### Requirement 6: Signup Data Integrity

**User Story:** As a system administrator, I want signup data to maintain referential integrity, so that orphaned records are prevented.

#### Acceptance Criteria

1. WHEN a character is deleted, THE System SHALL prevent deletion if the character has active signups
2. WHEN a raid is deleted, THE System SHALL cascade delete all associated signups
3. WHEN a signup is created for a non-existent raid, THE System SHALL reject the request
4. WHEN a signup is created for a non-existent character, THE System SHALL reject the request

### Requirement 7: JWT Authentication and Authorization

**User Story:** As a system administrator, I want all API endpoints to be secured, so that only authenticated users can access protected resources.

#### Acceptance Criteria

1. WHEN a request is made without a JWT token, THE System SHALL reject the request with a 401 Unauthorized error
2. WHEN a request is made with an invalid JWT token, THE System SHALL reject the request with a 401 Unauthorized error
3. WHEN a request is made with an expired JWT token, THE System SHALL reject the request with a 401 Unauthorized error
4. WHEN a JWT token is validated, THE System SHALL extract the user_id and line_user_id from the token payload
5. THE System SHALL use the extracted user_id to authorize resource access

### Requirement 8: API Response Format

**User Story:** As a frontend developer, I want consistent API response formats, so that I can reliably parse responses.

#### Acceptance Criteria

1. WHEN an API operation succeeds, THE System SHALL return a JSON response with appropriate HTTP status code
2. WHEN an API operation fails due to validation, THE System SHALL return a 400 Bad Request with error details
3. WHEN an API operation fails due to authentication, THE System SHALL return a 401 Unauthorized with error message
4. WHEN an API operation fails due to authorization, THE System SHALL return a 403 Forbidden with error message
5. WHEN an API operation fails due to resource not found, THE System SHALL return a 404 Not Found with error message
6. WHEN an API operation fails due to server error, THE System SHALL return a 500 Internal Server Error with error message

### Requirement 9: Database Transaction Management

**User Story:** As a system administrator, I want database operations to be transactional, so that data consistency is maintained.

#### Acceptance Criteria

1. WHEN a character is set as default, THE System SHALL update both the new default and unset the old default in a single transaction
2. WHEN a raid is deleted with signups, THE System SHALL delete all signups and the raid in a single transaction
3. IF any database operation fails, THEN THE System SHALL rollback all changes in that transaction

### Requirement 10: Timestamp Management

**User Story:** As a system administrator, I want automatic timestamp tracking, so that I can audit when records are created and modified.

#### Acceptance Criteria

1. WHEN a character is created, THE System SHALL set the created_at timestamp to the current time
2. WHEN a character is updated, THE System SHALL update the updated_at timestamp to the current time
3. WHEN a raid is created, THE System SHALL set the created_at timestamp to the current time
4. WHEN a signup is created, THE System SHALL set the created_at timestamp to the current time
