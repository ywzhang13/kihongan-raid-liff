# KiHongan Raid System - API 文件

## 基本資訊

- **Base URL**: `https://your-domain.com`
- **認證方式**: JWT Bearer Token
- **Content-Type**: `application/json`

## 認證

### LINE 登入

```http
POST /auth/line
```

**Request Body:**
```json
{
  "idToken": "LINE_ID_TOKEN",
  "userId": "U1234567890",
  "name": "使用者名稱",
  "picture": "https://profile.line-scdn.net/..."
}
```

**Response (200 OK):**
```json
{
  "appToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "lineUserId": "U1234567890",
  "userDbId": 1
}
```

**說明:**
- 驗證 LINE idToken
- 建立或更新使用者資料
- 返回應用專用的 JWT token

---

## 角色管理

所有角色端點需要 JWT 認證。

### 取得我的角色列表

```http
GET /me/characters
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "戰士角色",
    "job": "坦克",
    "level": 50,
    "gameId": "GAME123",
    "note": "主坦",
    "isDefault": true,
    "createdAt": "2026-02-05T10:00:00Z",
    "updatedAt": "2026-02-05T10:00:00Z"
  }
]
```

### 建立角色

```http
POST /me/characters
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "法師角色",
  "job": "DPS",
  "level": 45,
  "gameId": "GAME456",
  "note": "副本專用",
  "isDefault": false
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "name": "法師角色",
  "job": "DPS",
  "level": 45,
  "gameId": "GAME456",
  "note": "副本專用",
  "isDefault": false,
  "createdAt": "2026-02-05T11:00:00Z",
  "updatedAt": "2026-02-05T11:00:00Z"
}
```

**驗證規則:**
- `name`: 必填，不可為空
- `level`: 選填，不可為負數
- `job`, `gameId`, `note`: 選填
- `isDefault`: 選填，預設 false

### 更新角色

```http
PUT /me/characters/{id}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body (部分更新):**
```json
{
  "name": "新名稱",
  "level": 60
}
```

**Response (200 OK):**
```json
{
  "id": 2,
  "name": "新名稱",
  "job": "DPS",
  "level": 60,
  "gameId": "GAME456",
  "note": "副本專用",
  "isDefault": false,
  "createdAt": "2026-02-05T11:00:00Z",
  "updatedAt": "2026-02-05T12:00:00Z"
}
```

**說明:**
- 只更新提供的欄位
- 其他欄位保持不變
- 只能更新自己的角色

### 刪除角色

```http
DELETE /me/characters/{id}
Authorization: Bearer {token}
```

**Response (204 No Content)**

**錯誤情況:**
- 400: 角色有活躍的報名記錄
- 403: 不是角色擁有者
- 404: 角色不存在

---

## 遠征管理

### 取得所有遠征

```http
GET /raids
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "古龍討伐",
    "subtitle": "困難模式",
    "boss": "古代巨龍",
    "startTime": "2026-02-10T20:00:00Z",
    "createdBy": 1,
    "createdAt": "2026-02-05T10:00:00Z"
  }
]
```

**說明:**
- 不需要認證
- 按 startTime 升序排序

### 建立遠征

```http
POST /raids
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "新遠征",
  "subtitle": "普通模式",
  "boss": "火龍",
  "startTime": "2026-02-15T19:00:00Z"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "title": "新遠征",
  "subtitle": "普通模式",
  "boss": "火龍",
  "startTime": "2026-02-15T19:00:00Z",
  "createdBy": 1,
  "createdAt": "2026-02-05T12:00:00Z"
}
```

**驗證規則:**
- `title`: 必填，不可為空
- `startTime`: 必填，不可為過去時間
- `subtitle`, `boss`: 選填

### 刪除遠征

```http
DELETE /raids/{id}
Authorization: Bearer {token}
```

**Response (204 No Content)**

**說明:**
- 需要認證
- 會級聯刪除所有報名記錄

---

## 報名管理

### 報名遠征

```http
POST /raids/{raidId}/signup
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "characterId": 1
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "characterId": 1,
  "characterName": "戰士角色",
  "job": "坦克",
  "level": 50,
  "userId": 1,
  "userName": "使用者名稱",
  "userPicture": "https://profile.line-scdn.net/...",
  "status": "confirmed"
}
```

**驗證規則:**
- 只能用自己的角色報名
- 同一角色不能重複報名同一遠征
- 遠征和角色必須存在

**錯誤情況:**
- 400: 重複報名
- 403: 使用他人角色
- 404: 遠征或角色不存在

### 取得遠征報名名單

```http
GET /raids/{raidId}/signups
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "characterId": 1,
    "characterName": "戰士角色",
    "job": "坦克",
    "level": 50,
    "userId": 1,
    "userName": "使用者名稱",
    "userPicture": "https://profile.line-scdn.net/...",
    "status": "confirmed"
  },
  {
    "id": 2,
    "characterId": 3,
    "characterName": "法師角色",
    "job": "DPS",
    "level": 45,
    "userId": 2,
    "userName": "其他使用者",
    "userPicture": "https://profile.line-scdn.net/...",
    "status": "confirmed"
  }
]
```

**說明:**
- 不需要認證
- 包含完整的角色和使用者資訊
- 按報名時間排序

---

## 錯誤回應

### 400 Bad Request (驗證錯誤)

```json
{
  "error": "Validation failed",
  "message": "Character name cannot be empty"
}
```

### 401 Unauthorized (未認證)

```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### 403 Forbidden (權限不足)

```json
{
  "error": "Forbidden",
  "message": "You do not have permission to access this resource"
}
```

### 404 Not Found (資源不存在)

```json
{
  "error": "Not found",
  "message": "Character not found"
}
```

### 500 Internal Server Error (伺服器錯誤)

```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred"
}
```

---

## 使用範例

### cURL

```bash
# 取得遠征列表
curl https://your-domain.com/raids

# 建立角色
curl -X POST https://your-domain.com/me/characters \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的角色",
    "job": "坦克",
    "level": 50
  }'

# 報名遠征
curl -X POST https://your-domain.com/raids/1/signup \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "characterId": 1
  }'
```

### JavaScript (Fetch)

```javascript
// 取得遠征列表
const raids = await fetch('https://your-domain.com/raids')
  .then(res => res.json());

// 建立角色
const character = await fetch('https://your-domain.com/me/characters', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: '我的角色',
    job: '坦克',
    level: 50
  })
}).then(res => res.json());

// 報名遠征
const signup = await fetch('https://your-domain.com/raids/1/signup', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    characterId: 1
  })
}).then(res => res.json());
```

---

## Rate Limiting

目前未實作 rate limiting，建議：
- 合理使用 API
- 避免短時間大量請求
- 實作客戶端快取

## CORS

開發環境允許所有來源，生產環境需要設定允許的來源。

## 版本控制

目前版本: v1.0
API 路徑不包含版本號，未來可能加入 `/v1/` 前綴。
