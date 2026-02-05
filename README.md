# KiHongan Raid System

LINE-based raid signup system for managing game characters and raid events.

## 📋 專案概述

這是一個基於 LINE LIFF 的遠征報名系統，解決群組內複製貼上報名的混亂問題。使用者可以：

- 🔐 透過 LINE 登入並綁定身分
- 👤 管理多個遊戲角色
- 📅 報名遠征活動
- 👥 查看遠征報名名單
- 🎯 管理者可發布遠征

## 🏗️ 技術架構

### 後端
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** (JWT 認證)
- **JDBC** (直接資料庫操作)
- **PostgreSQL** (Supabase)
- **jqwik** (Property-Based Testing)
- **Testcontainers** (整合測試)

### 前端
- **Vue3 / 原生 JS**
- **LIFF SDK v2**
- **Vercel** (部署)

### 資料庫
- **Supabase PostgreSQL**
- 4 個主要資料表：users, characters, raids, raid_signups

## 🚀 快速開始

### 前置需求

- Java 17+
- Maven 3.6+
- PostgreSQL 資料庫 (Supabase)

### 安裝

```bash
# Clone repository
git clone <repository-url>
cd KiHongan

# 安裝依賴
mvn clean install
```

### 配置

建立 `.env` 檔案或設定環境變數：

```bash
DATABASE_URL=jdbc:postgresql://your-host:5432/postgres?sslmode=require
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password
LINE_CHANNEL_ID=2009058924
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long
```

### 執行

```bash
# 執行測試
mvn test

# 啟動應用
mvn spring-boot:run
```

應用會在 `http://localhost:8080` 啟動

## 📚 文件

- [API 文件](API.md) - 完整的 API 端點說明
- [部署指南](DEPLOYMENT.md) - 部署到生產環境的步驟
- [需求文件](.kiro/specs/kihongan-raid-system/requirements.md) - 功能需求
- [設計文件](.kiro/specs/kihongan-raid-system/design.md) - 系統設計
- [任務清單](.kiro/specs/kihongan-raid-system/tasks.md) - 開發任務

## 🔌 API 端點

### 認證
- `POST /auth/line` - LINE 登入

### 角色管理
- `GET /me/characters` - 取得我的角色列表
- `POST /me/characters` - 建立角色
- `PUT /me/characters/{id}` - 更新角色
- `DELETE /me/characters/{id}` - 刪除角色

### 遠征管理
- `GET /raids` - 取得所有遠征
- `POST /raids` - 建立遠征
- `DELETE /raids/{id}` - 刪除遠征

### 報名
- `POST /raids/{id}/signup` - 報名遠征
- `GET /raids/{id}/signups` - 取得遠征報名名單

詳細說明請參考 [API.md](API.md)

## 🧪 測試

專案包含完整的測試覆蓋：

- **32+ Property-Based Tests** (每個 100+ iterations)
- **Integration Tests** (所有 API 端點)
- **Unit Tests** (Repository 層)

```bash
# 執行所有測試
mvn test

# 執行特定測試
mvn test -Dtest=CharacterServicePropertyTest
```

## 📦 專案結構

```
src/
├── main/
│   ├── java/com/kihongan/raidsystem/
│   │   ├── config/          # 配置類別
│   │   ├── controller/      # REST Controllers
│   │   ├── domain/          # Domain 層
│   │   │   ├── character/   # 角色管理
│   │   │   ├── raid/        # 遠征管理
│   │   │   └── signup/      # 報名管理
│   │   ├── exception/       # 自定義異常
│   │   └── security/        # JWT 認證
│   └── resources/
│       └── application.yml  # 應用配置
└── test/
    ├── java/                # 測試代碼
    └── resources/
        ├── application-test.yml
        └── schema.sql       # 測試資料庫 schema
```

## 🎯 開發階段

### ✅ Phase 1: 基礎 (已完成)
- LINE Login 整合
- JWT 認證系統
- 資料庫連線
- 使用者管理

### ✅ Phase 2: 核心功能 (已完成)
- 角色 CRUD
- 遠征 CRUD
- 報名系統
- 完整測試覆蓋

### ⬜ Phase 3: 進階功能 (未來)
- Admin UI
- Flex Message 輸出
- Permission 系統
- 多群組支援
- 排隊候補功能

## 🔒 安全性

- JWT Token 認證
- 密碼加密儲存
- SQL Injection 防護 (Prepared Statements)
- CORS 配置
- HTTPS 強制 (生產環境)

## 🚀 部署

支援多種部署平台：

- **Zeabur** (推薦)
- **Fly.io**
- **Render**
- **Heroku**

詳細步驟請參考 [DEPLOYMENT.md](DEPLOYMENT.md)

## 🤝 貢獻

歡迎提交 Issue 和 Pull Request！

## 📄 授權

MIT License

## 👥 團隊

- **產品 / 架構**: 育瑋
- **技術實作**: Kiro AI

## 🔗 相關連結

- [LIFF 文件](https://developers.line.biz/en/docs/liff/)
- [Spring Boot 文件](https://spring.io/projects/spring-boot)
- [jqwik 文件](https://jqwik.net/)
- [Supabase 文件](https://supabase.com/docs)

---

**注意**: 這是 Phase 2 的完整實作，包含所有核心功能和完整的測試覆蓋。
