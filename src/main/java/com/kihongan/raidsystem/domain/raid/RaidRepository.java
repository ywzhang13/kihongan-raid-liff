package com.kihongan.raidsystem.domain.raid;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Raid entity using JDBC.
 * Handles all database operations for raids.
 */
@Repository
public class RaidRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public RaidRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<Raid> raidRowMapper = (rs, rowNum) -> {
        Raid raid = new Raid();
        raid.setId(rs.getLong("id"));
        raid.setTitle(rs.getString("title"));
        raid.setSubtitle(rs.getString("subtitle"));
        raid.setBoss(rs.getString("boss"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            raid.setStartTime(startTime.toInstant());
        }
        
        raid.setCreatedBy(rs.getLong("created_by"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            raid.setCreatedAt(createdAt.toInstant());
        }
        
        return raid;
    };
    
    /**
     * Finds all raids ordered by start time.
     * Only returns raids that haven't expired (start_time >= current time).
     */
    public List<Raid> findAllOrderByStartTime() {
        String sql = "SELECT * FROM raids WHERE start_time >= CURRENT_TIMESTAMP ORDER BY start_time ASC";
        return jdbcTemplate.query(sql, raidRowMapper);
    }
    
    /**
     * Finds a raid by ID.
     */
    public Optional<Raid> findById(Long id) {
        String sql = "SELECT * FROM raids WHERE id = ?";
        List<Raid> results = jdbcTemplate.query(sql, raidRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Saves a raid (insert only, raids are not updated).
     */
    public Raid save(Raid raid) {
        String sql = """
                INSERT INTO raids (title, subtitle, boss, start_time, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        
        Instant now = Instant.now();
        raid.setCreatedAt(now);
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, raid.getTitle());
            ps.setString(2, raid.getSubtitle());
            ps.setString(3, raid.getBoss());
            ps.setTimestamp(4, Timestamp.from(raid.getStartTime()));
            ps.setLong(5, raid.getCreatedBy());
            ps.setTimestamp(6, Timestamp.from(raid.getCreatedAt()));
            return ps;
        }, keyHolder);
        
        // PostgreSQL returns all columns, so we need to get the 'id' specifically
        Number key = (Number) keyHolder.getKeys().get("id");
        raid.setId(key.longValue());
        return raid;
    }
    
    /**
     * Deletes a raid by ID.
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM raids WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
