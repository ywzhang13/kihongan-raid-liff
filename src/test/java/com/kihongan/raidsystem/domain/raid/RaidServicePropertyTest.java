package com.kihongan.raidsystem.domain.raid;

import com.kihongan.raidsystem.BaseIntegrationTest;
import com.kihongan.raidsystem.domain.raid.dto.CreateRaidRequest;
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
 * Property-based tests for RaidService.
 * Tests universal correctness properties with random inputs.
 */
@JqwikSpringSupport
class RaidServicePropertyTest extends BaseIntegrationTest {
    
    @Autowired
    private RaidService raidService;
    
    @Autowired
    private RaidRepository raidRepository;
    
    // Feature: kihongan-raid-system, Property 13: Raid creation round-trip
    @Property(tries = 100)
    void raidCreationRoundTrip(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String subtitle,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String boss,
            @ForAll @IntRange(min = 1, max = 365) int daysInFuture) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // AND a valid raid request
        Instant startTime = Instant.now().plus(daysInFuture, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(title, subtitle, boss, startTime);
        
        // WHEN creating the raid
        Raid created = raidService.createRaid(userId, request);
        
        // THEN querying it should return the same data with creator's userId
        List<Raid> raids = raidService.listRaids();
        assertThat(raids).anyMatch(r -> r.getId().equals(created.getId()));
        
        Raid retrieved = raidRepository.findById(created.getId()).orElseThrow();
        assertThat(retrieved.getTitle()).isEqualTo(title);
        assertThat(retrieved.getSubtitle()).isEqualTo(subtitle);
        assertThat(retrieved.getBoss()).isEqualTo(boss);
        assertThat(retrieved.getCreatedBy()).isEqualTo(userId);
        assertThat(retrieved.getStartTime()).isEqualTo(startTime);
    }
    
    // Feature: kihongan-raid-system, Property 14: Raid list chronological ordering
    @Property(tries = 100)
    void raidListChronologicalOrdering(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @IntRange(min = 2, max = 5) int raidCount) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // AND multiple raids with different start times
        Instant baseTime = Instant.now().plus(1, ChronoUnit.DAYS);
        
        for (int i = 0; i < raidCount; i++) {
            // Create raids in random order but with sequential times
            int daysOffset = raidCount - i; // Reverse order
            Instant startTime = baseTime.plus(daysOffset, ChronoUnit.DAYS);
            CreateRaidRequest req = new CreateRaidRequest(
                    "Raid" + i, "Sub", "Boss", startTime
            );
            raidService.createRaid(userId, req);
        }
        
        // WHEN querying all raids
        List<Raid> raids = raidService.listRaids();
        
        // THEN they should be ordered by start_time ascending
        assertThat(raids).hasSizeGreaterThanOrEqualTo(raidCount);
        
        // Check that our raids are in chronological order
        for (int i = 0; i < raids.size() - 1; i++) {
            Instant current = raids.get(i).getStartTime();
            Instant next = raids.get(i + 1).getStartTime();
            assertThat(current).isBeforeOrEqualTo(next);
        }
    }
    
    // Feature: kihongan-raid-system, Property 11: Past raid times rejected
    @Property(tries = 100)
    void pastRaidTimesRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title,
            @ForAll @IntRange(min = 1, max = 365) int daysInPast) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a raid with past start time
        Instant pastTime = Instant.now().minus(daysInPast, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(title, "Sub", "Boss", pastTime);
        
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> raidService.createRaid(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("past");
    }
    
    // Feature: kihongan-raid-system, Property 12: Required raid fields enforced
    @Property(tries = 100)
    void requiredRaidFieldsEnforced(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll("emptyOrWhitespaceStrings") String emptyTitle) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a raid with empty title
        Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(emptyTitle, "Sub", "Boss", futureTime);
        
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> raidService.createRaid(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }
    
    @Property(tries = 100)
    void nullStartTimeRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String title) {
        
        // GIVEN a user exists
        createTestUser(userId, "U" + userId);
        
        // WHEN creating a raid with null start_time
        CreateRaidRequest request = new CreateRaidRequest(title, "Sub", "Boss", null);
        
        // THEN it should throw ValidationException
        assertThatThrownBy(() -> raidService.createRaid(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("start time");
    }
    
    // Feature: kihongan-raid-system, Property 15: Raid deletion cascades to signups
    @Property(tries = 50)
    void raidDeletionCascadesToSignups(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String raidTitle,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String charName) {
        
        // GIVEN a user with a character
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
        
        // AND a raid with a signup
        Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        CreateRaidRequest request = new CreateRaidRequest(raidTitle, "Sub", "Boss", futureTime);
        Raid raid = raidService.createRaid(userId, request);
        
        jdbcTemplate.update(
                "INSERT INTO raid_signups (raid_id, character_id, status) VALUES (?, ?, ?)",
                raid.getId(), charId, "confirmed"
        );
        
        // Verify signup exists
        Integer signupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM raid_signups WHERE raid_id = ?",
                Integer.class,
                raid.getId()
        );
        assertThat(signupCount).isEqualTo(1);
        
        // WHEN deleting the raid
        raidService.deleteRaid(raid.getId());
        
        // THEN both raid and signups should be deleted (atomic operation)
        assertThat(raidRepository.findById(raid.getId())).isEmpty();
        
        Integer remainingSignups = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM raid_signups WHERE raid_id = ?",
                Integer.class,
                raid.getId()
        );
        assertThat(remainingSignups).isEqualTo(0);
    }
    
    // Arbitraries
    
    @Provide
    Arbitrary<String> emptyOrWhitespaceStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n")
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
