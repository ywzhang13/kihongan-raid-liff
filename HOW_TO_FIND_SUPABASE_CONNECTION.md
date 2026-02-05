# 如何找到 Supabase 資料庫連線參數

## 步驟 1: 登入 Supabase Dashboard
前往 https://supabase.com/dashboard

## 步驟 2: 選擇你的專案
點選你的專案（從截圖看起來是 "Shun's Project"）

## 步驟 3: 找到連線資訊

### 方法 A: 從 Project Settings 找
1. 點選左側選單的 **Settings**（齒輪圖示）
2. 點選 **Database**
3. 往下捲動找到 **Connection string** 或 **Connection info** 區塊
4. 你會看到：
   - **Host**: `db.xxx.supabase.co`
   - **Database name**: `postgres`
   - **Port**: `5432`
   - **User**: `postgres`
   - **Password**: 點選 "Show" 或 "Reset password" 來查看

### 方法 B: 從 Connection Pooling 找
1. 在 Settings > Database 頁面
2. 找到 **Connection Pooling** 區塊
3. 選擇 **Transaction** 模式
4. 複製 **Connection string**，格式類似：
   ```
   postgresql://postgres.[project-ref]:[password]@aws-0-[region].pooler.supabase.com:6543/postgres
   ```

## 步驟 4: 轉換為 JDBC URL

如果你的 Supabase 連線字串是：
```
postgresql://postgres.abcdefgh:mypassword@db.abcdefgh.supabase.co:5432/postgres
```

轉換為 JDBC URL：
```
jdbc:postgresql://db.abcdefgh.supabase.co:5432/postgres
```

## 步驟 5: 填入 .env 檔案

```env
DATABASE_URL=jdbc:postgresql://db.abcdefgh.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=你的密碼
```

## 注意事項

1. **密碼重設**: 如果忘記密碼，可以在 Settings > Database 中重設
2. **防火牆**: Supabase 預設允許所有 IP 連線，如果有設定 IP 白名單，請確認你的 IP 在名單中
3. **SSL**: Supabase 需要 SSL 連線，Spring Boot 會自動處理
4. **Connection Pooling**: 建議使用 Transaction mode 的連線字串（port 6543）以獲得更好的效能

## 測試連線

填好 .env 後，重新啟動應用程式：
```bash
./mvnw.cmd spring-boot:run
```

如果連線成功，你會在日誌中看到：
```
Tomcat started on port 8080 (http) with context path ''
```

如果連線失敗，會看到類似的錯誤：
```
Connection refused
Authentication failed
```
