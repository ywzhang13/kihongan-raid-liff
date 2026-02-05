package com.kihongan.raidsystem.controller;

import com.kihongan.raidsystem.controller.dto.LineLoginRequest;
import com.kihongan.raidsystem.controller.dto.LineLoginResponse;
import com.kihongan.raidsystem.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * Authentication controller for LINE login.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtService jwtService;
    private final long jwtExpiration = 86400000L; // 24 hours

    public AuthController(JdbcTemplate jdbcTemplate, JwtService jwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
    }

    @PostMapping("/line")
    public ResponseEntity<LineLoginResponse> lineLogin(@RequestBody LineLoginRequest request) {
        // 在實際環境中，這裡應該驗證 LINE idToken
        // 目前為測試環境，直接接受請求
        
        String lineUserId = request.getUserId();
        String name = request.getName();
        String picture = request.getPicture();
        
        // 檢查使用者是否已存在
        Long userId = findUserByLineUserId(lineUserId);
        
        if (userId == null) {
            // 建立新使用者
            userId = createUser(lineUserId, name, picture);
        } else {
            // 更新使用者資訊
            updateUser(userId, name, picture);
        }
        
        // 生成 JWT token
        String token = jwtService.generateToken(userId, lineUserId, jwtExpiration);
        
        LineLoginResponse response = new LineLoginResponse();
        response.setAppToken(token);
        response.setLineUserId(lineUserId);
        response.setUserDbId(userId);
        
        return ResponseEntity.ok(response);
    }

    private Long findUserByLineUserId(String lineUserId) {
        String sql = "SELECT id FROM users WHERE line_user_id = ?";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return rs.getLong("id");
            }
            return null;
        }, lineUserId);
    }

    private Long createUser(String lineUserId, String name, String picture) {
        String sql = "INSERT INTO users (line_user_id, name, picture, created_at, updated_at) VALUES (?, ?, ?, ?, ?) RETURNING id";
        
        Instant now = Instant.now();
        
        return jdbcTemplate.queryForObject(sql, Long.class, 
            lineUserId, name, picture, Timestamp.from(now), Timestamp.from(now));
    }

    private void updateUser(Long userId, String name, String picture) {
        String sql = "UPDATE users SET name = ?, picture = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, name, picture, Timestamp.from(Instant.now()), userId);
    }
}
