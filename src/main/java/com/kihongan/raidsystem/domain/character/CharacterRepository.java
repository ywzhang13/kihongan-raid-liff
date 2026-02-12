package com.kihongan.raidsystem.domain.character;

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
 * Repository for Character entity using JDBC.
 * Handles all database operations for characters.
 */
@Repository
public class CharacterRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public CharacterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<Character> characterRowMapper = (rs, rowNum) -> {
        Character character = new Character();
        character.setId(rs.getLong("id"));
        character.setUserId(rs.getLong("user_id"));
        character.setName(rs.getString("name"));
        character.setJob(rs.getString("job"));
        
        Integer level = (Integer) rs.getObject("level");
        character.setLevel(level);
        
        character.setIsDefault(rs.getBoolean("is_default"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            character.setCreatedAt(createdAt.toInstant());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            character.setUpdatedAt(updatedAt.toInstant());
        }
        
        return character;
    };
    
    /**
     * Finds all characters belonging to a user.
     */
    public List<Character> findByUserId(Long userId) {
        String sql = "SELECT * FROM characters WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, characterRowMapper, userId);
    }
    
    /**
     * Finds a character by ID.
     */
    public Optional<Character> findById(Long id) {
        String sql = "SELECT * FROM characters WHERE id = ?";
        List<Character> results = jdbcTemplate.query(sql, characterRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Saves a character (insert or update).
     */
    public Character save(Character character) {
        if (character.getId() == null) {
            return insert(character);
        } else {
            return update(character);
        }
    }
    
    private Character insert(Character character) {
        String sql = """
                INSERT INTO characters (user_id, name, job, level, is_default, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        
        Instant now = Instant.now();
        character.setCreatedAt(now);
        character.setUpdatedAt(now);
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, character.getUserId());
            ps.setString(2, character.getName());
            ps.setString(3, character.getJob());
            if (character.getLevel() != null) {
                ps.setInt(4, character.getLevel());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setBoolean(5, character.getIsDefault() != null ? character.getIsDefault() : false);
            ps.setTimestamp(6, Timestamp.from(character.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.from(character.getUpdatedAt()));
            return ps;
        }, keyHolder);
        
        // PostgreSQL returns all columns, so we need to get the 'id' specifically
        Number key = (Number) keyHolder.getKeys().get("id");
        character.setId(key.longValue());
        return character;
    }
    
    private Character update(Character character) {
        String sql = """
                UPDATE characters 
                SET name = ?, job = ?, level = ?, is_default = ?, updated_at = ?
                WHERE id = ?
                """;
        
        Instant now = Instant.now();
        character.setUpdatedAt(now);
        
        jdbcTemplate.update(sql,
                character.getName(),
                character.getJob(),
                character.getLevel(),
                character.getIsDefault(),
                Timestamp.from(character.getUpdatedAt()),
                character.getId()
        );
        
        return character;
    }
    
    /**
     * Deletes a character by ID.
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM characters WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    /**
     * Unsets the default flag for all characters of a user.
     */
    public void unsetDefaultForUser(Long userId) {
        String sql = "UPDATE characters SET is_default = false WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
    
    /**
     * Checks if a character exists and belongs to a user.
     */
    public boolean existsByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT COUNT(*) FROM characters WHERE id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, userId);
        return count != null && count > 0;
    }
    
    /**
     * Checks if a character has active signups.
     */
    public boolean hasActiveSignups(Long characterId) {
        String sql = "SELECT COUNT(*) FROM raid_signups WHERE character_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, characterId);
        return count != null && count > 0;
    }
}
