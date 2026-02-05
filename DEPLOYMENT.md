# KiHongan Raid System - 部署指南

## 前置需求

### 開發環境
- Java 17+
- Maven 3.6+
- PostgreSQL 資料庫 (Supabase)

### 生產環境
- Java 17 Runtime
- HTTPS 支援
- 環境變數管理

## 資料庫設定

### 1. 在 Supabase 建立資料表

執行以下 SQL：

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    line_user_id VARCHAR(64) UNIQUE NOT NULL,
    name TEXT,
    picture TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Characters table
CREATE TABLE characters (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name TEXT NOT NULL,
    job TEXT,
    level INTEGER,
    game_id TEXT,
    note TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Raids table
CREATE TABLE raids (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    subtitle TEXT,
    boss TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Raid signups table
CREATE TABLE raid_signups (
    id BIGSERIAL PRIMARY KEY,
    raid_id BIGINT NOT NULL REFERENCES raids(id) ON DELETE CASCADE,
    character_id BIGINT NOT NULL REFERENCES characters(id),
    status TEXT DEFAULT 'confirmed',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(raid_id, character_id)
);

-- Indexes
CREATE INDEX idx_characters_user_id ON characters(user_id);
CREATE INDEX idx_raids_start_time ON raids(start_time);
CREATE INDEX idx_raid_signups_raid_id ON raid_signups(raid_id);
CREATE INDEX idx_raid_signups_character_id ON raid_signups(character_id);
```

## 環境變數設定

### 開發環境 (.env)

```bash
# Database
DATABASE_URL=jdbc:postgresql://your-supabase-host:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-password

# LINE
LINE_CHANNEL_ID=2009058924

# JWT
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long-for-security
```

### 生產環境

在部署平台設定以下環境變數：

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `LINE_CHANNEL_ID`
- `JWT_SECRET` (至少 32 字元)

## 本地開發

### 1. 安裝依賴

```bash
mvn clean install
```

### 2. 執行測試

```bash
mvn test
```

### 3. 啟動應用

```bash
mvn spring-boot:run
```

應用會在 `http://localhost:8080` 啟動

## 部署到生產環境

### 選項 1: Zeabur

1. 連接 GitHub repository
2. 選擇 Java 17 runtime
3. 設定環境變數
4. 部署

### 選項 2: Fly.io

```bash
# 安裝 flyctl
curl -L https://fly.io/install.sh | sh

# 登入
flyctl auth login

# 初始化
flyctl launch

# 設定環境變數
flyctl secrets set DATABASE_URL="..."
flyctl secrets set DATABASE_USERNAME="..."
flyctl secrets set DATABASE_PASSWORD="..."
flyctl secrets set LINE_CHANNEL_ID="2009058924"
flyctl secrets set JWT_SECRET="..."

# 部署
flyctl deploy
```

### 選項 3: Render

1. 建立新的 Web Service
2. 連接 GitHub repository
3. Build Command: `mvn clean package`
4. Start Command: `java -jar target/raid-system-0.0.1-SNAPSHOT.jar`
5. 設定環境變數
6. 部署

## API 端點

### Authentication
- `POST /auth/line` - LINE 登入

### Characters
- `GET /me/characters` - 取得我的角色列表
- `POST /me/characters` - 建立角色
- `PUT /me/characters/{id}` - 更新角色
- `DELETE /me/characters/{id}` - 刪除角色

### Raids
- `GET /raids` - 取得所有遠征
- `POST /raids` - 建立遠征
- `DELETE /raids/{id}` - 刪除遠征

### Signups
- `POST /raids/{id}/signup` - 報名遠征
- `GET /raids/{id}/signups` - 取得遠征報名名單

## 健康檢查

```bash
curl http://localhost:8080/actuator/health
```

## 故障排除

### 資料庫連線失敗
- 檢查 DATABASE_URL 格式
- 確認 Supabase 允許外部連線
- 檢查 SSL 設定 (`sslmode=require`)

### JWT 驗證失敗
- 確認 JWT_SECRET 至少 32 字元
- 檢查 token 是否過期
- 確認 Authorization header 格式：`Bearer <token>`

### 測試失敗
- 確認 Docker 正在運行 (Testcontainers)
- 檢查網路連線
- 清除 Maven cache: `mvn clean`

## 監控與日誌

### 查看日誌

```bash
# 本地
tail -f logs/spring.log

# Fly.io
flyctl logs

# Render
在 Dashboard 查看
```

### 效能監控

建議整合：
- Spring Boot Actuator
- Prometheus + Grafana
- Sentry (錯誤追蹤)

## 安全性建議

1. **JWT Secret**: 使用強密碼生成器產生至少 32 字元的隨機字串
2. **HTTPS**: 生產環境必須使用 HTTPS
3. **環境變數**: 絕不將敏感資訊提交到 Git
4. **CORS**: 根據需求設定允許的來源
5. **Rate Limiting**: 考慮加入 API rate limiting

## 備份策略

### 資料庫備份

Supabase 自動備份，但建議：
- 定期匯出重要資料
- 測試還原流程
- 保留多個備份版本

## 擴展性

### 水平擴展
- 應用是無狀態的，可以輕鬆水平擴展
- 使用 Load Balancer 分散流量

### 快取
- 考慮加入 Redis 快取熱門資料
- 快取遠征列表
- 快取用戶角色列表

## 支援

如有問題，請查看：
- README.md
- API 文件
- GitHub Issues
