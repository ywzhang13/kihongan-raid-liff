// 全域變數
const API_BASE_URL = 'https://kihongan-raid-liff.onrender.com';
let appToken = null;
let currentUser = null;

// 初始化 LIFF
let isInitializing = false;
async function initializeLiff() {
    // 防止重複初始化
    if (isInitializing) {
        console.log('LIFF 正在初始化中，跳過重複請求');
        return;
    }
    
    isInitializing = true;
    
    try {
        showMessage('userStatus', '⏳ 正在初始化...', 'info');
        
        // 使用正確的 LIFF ID
        await liff.init({ liffId: '2009058924-rvQKQaLI' });
        
        if (!liff.isLoggedIn()) {
            showMessage('userStatus', '🔐 請登入 LINE', 'warning');
            liff.login();
            return;
        }
        
        // 取得使用者資訊
        const profile = await liff.getProfile();
        const idToken = liff.getIDToken();
        
        showMessage('userStatus', '⏳ 正在登入後端...', 'info');
        
        // 呼叫後端登入 API（增加超時處理）
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10秒超時
        
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
            }),
            signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`登入失敗 (${response.status}): ${errorText}`);
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
        
        // 自動載入初始資料（預設顯示遠征列表）
        await loadRaids();
        
    } catch (error) {
        console.error('LIFF 初始化失敗:', error);
        
        if (error.name === 'AbortError') {
            showMessage('userStatus', '❌ 連線超時，請檢查後端服務是否正常運作', 'error');
        } else {
            showMessage('userStatus', '❌ 初始化失敗: ' + error.message, 'error');
        }
    } finally {
        isInitializing = false;
    }
}

// 模擬登入（測試用）
async function mockLogin() {
    try {
        const mockProfile = {
            userId: 'U' + Math.random().toString(36).substring(2, 15),
            displayName: '測試使用者',
            pictureUrl: 'https://via.placeholder.com/150'
        };
        
        // 呼叫後端登入 API
        const response = await fetch(`${API_BASE_URL}/auth/line`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                idToken: 'mock_id_token',
                userId: mockProfile.userId,
                name: mockProfile.displayName,
                picture: mockProfile.pictureUrl
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
            name: mockProfile.displayName,
            picture: mockProfile.pictureUrl
        };
        
        showMessage('userStatus', `✅ 已登入: ${currentUser.name}`, 'success');
        
        // 自動載入初始資料
        loadMyCharacters();
        loadRaids();
        
    } catch (error) {
        console.error('登入錯誤:', error);
        showMessage('userStatus', '❌ 登入失敗: ' + error.message, 'error');
    }
}

// 切換分頁
function switchTab(tabName) {
    // 更新分頁按鈕
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });
    event.target.classList.add('active');
    
    // 更新內容區
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(tabName).classList.add('active');
    
    // 載入對應資料
    if (tabName === 'characters') {
        loadMyCharacters();
    } else if (tabName === 'raids') {
        loadRaids();
    }
}

// 顯示訊息
function showMessage(elementId, message, type = 'info') {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    element.textContent = message;
    element.className = 'token-status';
    
    if (type === 'success') {
        element.classList.add('valid');
    } else if (type === 'error') {
        element.classList.add('invalid');
    } else if (type === 'warning') {
        element.style.background = '#fff3cd';
        element.style.color = '#856404';
        element.style.border = '1px solid #ffeaa7';
    }
}

// API 請求輔助函數
async function apiRequest(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (appToken && !options.noAuth) {
        headers['Authorization'] = `Bearer ${appToken}`;
    }
    
    // 增加超時處理
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 15000); // 15秒超時
    
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers,
            signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            const error = await response.json().catch(() => ({ 
                error: response.statusText,
                status: response.status 
            }));
            throw error;
        }
        
        if (response.status === 204) {
            return null;
        }
        
        return await response.json();
    } catch (error) {
        clearTimeout(timeoutId);
        
        if (error.name === 'AbortError') {
            throw new Error('請求超時，請檢查網路連線');
        }
        throw error;
    }
}

// ==================== 角色管理 ====================

// 開啟建立角色 Modal
function openCreateCharacterModal() {
    // 清空表單
    document.getElementById('charName').value = '';
    document.getElementById('charJob').value = '';
    document.getElementById('charLevel').value = '';
    document.getElementById('charIsDefault').checked = false;
    
    // 顯示 Modal
    document.getElementById('createCharacterModal').classList.add('show');
}

async function loadMyCharacters() {
    try {
        const characters = await apiRequest('/me/characters');
        displayCharacters(characters);
    } catch (error) {
        console.error('載入角色失敗:', error);
        const container = document.getElementById('charactersList');
        if (container) {
            container.innerHTML = '<p style="color: #e74c3c; padding: 20px; text-align: center;">❌ 載入失敗，請稍後再試</p>';
        }
    }
}

function displayCharacters(characters) {
    const container = document.getElementById('charactersList');
    if (!container) return;
    
    if (characters.length === 0) {
        container.innerHTML = '<p style="color: #999; padding: 20px; text-align: center;">尚無角色，請建立第一個角色</p>';
        return;
    }
    
    container.innerHTML = characters.map(char => `
        <div class="list-item">
            <h4>${char.name} ${char.isDefault ? '⭐' : ''}</h4>
            <p><strong>職業:</strong> ${char.job || '未設定'}</p>
            <p><strong>等級:</strong> ${char.level || '未設定'}</p>
            <p style="font-size: 12px; color: #999;">建立時間: ${new Date(char.createdAt).toLocaleString('zh-TW')}</p>
            <div class="action-buttons">
                <button onclick="editCharacter(${char.id})">編輯</button>
                <button onclick="deleteCharacter(${char.id})" class="danger">刪除</button>
            </div>
        </div>
    `).join('');
}

async function createCharacter() {
    try {
        const name = document.getElementById('charName').value.trim();
        const job = document.getElementById('charJob').value;
        const level = document.getElementById('charLevel').value;
        const isDefault = document.getElementById('charIsDefault').checked;
        
        if (!name) {
            alert('請輸入角色名稱');
            return;
        }
        
        const data = {
            name,
            job: job || null,
            level: level ? parseInt(level) : null,
            isDefault
        };
        
        await apiRequest('/me/characters', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        // 關閉 Modal
        closeModal('createCharacterModal');
        
        // 顯示成功訊息
        showSuccessMessage('✅ 角色建立成功！');
        
        // 重新載入列表
        loadMyCharacters();
        
    } catch (error) {
        console.error('建立角色失敗:', error);
        alert('建立角色失敗: ' + (error.error || error.message || JSON.stringify(error)));
    }
}

async function deleteCharacter(id) {
    if (!confirm('確定要刪除這個角色嗎？')) {
        return;
    }
    
    try {
        await apiRequest(`/me/characters/${id}`, {
            method: 'DELETE'
        });
        
        loadMyCharacters();
        
    } catch (error) {
        console.error('刪除角色失敗:', error);
    }
}

// 編輯角色
let editingCharacterId = null;

async function editCharacter(id) {
    try {
        // 載入角色資料
        const characters = await apiRequest('/me/characters');
        const character = characters.find(c => c.id === id);
        
        if (!character) {
            alert('找不到角色資料');
            return;
        }
        
        // 填入表單
        editingCharacterId = id;
        document.getElementById('editCharName').value = character.name || '';
        document.getElementById('editCharJob').value = character.job || '';
        document.getElementById('editCharLevel').value = character.level || '';
        document.getElementById('editCharIsDefault').checked = character.isDefault || false;
        
        // 顯示 Modal
        document.getElementById('editCharacterModal').classList.add('show');
        
    } catch (error) {
        console.error('載入角色資料失敗:', error);
        alert('載入角色資料失敗');
    }
}

async function saveCharacterEdit() {
    if (!editingCharacterId) {
        alert('錯誤：沒有選擇要編輯的角色');
        return;
    }
    
    try {
        const name = document.getElementById('editCharName').value.trim();
        const job = document.getElementById('editCharJob').value;
        const level = document.getElementById('editCharLevel').value;
        const isDefault = document.getElementById('editCharIsDefault').checked;
        
        if (!name) {
            alert('請輸入角色名稱');
            return;
        }
        
        const data = {
            name,
            job: job || null,
            level: level ? parseInt(level) : null,
            isDefault
        };
        
        await apiRequest(`/me/characters/${editingCharacterId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        
        // 關閉 Modal
        closeModal('editCharacterModal');
        editingCharacterId = null;
        
        // 顯示成功訊息
        showSuccessMessage('✅ 角色更新成功！');
        
        // 重新載入列表
        loadMyCharacters();
        
    } catch (error) {
        console.error('更新角色失敗:', error);
        alert('更新角色失敗: ' + (error.message || JSON.stringify(error)));
    }
}

// ==================== 遠征管理 ====================

// 開啟建立遠征 Modal
async function openCreateRaidModal() {
    // 清空表單
    document.getElementById('raidBoss').value = '';
    document.getElementById('raidSubtitle').value = '';
    document.getElementById('raidStartTime').value = '';
    document.getElementById('raidCharacter').value = '';
    
    // 載入角色列表
    await loadCharactersForRaidCreation();
    
    // 顯示 Modal
    document.getElementById('createRaidModal').classList.add('show');
}

async function loadRaids() {
    try {
        const raids = await apiRequest('/raids', { noAuth: true });
        displayRaids(raids);
    } catch (error) {
        console.error('載入遠征失敗:', error);
        const container = document.getElementById('raidsList');
        if (container) {
            container.innerHTML = '<p style="color: #e74c3c; padding: 20px; text-align: center;">❌ 載入失敗，請稍後再試</p>';
        }
    }
}

function displayRaids(raids) {
    const container = document.getElementById('raidsList');
    if (!container) return;
    
    // 後端已過濾過期遠征，直接顯示全部
    if (raids.length === 0) {
        container.innerHTML = '<p style="color: #999; padding: 20px; text-align: center;">目前沒有遠征活動</p>';
        return;
    }
    
    // 按時間排序
    raids.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
    
    // 為每個遠征載入報名人數
    raids.forEach(async (raid) => {
        try {
            const signups = await apiRequest(`/raids/${raid.id}/signups`, { noAuth: true });
            raid.signupCount = signups.length;
            updateRaidCard(raid);
        } catch (error) {
            console.error('載入報名人數失敗:', error);
        }
    });
    
    container.innerHTML = raids.map(raid => createRaidCard(raid)).join('');
}

// 取得星期幾的中文名稱
function getWeekDayName(date) {
    const days = ['日', '一', '二', '三', '四', '五', '六'];
    return '週' + days[date.getDay()];
}

function createRaidCard(raid) {
    const signupCount = raid.signupCount || 0;
    const isFull = signupCount >= 6;
    const raidDate = new Date(raid.startTime);
    const weekDay = getWeekDayName(raidDate);
    
    return `
        <div class="raid-card" id="raid-${raid.id}" onclick="handleRaidCardClick(event, ${raid.id})" style="cursor: pointer;">
            <div style="display: flex; justify-content: space-between; align-items: start;">
                <h4>🎯 ${raid.title}</h4>
                <span class="signup-count ${isFull ? 'full' : ''}" style="${isFull ? 'background: #e74c3c;' : ''}">
                    ${signupCount}/6 人
                </span>
            </div>
            <div class="raid-info">
                <div class="raid-info-item">
                    <strong>⏰ 時間:</strong> ${raidDate.toLocaleString('zh-TW', {
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit'
                    })} ${weekDay}
                </div>
                <div class="raid-info-item">
                    <strong>👤 建立人:</strong> ${raid.createdByName || '未知'}
                </div>
                ${raid.subtitle ? `<div class="raid-info-item"><strong>📝 備註:</strong> ${raid.subtitle}</div>` : ''}
            </div>
            <div class="action-buttons" onclick="event.stopPropagation()">
                <button onclick="signupForRaid(${raid.id})" ${isFull ? 'disabled' : ''}>
                    ${isFull ? '❌ 已滿員' : '✅ 我要參加'}
                </button>
                <button onclick="deleteRaid(${raid.id})" class="danger">🗑️ 刪除</button>
            </div>
            <div class="raid-signups" id="signups-${raid.id}">
                <div class="signups-loading">載入中...</div>
            </div>
        </div>
    `;
}

// 點擊卡片展開/收合報名名單（忽略按鈕點擊）
function handleRaidCardClick(event, raidId) {
    // 如果點擊的是按鈕，不處理
    if (event.target.tagName === 'BUTTON') return;
    toggleRaidSignups(raidId);
}

function updateRaidCard(raid) {
    const card = document.getElementById(`raid-${raid.id}`);
    if (card) {
        const newCard = document.createElement('div');
        newCard.innerHTML = createRaidCard(raid);
        card.replaceWith(newCard.firstElementChild);
    }
}

// 切換報名名單顯示
async function toggleRaidSignups(raidId) {
    const signupsDiv = document.getElementById(`signups-${raidId}`);
    const raidCard = document.getElementById(`raid-${raidId}`);
    
    if (signupsDiv.classList.contains('show')) {
        // 隱藏
        signupsDiv.classList.remove('show');
        raidCard.classList.remove('expanded');
    } else {
        // 顯示並載入資料
        signupsDiv.classList.add('show');
        raidCard.classList.add('expanded');
        await loadRaidSignups(raidId);
    }
}

// 載入報名名單
async function loadRaidSignups(raidId) {
    const signupsDiv = document.getElementById(`signups-${raidId}`);
    
    try {
        signupsDiv.innerHTML = '<div class="signups-loading">載入中...</div>';
        
        const signups = await apiRequest(`/raids/${raidId}/signups`, { noAuth: true });
        
        if (signups.length === 0) {
            signupsDiv.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">😴</div>
                    <p>目前沒有人報名</p>
                </div>
            `;
        } else {
            signupsDiv.innerHTML = `
                <div class="signups-header">
                    已報名人數: <span>${signups.length}</span> 人
                </div>
                ${signups.map((signup, index) => {
                    const isCurrentUser = currentUser && signup.userId === currentUser.userDbId;
                    return `
                    <div class="signup-list-item">
                        ${signup.userPicture ? 
                            `<img src="${signup.userPicture}" alt="${signup.userName}">` : 
                            `<div style="width: 50px; height: 50px; border-radius: 50%; background: #667eea; color: white; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: bold;">${index + 1}</div>`
                        }
                        <div class="signup-info">
                            <h4>⚔️ ${signup.characterName}</h4>
                            <p><strong>玩家:</strong> ${signup.userName}</p>
                            <p><strong>職業:</strong> ${signup.job || '未設定'} | <strong>等級:</strong> ${signup.level || '?'}</p>
                            ${isCurrentUser ? `
                                <button onclick="cancelSignup(${raidId})" class="cancel-signup-btn" style="margin-top: 8px; padding: 6px 12px; background: #e74c3c; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">
                                    ❌ 取消報名
                                </button>
                            ` : ''}
                        </div>
                    </div>
                `}).join('')}
            `;
        }
        
    } catch (error) {
        console.error('載入報名名單失敗:', error);
        signupsDiv.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">❌</div>
                <p>載入失敗: ${error.message || '請稍後再試'}</p>
            </div>
        `;
    }
}

// 報名遠征
async function signupForRaid(raidId) {
    try {
        // 載入角色列表
        const characters = await apiRequest('/me/characters');
        
        if (characters.length === 0) {
            alert('請先建立角色');
            switchTab('characters');
            return;
        }
        
        // 顯示角色選擇 Modal
        showCharacterSelectModal(raidId, characters);
        
    } catch (error) {
        console.error('載入角色失敗:', error);
        alert('載入角色失敗');
    }
}

// 顯示角色選擇 Modal
function showCharacterSelectModal(raidId, characters) {
    const modal = document.getElementById('characterSelectModal');
    const list = document.getElementById('characterSelectList');
    
    list.innerHTML = characters.map(char => `
        <div class="character-select-item" id="char-item-${char.id}" onclick="confirmSignup(${raidId}, ${char.id}, this)">
            <h4>⚔️ ${char.name} ${char.isDefault ? '⭐' : ''}</h4>
            <p><strong>職業:</strong> ${char.job || '未設定'} | <strong>等級:</strong> ${char.level || '?'}</p>
        </div>
    `).join('');
    
    modal.classList.add('show');
}

// 確認報名
async function confirmSignup(raidId, characterId, itemEl) {
    // 防止重複點擊
    const allItems = document.querySelectorAll('.character-select-item');
    allItems.forEach(el => {
        el.style.pointerEvents = 'none';
        el.style.opacity = '0.5';
    });
    if (itemEl) {
        itemEl.style.opacity = '1';
        itemEl.innerHTML += ' <span style="font-size:12px;color:#888;">⏳ 報名中...</span>';
    }

    try {
        const result = await apiRequest(`/raids/${raidId}/signup`, {
            method: 'POST',
            body: JSON.stringify({ characterId })
        });
        
        closeModal('characterSelectModal');
        showSuccessMessage('✅ 報名成功！');
        
        await loadRaids();
        
        const signupsDiv = document.getElementById(`signups-${raidId}`);
        if (signupsDiv && signupsDiv.classList.contains('show')) {
            await loadRaidSignups(raidId);
        }
        
    } catch (error) {
        console.error('報名失敗:', error);
        closeModal('characterSelectModal');
        alert('報名失敗: ' + (error.message || JSON.stringify(error)));
    }
}

// 取消報名
async function cancelSignup(raidId) {
    if (!confirm('確定要取消報名嗎？')) {
        return;
    }
    
    try {
        await apiRequest(`/raids/${raidId}/signup`, {
            method: 'DELETE'
        });
        
        // 顯示成功訊息
        showSuccessMessage('✅ 已取消報名');
        
        // 重新載入遠征列表和報名名單
        await loadRaids();
        
        // 如果報名名單已展開，重新載入
        const signupsDiv = document.getElementById(`signups-${raidId}`);
        if (signupsDiv && signupsDiv.classList.contains('show')) {
            await loadRaidSignups(raidId);
        }
        
    } catch (error) {
        console.error('取消報名失敗:', error);
        alert('取消報名失敗: ' + (error.message || JSON.stringify(error)));
    }
}

// 查看遠征報名名單
async function viewRaidSignups(raidId) {
    try {
        const signups = await apiRequest(`/raids/${raidId}/signups`, { noAuth: true });
        showSignupListModal(signups);
        
    } catch (error) {
        console.error('載入報名名單失敗:', error);
        alert('載入失敗');
    }
}

// 顯示報名名單 Modal
function showSignupListModal(signups) {
    const modal = document.getElementById('signupListModal');
    const content = document.getElementById('signupListContent');
    
    if (signups.length === 0) {
        content.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">😴</div>
                <p>目前沒有人報名</p>
            </div>
        `;
    } else {
        content.innerHTML = `
            <div style="margin-bottom: 15px; padding: 10px; background: #e8f4f8; border-radius: 4px; text-align: center;">
                <strong>已報名人數:</strong> <span style="color: #667eea; font-size: 18px; font-weight: bold;">${signups.length}</span> 人
            </div>
            ${signups.map((signup, index) => `
                <div class="signup-list-item">
                    ${signup.userPicture ? 
                        `<img src="${signup.userPicture}" alt="${signup.userName}">` : 
                        `<div style="width: 50px; height: 50px; border-radius: 50%; background: #667eea; color: white; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: bold;">${index + 1}</div>`
                    }
                    <div class="signup-info">
                        <h4>⚔️ ${signup.characterName}</h4>
                        <p><strong>玩家:</strong> ${signup.userName}</p>
                        <p><strong>職業:</strong> ${signup.job || '未設定'} | <strong>等級:</strong> ${signup.level || '?'}</p>
                    </div>
                </div>
            `).join('')}
        `;
    }
    
    modal.classList.add('show');
}

// 關閉 Modal
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    modal.classList.remove('show');
}

// 顯示成功訊息
function showSuccessMessage(message) {
    const messageDiv = document.createElement('div');
    messageDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #27ae60;
        color: white;
        padding: 15px 25px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        z-index: 2000;
        font-size: 16px;
        font-weight: bold;
        animation: slideInRight 0.3s;
    `;
    messageDiv.textContent = message;
    document.body.appendChild(messageDiv);
    
    setTimeout(() => {
        messageDiv.style.animation = 'slideOutRight 0.3s';
        setTimeout(() => messageDiv.remove(), 300);
    }, 3000);
}

// 點擊 Modal 外部關閉
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.classList.remove('show');
    }
}

// 載入角色到建立遠征的下拉選單
async function loadCharactersForRaidCreation() {
    try {
        const characters = await apiRequest('/me/characters');
        const select = document.getElementById('raidCharacter');
        
        if (!select) return;
        
        // 清空選項
        select.innerHTML = '<option value="">請選擇角色</option>';
        
        // 添加角色選項
        characters.forEach(char => {
            const option = document.createElement('option');
            option.value = char.id;
            option.textContent = `${char.name} ${char.isDefault ? '⭐' : ''} (${char.job || '未設定'} Lv.${char.level || '?'})`;
            if (char.isDefault) {
                option.selected = true; // 預設選擇預設角色
            }
            select.appendChild(option);
        });
    } catch (error) {
        console.error('載入角色失敗:', error);
    }
}

async function createRaid() {
    // 防止重複提交
    const submitButton = document.getElementById('createRaidBtn');
    if (submitButton.disabled) {
        return;
    }
    
    const boss = document.getElementById('raidBoss').value.trim();
    const subtitle = document.getElementById('raidSubtitle').value.trim();
    const startTime = document.getElementById('raidStartTime').value;
    const characterId = document.getElementById('raidCharacter').value;
    
    // 驗證輸入（在禁用按鈕之前）
    if (!boss) {
        alert('請選擇 Boss');
        return;
    }
    
    if (!startTime) {
        alert('請選擇開始時間');
        return;
    }
    
    if (!characterId) {
        alert('請選擇參加角色');
        return;
    }
    
    try {
        // 禁用按鈕並顯示 loading
        submitButton.disabled = true;
        submitButton.textContent = '⏳ 建立中...';
        
        const data = {
            title: boss,
            subtitle: subtitle || null,
            boss: boss,
            startTime: new Date(startTime).toISOString(),
            characterId: parseInt(characterId)
        };
        
        await apiRequest('/raids', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        // 關閉 Modal
        closeModal('createRaidModal');
        
        // 顯示成功訊息
        showSuccessMessage('✅ 遠征隊建立成功！');
        
        // 重新載入列表
        loadRaids();
        
    } catch (error) {
        console.error('建立遠征失敗:', error);
        alert('建立遠征失敗: ' + (error.error || error.message || JSON.stringify(error)));
    } finally {
        // 恢復按鈕狀態
        submitButton.disabled = false;
        submitButton.textContent = '⚔️ 建立遠征';
    }
}

async function deleteRaid(id) {
    if (!confirm('確定要刪除這個遠征嗎？所有報名記錄也會被刪除。')) {
        return;
    }
    
    try {
        await apiRequest(`/raids/${id}`, {
            method: 'DELETE'
        });
        
        loadRaids();
        
    } catch (error) {
        console.error('刪除遠征失敗:', error);
    }
}

// 頁面載入時初始化
window.addEventListener('DOMContentLoaded', () => {
    initializeLiff();
});
