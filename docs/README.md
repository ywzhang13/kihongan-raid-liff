# KiHongan 遠征報名系統 - 前端

這是 KiHongan 遠征報名系統的前端介面，使用 LINE LIFF SDK 整合。

## 功能特色

- ✅ LINE 登入整合
- ✅ 角色管理（建立、編輯、刪除）
- ✅ 遠征活動管理
- ✅ 報名系統（上限 6 人）
- ✅ 週四早上 8:00 自動重製
- ✅ 按星期排序（四五六日一二三）
- ✅ 響應式設計

## 本地測試

1. 直接用瀏覽器開啟 `index.html`
2. 或使用簡單的 HTTP 伺服器：

```bash
# Python 3
python -m http.server 8000

# Node.js
npx http-server

# PHP
php -S localhost:8000
```

3. 開啟 http://localhost:8000

## 部署到生產環境

### 方法 1: GitHub Pages（推薦）

1. 修改 `app.js` 中的 API URL：
```javascript
const API_BASE_URL = 'https://你的後端URL';
```

2. 推送到 GitHub：
```bash
git add .
git commit -m "Update API URL"
git push
```

3. 在 GitHub Repository → Settings → Pages
4. Source 選擇 `main` branch，資料夾選擇 `/frontend`
5. 儲存後等待部署完成

你的前端 URL 會是：
```
https://你的GitHub帳號.github.io/KiHongan/
```

### 方法 2: Netlify

1. 前往 https://netlify.com
2. 拖曳 `frontend` 資料夾到 Netlify
3. 或連接 GitHub repository，設定 Base directory 為 `frontend`

### 方法 3: Vercel

```bash
# 安裝 Vercel CLI
npm i -g vercel

# 部署
cd frontend
vercel
```

## LINE LIFF 設定

### 1. 建立 LINE Login Channel

1. 前往 https://developers.line.biz/console/
2. 建立新的 LINE Login channel
3. 記下 Channel ID

### 2. 建立 LIFF App

1. 在 Channel 設定中，進入 LIFF 分頁
2. 點擊「Add」建立新的 LIFF app
3. 設定：
   - **LIFF app name**: KiHongan 遠征報名
   - **Size**: Full
   - **Endpoint URL**: 你的前端 URL
   - **Scope**: profile, openid
4. 記下 LIFF ID

### 3. 更新前端設定

編輯 `app.js`，修改 `initializeLiff()` 函數：

```javascript
async function initializeLiff() {
    try {
        // 替換為你的 LIFF ID
        await liff.init({ liffId: '你的LIFF-ID' });
        
        if (!liff.isLoggedIn()) {
            liff.login();
            return;
        }
        
        // 取得使用者資訊
        const profile = await liff.getProfile();
        const idToken = liff.getIDToken();
        
        // 呼叫後端登入 API
        const response = await fetch(`${API_BASE_URL}/auth/line`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                idToken: idToken,
                userId: profile.userId,
                name: profile.displayName,
                picture: profile.pictureUrl
            })
        });
        
        if (!response.ok) {
            throw new Error('登入失敗');
        }
        
        const data = await response.json();
        appToken = data.appToken;
        currentUser = {
            lineUserId: data.lineUserId,
            userDbId: data.userDbId,
            name: profile.displayName,
            picture: profile.pictureUrl
        };
        
        showMessage('userStatus', `✅ 已登入: ${currentUser.name}`, 'success');
        
        // 自動載入初始資料
        loadMyCharacters();
        loadRaids();
        
    } catch (error) {
        console.error('LIFF 初始化失敗:', error);
        showMessage('userStatus', '❌ 初始化失敗: ' + error.message, 'error');
    }
}
```

### 4. 設定 Callback URL

在 LINE Developers Console → 你的 Channel → LINE Login 分頁：
- **Callback URL**: 設定為你的前端 URL

## 使用方式

### 分享給 LINE 群組成員

在 LINE 群組中傳送：
```
🎮 KiHongan 遠征報名系統
https://liff.line.me/你的LIFF-ID

點擊連結即可使用！
```

### 功能說明

1. **我的角色**：
   - 建立角色（名稱、職業、等級）
   - 編輯角色資訊
   - 刪除角色
   - 設定預設角色（⭐）

2. **本周遠征**：
   - 建立遠征活動（Boss、時間、備註）
   - 報名參加（選擇角色）
   - 查看報名名單
   - 刪除遠征
   - 自動顯示星期幾
   - 按週四到週三排序

## 技術細節

### 檔案結構

```
frontend/
├── index.html      # 主頁面
├── app.js          # 應用邏輯
└── README.md       # 說明文件
```

### API 整合

所有 API 請求都透過 `apiRequest()` 函數處理：

```javascript
async function apiRequest(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (appToken && !options.noAuth) {
        headers['Authorization'] = `Bearer ${appToken}`;
    }
    
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers
    });
    
    // 處理回應...
}
```

### 週期計算

系統使用週四早上 8:00 作為週期起點：

```javascript
function getThisWeekThursday() {
    const now = new Date();
    const day = now.getDay();
    const diff = day >= 4 ? 4 - day : 4 - day - 7;
    
    const thursday = new Date(now);
    thursday.setDate(now.getDate() + diff);
    thursday.setHours(8, 0, 0, 0);
    
    if (now < thursday) {
        thursday.setDate(thursday.getDate() - 7);
    }
    
    return thursday;
}
```

### 星期排序

遠征活動按照週四到週三的順序排列：

```javascript
function getWeekDayOrder(date) {
    const day = date.getDay();
    if (day === 4) return 0; // 週四
    if (day === 5) return 1; // 週五
    if (day === 6) return 2; // 週六
    if (day === 0) return 3; // 週日
    if (day === 1) return 4; // 週一
    if (day === 2) return 5; // 週二
    if (day === 3) return 6; // 週三
    return 0;
}
```

## 瀏覽器支援

- Chrome 90+
- Safari 14+
- Firefox 88+
- Edge 90+
- LINE 內建瀏覽器

## 故障排除

### 問題：無法連接後端

**檢查**：
1. `API_BASE_URL` 是否正確？
2. 後端是否正常運行？
3. CORS 設定是否正確？

### 問題：LINE 登入失敗

**檢查**：
1. LIFF ID 是否正確？
2. Callback URL 是否設定？
3. 是否在 LINE 應用程式中開啟？

### 問題：顯示「測試模式」

這是因為使用了 `mockLogin()` 函數。部署到生產環境時，請確保：
1. 已設定正確的 LIFF ID
2. 已移除或註解掉 `mockLogin()` 相關程式碼
3. 使用真實的 LIFF 初始化流程

## 開發模式

如果要在本地測試而不使用 LINE LIFF，可以保留 `mockLogin()` 函數：

```javascript
async function initializeLiff() {
    try {
        // 開發模式：使用模擬登入
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            showMessage('userStatus', '⚠️ 測試模式：使用模擬登入', 'warning');
            await mockLogin();
            return;
        }
        
        // 生產模式：使用真實 LIFF
        await liff.init({ liffId: '你的LIFF-ID' });
        // ... 其他程式碼
    } catch (error) {
        console.error('LIFF 初始化失敗:', error);
    }
}
```

## 更新日誌

### v1.0.0 (2026-02-06)
- ✅ 初始版本
- ✅ LINE LIFF 整合
- ✅ 角色管理功能
- ✅ 遠征報名系統
- ✅ 週四重製機制
- ✅ 星期排序功能
- ✅ 編輯角色功能

## 授權

MIT License

## 支援

如有問題，請查看：
- [完整部署指南](../部署指南.md)
- [API 文件](../API.md)
- [後端 README](../README.md)
