package com.kihongan.raidsystem.domain.signup;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Repository for Signup entity using JDBC.
 * Handles all database operations for raid signups.
 */
@Repository
public class SignupRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public SignupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<Signup> signupRowMapper = (rs, rowNum) -> {
        Signup signup = new Signup();
        signup.setId(rs.getLong("id"));
        signup.setRaidId(rs.getLong("raid_id"));
        signup.setCharacterId(rs.getLong("character_id"));
        signup.setStatus(rs.getString("status"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            signup.setCreatedAt(createdAt.toInstant());
        }
        
        return signup;
    };
    
    private final RowMapper<SignupWithDetails> signupWithDetailsRowMapper = (rs, rowNum) -> {
        SignupWithDetails details = new SignupWithDetails();
        details.setSignupId(rs.getLong("signup_id"));
        details.setCharacterId(rs.getLong("character_id"));
        details.setCharacterName(rs.getString("character_name"));
        details.setJob(rs.getString("job"));
        
        Integer level = (Integer) rs.getObject("level");
        details.setLevel(level);
        
        details.setUserId(rs.getLong("user_id"));
        details.setUserName(rs.getString("user_name"));
        details.setUserPicture(rs.getString("user_picture"));
        details.setStatus(rs.getString("status"));
        
        return details;
    };
    
    /**
     * Saves a signup (insert only).
     */
    public Signup save(Signup signup) {
        String sql = """
                INSERT INTO raid_signups (raid_id, character_id, status, created_at)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        
        Instant now = Instant.now();
        signup.setCreatedAt(now);
        
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                signup.getRaidId(),
                signup.getCharacterId(),
                signup.getStatus(),
                Timestamp.from(signup.getCreatedAt())
        );
        
        signup.setId(id);
        return signup;
    }
    
    /**
     * Finds all signups for a raid with complete character and user details.
     */
    public List<SignupWithDetails> findByRaidIdWithDetails(Long raidId) {
        String sql = """
                SELECT 
                    rs.id as signup_id,
                    c.id as character_id,
                    c.name as character_name,
                    c.job,
                    c.level,
                    u.id as user_id,
                    u.name as user_name,
                    u.picture as user_picture,
                    rs.status
                FROM raid_signups rs
                JOIN characters c ON rs.character_id = c.id
                JOIN users u ON c.user_id = u.id
                WHERE rs.raid_id = ?
                ORDER BY rs.created_at ASC
                """;
        
        return jdbcTemplate.query(sql, signupWithDetailsRowMapper, raidId);
    }
    
    /**
     * Checks if a signup already exists for a raid-character pair.
     */
    public boolean existsByRaidIdAndCharacterId(Long raidId, Long characterId) {
        String sql = "SELECT COUNT(*) FROM raid_signups WHERE raid_id = ? AND character_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, raidId, characterId);
        return count != null && count > 0;
    }
    
    /**
     * Deletes all signups for a raid (for cascade deletion).
     */
    @Transactional
    public void deleteByRaidId(Long raidId) {
        String sql = "DELETE FROM raid_signups WHERE raid_id = ?";
        jdbcTemplate.update(sql, raidId);
    }
    
    /**
     * Deletes a signup by ID.
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM raid_signups WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
