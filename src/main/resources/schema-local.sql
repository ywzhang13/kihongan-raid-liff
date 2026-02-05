-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_user_id VARCHAR(64) UNIQUE NOT NULL,
    name TEXT,
    picture TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Characters table
CREATE TABLE IF NOT EXISTS characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name TEXT NOT NULL,
    job TEXT,
    level INTEGER,
    game_id TEXT,
    note TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Raids table
CREATE TABLE IF NOT EXISTS raids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title TEXT NOT NULL,
    subtitle TEXT,
    boss TEXT,
    start_time TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Raid signups table
CREATE TABLE IF NOT EXISTS raid_signups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    raid_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    status TEXT DEFAULT 'confirmed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (raid_id) REFERENCES raids(id) ON DELETE CASCADE,
    FOREIGN KEY (character_id) REFERENCES characters(id),
    UNIQUE(raid_id, character_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_characters_user_id ON characters(user_id);
CREATE INDEX IF NOT EXISTS idx_raids_start_time ON raids(start_time);
CREATE INDEX IF NOT EXISTS idx_raid_signups_raid_id ON raid_signups(raid_id);
CREATE INDEX IF NOT EXISTS idx_raid_signups_character_id ON raid_signups(character_id);
