package com.kihongan.raidsystem.domain.raid;

import com.kihongan.raidsystem.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RaidRepository.
 * Tests CRUD operations and query methods with specific examples.
 */
class RaidRepositoryTest extends BaseIntegrationTest {
    
    @Autowired
    private RaidRepository raidRepository;
    
    private Long testUserId;
    
    @BeforeEach
    void setUpTestData() {
        // Create test user
        testUserId = jdbcTemplate.queryForObject(
                "INSERT INTO users (line_user_id, name) VALUES (?, ?) RETURNING id",
                Long.class,
                "U123456789",
                "Test User"
        );
    }
    
    @Test
    void createRaid_shouldInsertAndReturnWithId() {
        // GIVEN a new raid
        Raid raid = new Raid();
        raid.setTitle("Dragon Raid");
        raid.setSubtitle("Hard Mode");
        raid.setBoss("Ancient Dragon");
        raid.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS));
        raid.setCreatedBy(testUserId);
        
        // WHEN saving the raid
        Raid saved = raidRepository.save(raid);
        
        // THEN it should have an ID and timestamp
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Dragon Raid");
        assertThat(saved.getSubtitle()).isEqualTo("Hard Mode");
        assertThat(saved.getBoss()).isEqualTo("Ancient Dragon");
    }
    
    @Test
    void findAllOrderByStartTime_shouldReturnRaidsInChronologicalOrder() {
        // GIVEN raids with different start times
        Instant now = Instant.now();
        
        Raid raid1 = createRaid("Raid 1", now.plus(3, ChronoUnit.DAYS));
        Raid raid2 = createRaid("Raid 2", now.plus(1, ChronoUnit.DAYS));
        Raid raid3 = createRaid("Raid 3", now.plus(2, ChronoUnit.DAYS));
        
        raidRepository.save(raid1);
        raidRepository.save(raid2);
        raidRepository.save(raid3);
        
        // WHEN querying all raids
        List<Raid> raids = raidRepository.findAllOrderByStartTime();
        
        // THEN they should be ordered by start_time ascending
        assertThat(raids).hasSize(3);
        assertThat(raids.get(0).getTitle()).isEqualTo("Raid 2"); // Day 1
        assertThat(raids.get(1).getTitle()).isEqualTo("Raid 3"); // Day 2
        assertThat(raids.get(2).getTitle()).isEqualTo("Raid 1"); // Day 3
    }
    
    @Test
    void findById_shouldReturnRaidWhenExists() {
        // GIVEN a saved raid
        Raid raid = createRaid("Test Raid", Instant.now().plus(1, ChronoUnit.DAYS));
        Raid saved = raidRepository.save(raid);
        
        // WHEN finding by ID
        Optional<Raid> found = raidRepository.findById(saved.getId());
        
        // THEN the raid should be found
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Raid");
    }
    
    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // WHEN finding a non-existent raid
        Optional<Raid> found = raidRepository.findById(99999L);
        
        // THEN it should return empty
        assertThat(found).isEmpty();
    }
    
    @Test
    void deleteById_shouldRemoveRaid() {
        // GIVEN a saved raid
        Raid raid = createRaid("To Delete", Instant.now().plus(1, ChronoUnit.DAYS));
        Raid saved = raidRepository.save(raid);
        
        // WHEN deleting the raid
        raidRepository.deleteById(saved.getId());
        
        // THEN it should no longer exist
        Optional<Raid> found = raidRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
    
    @Test
    void createRaid_withNullOptionalFields_shouldSucceed() {
        // GIVEN a raid with only required fields
        Raid raid = new Raid();
        raid.setTitle("Minimal Raid");
        raid.setSubtitle(null);
        raid.setBoss(null);
        raid.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS));
        raid.setCreatedBy(testUserId);
        
        // WHEN saving the raid
        Raid saved = raidRepository.save(raid);
        
        // THEN it should succeed with null optional fields
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Minimal Raid");
        assertThat(saved.getSubtitle()).isNull();
        assertThat(saved.getBoss()).isNull();
    }
    
    // Helper method
    private Raid createRaid(String title, Instant startTime) {
        Raid raid = new Raid();
        raid.setTitle(title);
        raid.setSubtitle("Subtitle");
        raid.setBoss("Boss");
        raid.setStartTime(startTime);
        raid.setCreatedBy(testUserId);
        return raid;
    }
}
