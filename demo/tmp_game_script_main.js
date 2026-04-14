
        let selectedPokemonName = null;
        let isStarterSelectionMode = false;
        const BGM_STORAGE_KEY = 'pokemon_game_bgm_muted';
        const MENU_BGM_SRC = '/audio/menu-bgm.mp3';
        const BATTLE_BGM_SRC = '/audio/battle-bgm.mp3';

        function isBgmMuted() {
            return localStorage.getItem(BGM_STORAGE_KEY) === 'true';
        }

        function updateBgmButton() {
            const btn = document.getElementById('bgmToggleBtn');
            if (!btn) return;
            btn.textContent = isBgmMuted() ? '🔇 音乐关' : '🔊 音乐开';
        }

        function getCurrentBgmSrc() {
            const battleScreen = document.getElementById('battleScreen');
            const inBattle = battleScreen && battleScreen.style.display !== 'none';
            return inBattle ? BATTLE_BGM_SRC : MENU_BGM_SRC;
        }

        async function syncBgm() {
            const player = document.getElementById('bgmPlayer');
            if (!player) return;

            const targetSrc = getCurrentBgmSrc();
            if (player.dataset.src !== targetSrc) {
                player.src = targetSrc;
                player.dataset.src = targetSrc;
            }

            player.volume = 0.35;
            player.muted = isBgmMuted();
            updateBgmButton();

            if (!player.muted) {
                try {
                    await player.play();
                } catch (e) {
                }
            }
        }

        function toggleBgmMute() {
            const nextMuted = !isBgmMuted();
            localStorage.setItem(BGM_STORAGE_KEY, String(nextMuted));

            const player = document.getElementById('bgmPlayer');
            if (player) {
                player.muted = nextMuted;
                if (!nextMuted) {
                    syncBgm();
                }
            }

            updateBgmButton();
        }

        function initPageBgm() {
            updateBgmButton();
            syncBgm();

            const tryStart = () => syncBgm();
            window.addEventListener('click', tryStart, { once: true });
            window.addEventListener('keydown', tryStart, { once: true });
            document.addEventListener('visibilitychange', () => {
                if (!document.hidden) {
                    syncBgm();
                }
            });
        }
        
        window.onload = function() {
            loadPokemons();
            updateInventoryStatus();
            checkTrainerBattleEligibility();
            checkNewAchievements(); // 检查新成就
            initPageBgm();
        };
        
        async function loadPokemons() {
            try {
                const backpackResponse = await fetch('/game/backpack/pokemons');
                const backpackPokemons = await backpackResponse.json();
                
                if (backpackPokemons.length > 0) {
                    document.querySelector('#selectionScreen h2').textContent = '选择背包中的精灵出战';
                    displayBackpackPokemons(backpackPokemons);
                } else {
                    const storageResponse = await fetch('/game/storage');
                    const storageData = await storageResponse.json();
                    
                    if (storageData.storage && storageData.storage.length > 0) {
                        document.querySelector('#selectionScreen h2').textContent = '背包为空';
                        document.getElementById('pokemonGrid').innerHTML = `
                            <div style="grid-column: span 3; text-align: center; padding: 40px;">
                                <p style="color: #7f8c8d; font-size: 18px; margin-bottom: 20px;">🎒 背包是空的，但仓库中有 ${storageData.storage.length} 只精灵</p>
                                <p style="color: #666; font-size: 14px; margin-bottom: 30px;">请点击上方的"🏠 仓库管理"按钮，将精灵从仓库移入背包</p>
                            </div>`;
                    } else {
                        document.querySelector('#selectionScreen h2').textContent = '选择你的初始精灵';
                        const response = await fetch('/game/pokemons');
                        const pokemons = await response.json();
                        displayPokemons(pokemons);
                    }
                }
            } catch (error) {
                console.error('加载失败:', error);
            }
        }
        
        async function displayBackpackPokemons(pokemons) {
            const grid = document.getElementById('pokemonGrid');
            grid.innerHTML = '';
            
            for (const pokemon of pokemons) {
                const card = document.createElement('div');
                card.className = 'pokemon-card';
                card.id = `card-${pokemon.id}`;
                
                let movesHtml = '<div class="pokemon-moves"><strong>技能:</strong></div>';
                try {
                    const movesResponse = await fetch(`/api/pokemons/${pokemon.id}/moves`);
                    const moves = await movesResponse.json();
                    moves.forEach(pm => {
                        movesHtml += `<div class="move-tag move-${pm.move.type.toLowerCase()}">${pm.move.name} (威力:${pm.move.power})</div>`;
                    });
                } catch (e) {
                    movesHtml = '<div>加载技能失败</div>';
                }
                
                const imagePath = `/images/pokemon/${pokemon.name}.png`;
                
                card.innerHTML = `
                    <div class="pokemon-image-container">
                        <img src="${imagePath}" alt="${pokemon.name}" class="pokemon-image" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                        <div class="pokemon-placeholder" style="display:none;">${pokemon.name}</div>
                    </div>
                    <h3 class="type-${pokemon.type.toLowerCase()}">${pokemon.name}</h3>
                    <div style="font-size: 11px; margin-bottom: 3px;">${getRarityStars(pokemon.rarity)}</div>
                    <div>属性: ${getTypeName(pokemon.type)}</div>
                    <div>等级: Lv.${pokemon.level}</div>
                    <div class="pokemon-stats">
                        <div>HP: ${pokemon.maxHp} | MP: ${pokemon.maxMp}</div>
                        <div>攻击: ${pokemon.attack} | 防御: ${pokemon.defense} | 速度: ${pokemon.speed}</div>
                    </div>
                    <div style="font-size: 12px; color: #666; margin-bottom: 8px;">天赋: ${getTalentText(pokemon.talents || pokemon.talentNames, pokemon.templatePreview)}</div>
                    <div class="moves-list">${movesHtml}</div>
                    <div style="display:flex; gap:8px; justify-content:center; margin-top: 10px; flex-wrap: wrap;">
                        <button class="select-btn" onclick="selectPokemon('${pokemon.name}')">选择</button>
                        <button class="select-btn" style="background: linear-gradient(135deg, #16a085, #1abc9c);" onclick="showPokemonDetailById(${pokemon.id}, ${pokemon.templatePreview ? 'true' : 'false'})">技能详情</button>
                    </div>
                `;
                grid.appendChild(card);
            }
        }
        
        async function displayPokemons(pokemons) {
            const grid = document.getElementById('pokemonGrid');
            grid.innerHTML = '';
            
            for (const pokemon of pokemons) {
                const card = document.createElement('div');
                card.className = 'pokemon-card';
                card.id = `card-${pokemon.id}`;
                
                let movesHtml = '<div class="pokemon-moves"><strong>技能:</strong></div>';
                try {
                    const movesResponse = await fetch(`/api/pokemons/${pokemon.id}/moves`);
                    const moves = await movesResponse.json();
                    moves.forEach(pm => {
                        movesHtml += `<div class="move-tag move-${pm.move.type.toLowerCase()}">${pm.move.name} (威力:${pm.move.power})</div>`;
                    });
                } catch (e) {
                    movesHtml = '<div>加载技能失败</div>';
                }
                
                const imagePath = `/images/pokemon/${pokemon.name}.png`;
                
                card.innerHTML = `
                    <div class="pokemon-image-container">
                        <img src="${imagePath}" alt="${pokemon.name}" class="pokemon-image" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                        <div class="pokemon-placeholder" style="display:none;">${pokemon.name}</div>
                    </div>
                    <h3 class="type-${pokemon.type.toLowerCase()}">${pokemon.name}</h3>
                    <div style="font-size: 11px; margin-bottom: 3px;">${getRarityStars(pokemon.rarity)}</div>
                    <div>属性: ${getTypeName(pokemon.type)}</div>
                    <div>等级: Lv.${pokemon.level}</div>
                    <div class="pokemon-stats">
                        <div>HP: ${pokemon.maxHp} | MP: ${pokemon.maxMp}</div>
                        <div>攻击: ${pokemon.attack} | 防御: ${pokemon.defense} | 速度: ${pokemon.speed}</div>
                    </div>
                    <div style="font-size: 12px; color: #666; margin-bottom: 8px;">天赋: ${getTalentText(pokemon.talents || pokemon.talentNames, pokemon.templatePreview)}</div>
                    <div class="moves-list">${movesHtml}</div>
                    <div style="display:flex; gap:8px; justify-content:center; margin-top: 10px; flex-wrap: wrap;">
                        <button class="select-btn" onclick="selectPokemon('${pokemon.name}')">选择</button>
                        <button class="select-btn" style="background: linear-gradient(135deg, #16a085, #1abc9c);" onclick="showPokemonDetailById(${pokemon.id}, ${pokemon.templatePreview ? 'true' : 'false'})">技能详情</button>
                    </div>
                `;
                grid.appendChild(card);
            }
        }
        
        async function selectPokemon(name) {
            const isInitialSelection = isStarterSelectionMode === true;

            if (isInitialSelection) {
                try {
                    const response = await fetch('/game/select-starter', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: `pokemonName=${encodeURIComponent(name)}`
                    });
                    const data = await response.json();
                    if (data.success) {
                        alert(data.message || '????????');
                        window.location.reload();
                    } else {
                        alert(data.message || '????????');
                    }
                } catch (error) {
                    console.error('????????:', error);
                    alert('????????????');
                }
                return;
            }

            selectedPokemonName = name;
            document.querySelectorAll('.pokemon-card').forEach(card => card.classList.remove('selected'));
            document.querySelectorAll('.pokemon-card').forEach(card => {
                const title = card.querySelector('h3');
                if (title && title.textContent === name) {
                    card.classList.add('selected');
                }
            });

            const battleBtn = document.getElementById('battleBtn');
            const trainerBattleBtn = document.getElementById('trainerBattleBtn');
            const dungeonBattleBtn = document.getElementById('dungeonBattleBtn');
            if (battleBtn) battleBtn.disabled = false;
            if (trainerBattleBtn) trainerBattleBtn.disabled = false;
            if (dungeonBattleBtn) dungeonBattleBtn.disabled = false;
        }

        function showTrainerDifficultyModal() {
            document.getElementById('trainerDifficultyModal').style.display = 'flex';
        }

        function closeTrainerDifficultyModal() {
            document.getElementById('trainerDifficultyModal').style.display = 'none';
        }

        async function startTrainerBattle(difficultyLevel = 30) {
            if (!selectedPokemonName) {
                alert('请先选择一个精灵！');
                return;
            }
            
            try {
                const response = await fetch('/game/trainer/start', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `pokemonName=${encodeURIComponent(selectedPokemonName)}&difficultyLevel=${encodeURIComponent(difficultyLevel)}`
                });
                const data = await response.json();
                
                if (data.success) {
                    closeTrainerDifficultyModal();
                    showTrainerBattleScreen(data);
                } else {
                    alert(data.message);
                }
            } catch (error) {
                console.error('开始训练师对战失败:', error);
            }
        }
        
        function showTrainerBattleScreen(data) {
            document.getElementById('selectionScreen').style.display = 'none';
            document.getElementById('battleScreen').style.display = 'block';
            document.getElementById('battleRates').style.display = 'none';
            document.getElementById('battleScreen').setAttribute('data-trainer-battle', 'true');
            
            updateBattleDisplay(data);
            if (data.playerMoves) renderMoves(data.playerMoves, data.playerMp, data.playerPokemon.currentMp);
            showTrainerBattleInfo(data.enemyRemaining, data.playerRemaining, data.trainerDifficultyLevel, data.trainerRewardGold);
            syncBgm();
        }
        
        function showTrainerBattleInfo(enemyRemaining, playerRemaining, difficultyLevel = 30, rewardGold = 1000) {
            const battleScreen = document.getElementById('battleScreen');
            let infoDiv = document.getElementById('trainerBattleInfo');
            if (!infoDiv) {
                infoDiv = document.createElement('div');
                infoDiv.id = 'trainerBattleInfo';
                infoDiv.style.cssText = 'text-align: center; margin-bottom: 15px; padding: 10px; background: linear-gradient(135deg, #9b59b6, #8e44ad); color: white; border-radius: 10px; font-weight: bold;';
                battleScreen.insertBefore(infoDiv, battleScreen.firstChild);
            }
            infoDiv.innerHTML = `🏆 训练师对战 | 难度 Lv.${difficultyLevel} | 敌方剩余: ${enemyRemaining} 只 | 我方剩余: ${playerRemaining} 只<br><span style="font-size: 12px; opacity: 0.9;">当前胜利奖励：${rewardGold} 金币，对手全队固定 Lv.${difficultyLevel}</span>`;
        }
        
        function showTrainerResult(isWin, data) {
            const modal = document.getElementById('resultModal');
            const title = document.getElementById('resultTitle');
            const message = document.getElementById('resultMessage');
            
            if (isWin) {
                title.textContent = '🏆 训练师对战胜利！';
                title.className = 'win';
                let msg = `恭喜你击败了训练师！<br>击败敌方精灵: ${data.enemyDefeated} 只<br>我方战败精灵: ${data.playerDefeated} 只`;
                if (data.reward && data.reward > 0) {
                    msg += `<br><br>💰 获得 ${data.reward} 金币奖励！`;
                }
                message.innerHTML = msg + buildGrowthSummary(data);
            } else {
                title.textContent = '💔 训练师对战失败...';
                title.className = 'lose';
                message.innerHTML = `你输给了训练师...<br>击败敌方精灵: ${data.enemyDefeated || 0} 只<br>我方战败精灵: ${data.playerDefeated || 0} 只<br>不要气馁，再来一局吧！` + buildGrowthSummary(data);
            }
            
            modal.style.display = 'flex';
            // 更新金币显示
            updateInventoryStatus();
        }
        
        // 副本挑战相关显示函数
        function showDungeonBattleInfo(enemyRemaining, enemyDefeated) {
            const battleScreen = document.getElementById('battleScreen');
            let infoDiv = document.getElementById('dungeonBattleInfo');
            if (!infoDiv) {
                infoDiv = document.createElement('div');
                infoDiv.id = 'dungeonBattleInfo';
                infoDiv.style.cssText = 'text-align: center; margin-bottom: 15px; padding: 10px; background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; border-radius: 10px; font-weight: bold;';
                battleScreen.insertBefore(infoDiv, battleScreen.firstChild);
            }
            const bossName = battleScreen.getAttribute('data-dungeon-boss') || 'BOSS';
            infoDiv.innerHTML = `🏰 副本挑战 | ${bossName} | 已击败: ${enemyDefeated}/6 | 剩余: ${enemyRemaining} 只<br><span style="font-size: 12px; opacity: 0.9;">连续挑战 6 只同名 60 级 BOSS，强度固定拉满，通关可拿专属奖励</span>`;
        }
        
        function showDungeonResult(isWin, data) {
            const modal = document.getElementById('resultModal');
            const title = document.getElementById('resultTitle');
            const message = document.getElementById('resultMessage');
            
            if (isWin) {
                title.textContent = '🏰 副本通关！';
                title.className = 'win';
                let msg = `恭喜你通关副本！`;
                if (data.rewardPokemon) {
                    msg += `<br><br>🎁 获得精灵: ${data.rewardPokemon}`;
                }
                if (data.rewardGold) {
                    msg += `<br><br>💰 获得 ${data.rewardGold} 金币！`;
                }
                message.innerHTML = msg + buildGrowthSummary(data);
            } else {
                title.textContent = '💔 副本挑战失败...';
                title.className = 'lose';
                message.innerHTML = `副本挑战失败...<br>不要气馁，提升实力后再来挑战吧！` + buildGrowthSummary(data);
            }
            
            modal.style.display = 'flex';
            
            // 清除副本挑战标记
            const battleScreen = document.getElementById('battleScreen');
            battleScreen.removeAttribute('data-dungeon-battle');
            battleScreen.removeAttribute('data-dungeon-boss');
            const infoDiv = document.getElementById('dungeonBattleInfo');
            if (infoDiv) infoDiv.remove();
            
            // 更新金币和背包显示
            updateInventoryStatus();
            loadPokemons();
        }
        
        function showTrainerBattleComingSoon() {
            alert('训练师对战功能即将开放，敬请期待！');
        }
        
        function showBattleScreen(data, isWildBattle = true) {
            document.getElementById('selectionScreen').style.display = 'none';
            document.getElementById('battleScreen').style.display = 'block';
            
            // 只有野外捕宠显示捕获率和逃跑率
            const battleRates = document.getElementById('battleRates');
            if (battleRates) {
                battleRates.style.display = isWildBattle ? 'block' : 'none';
            }
            
            updateBattleDisplay(data);
            if (data.playerMoves) renderMoves(data.playerMoves, data.playerMp, data.playerPokemon.currentMp);
            
            // 只有野外捕宠才刷新精灵球
            if (isWildBattle) {
                refreshBattleBallCounts();
                selectPokeBall('BASIC');
            }

            syncBgm();
        }
        
        function updateBattleDisplay(data) {
            const player = data.playerPokemon;
            const enemy = data.enemyPokemon;
            
            // 防御性检查
            if (!player || !enemy) {
                console.error('战斗数据显示错误：player或enemy为空', data);
                return;
            }
            
            const playerImage = document.getElementById('playerImage');
            const playerPlaceholder = document.getElementById('playerImagePlaceholder');
            if (playerImage && player) {
                playerImage.style.display = 'block';
                playerImage.src = `/images/pokemon/${player.name}.png`;
                if (playerPlaceholder) {
                    playerPlaceholder.textContent = player.name;
                    playerPlaceholder.style.display = 'none';
                }
            }
            
            const enemyImage = document.getElementById('enemyImage');
            const enemyPlaceholder = document.getElementById('enemyImagePlaceholder');
            if (enemyImage && enemy) {
                enemyImage.style.display = 'block';
                enemyImage.src = `/images/pokemon/${enemy.name}.png`;
                if (enemyPlaceholder) {
                    enemyPlaceholder.textContent = enemy.name;
                    enemyPlaceholder.style.display = 'none';
                }
            }
            
            document.getElementById('playerName').textContent = player.name || '未知';
            document.getElementById('playerLevelText').textContent = '等级: Lv.' + (player.level || 1);
            document.getElementById('playerExpText').textContent = formatExpText(player);
            document.getElementById('playerRarity').innerHTML = getRarityStars(player.rarity || 1);
            const playerHp = player.currentHp ?? data.playerHp ?? 0;
            const playerMaxHp = player.maxHp ?? data.playerMaxHp ?? 100;
            document.getElementById('playerHpText').textContent = `HP: ${playerHp}/${playerMaxHp}`;
            document.getElementById('playerHpBar').style.width = `${Math.max(0, Math.min(100, (playerHp / playerMaxHp) * 100))}%`;
            
            const playerMp = data.playerMp !== undefined ? data.playerMp : (player.currentMp ?? 0);
            const playerMaxMp = data.playerMaxMp !== undefined ? data.playerMaxMp : (player.maxMp ?? 50);
            document.getElementById('playerMpText').textContent = `MP: ${playerMp}/${playerMaxMp}`;
            document.getElementById('playerMpBar').style.width = `${Math.max(0, Math.min(100, (playerMp / playerMaxMp) * 100))}%`;
            
            const playerEff = data.playerEffectiveness !== undefined ? data.playerEffectiveness : 1.0;
            const playerEffColor = getEffectivenessColor(playerEff);
            document.getElementById('playerType').innerHTML = `\u5c5e\u6027: ${getTypeName(player.type)} <span style="color:${playerEffColor};font-weight:bold;">${formatEffectiveness(playerEff)}</span>${formatBattleBuffText(data.playerBattleBuffs)}`;
            
            document.getElementById('enemyName').textContent = enemy.name || '未知';
            document.getElementById('enemyLevelText').textContent = '等级: Lv.' + (enemy.level || 1);
            document.getElementById('enemyExpText').textContent = (enemy.level || 1) >= (enemy.maxLevel || 60) ? '成长型对手 | 已达高阶' : '成长型对手 | 强度随模式提升';
            document.getElementById('enemyRarity').innerHTML = getRarityStars(enemy.rarity || 1);
            const enemyHp = enemy.currentHp ?? data.enemyHp ?? 0;
            const enemyMaxHp = enemy.maxHp ?? data.enemyMaxHp ?? 100;
            document.getElementById('enemyHpText').textContent = `HP: ${enemyHp}/${enemyMaxHp}`;
            document.getElementById('enemyHpBar').style.width = `${Math.max(0, Math.min(100, (enemyHp / enemyMaxHp) * 100))}%`;
            
            const enemyMp = data.enemyMp !== undefined ? data.enemyMp : (enemy.currentMp ?? 0);
            const enemyMaxMp = data.enemyMaxMp !== undefined ? data.enemyMaxMp : (enemy.maxMp ?? 50);
            document.getElementById('enemyMpText').textContent = `MP: ${enemyMp}/${enemyMaxMp}`;
            document.getElementById('enemyMpBar').style.width = `${Math.max(0, Math.min(100, (enemyMp / enemyMaxMp) * 100))}%`;
            
            const enemyEff = data.enemyEffectiveness !== undefined ? data.enemyEffectiveness : 1.0;
            const enemyEffColor = getEffectivenessColor(enemyEff);
            document.getElementById('enemyType').innerHTML = `\u5c5e\u6027: ${getTypeName(enemy.type)} <span style="color:${enemyEffColor};font-weight:bold;">${formatEffectiveness(enemyEff)}</span>${formatBattleBuffText(data.enemyBattleBuffs)}`;
            
            if (data.battleLog) {
                const logDiv = document.getElementById('battleLog');
                logDiv.innerHTML = '';
                data.battleLog.forEach(log => {
                    const div = document.createElement('div');
                    div.textContent = log;
                    if (log.includes('回合')) div.setAttribute('data-turn', 'true');
                    else if (log.includes('【玩家】')) div.setAttribute('data-player', 'true');
                    else if (log.includes('【敌方】')) div.setAttribute('data-enemy', 'true');
                    logDiv.appendChild(div);
                });
                logDiv.scrollTop = logDiv.scrollHeight;
            }
            
            updateBattleRates();
        }
        
        function formatBattleBuffText(buffList) {
            if (!buffList || buffList.length === 0) {
                return '';
            }
            return `<div style="margin-top:4px;font-size:12px;color:#8e44ad;">\u72b6\u6001: ${buffList.join(' / ')}</div>`;
        }

        function getMoveSummary(move) {
            if (move.description) {
                return move.description;
            }
            const accuracyText = move.accuracy !== undefined ? `??:${move.accuracy}% | ` : '';
            return `\u5a01\u529b:${move.power ?? 0} | ${accuracyText}MP:${move.mpCost ?? 0}`;
        }

        function renderMoves(moves, playerMp, pokemonCurrentMp) {
            const container = document.getElementById('movesContainer');
            container.innerHTML = '';
            
            if (!moves || moves.length === 0) {
                container.innerHTML = '<div style="grid-column: span 2; text-align: center; color: #666;">\u6ca1\u6709\u53ef\u7528\u6280\u80fd</div>';
                return;
            }
            
            const currentMp = pokemonCurrentMp !== undefined ? pokemonCurrentMp : playerMp;
            
            moves.forEach(move => {
                const btn = document.createElement('button');
                btn.className = 'move-btn';
                const canUse = currentMp >= move.mpCost;
                btn.disabled = !canUse;
                btn.innerHTML = `<div>${move.name}</div><div style="font-size: 0.8em;">${getMoveSummary(move)} | MP:${move.mpCost}</div>`;
                if (canUse) {
                    btn.onclick = () => useMove(move.id);
                }
                container.appendChild(btn);
            });
        }
        
        async function useMove(moveId) {
            try {
                // 判断是否为训练师对战或副本挑战
                const isTrainerBattle = document.getElementById('battleScreen').getAttribute('data-trainer-battle') === 'true';
                const isDungeonBattle = document.getElementById('battleScreen').getAttribute('data-dungeon-battle') === 'true';
                
                let url;
                if (isTrainerBattle) {
                    url = '/game/trainer/attack';
                } else if (isDungeonBattle) {
                    url = '/game/dungeon/attack';
                } else {
                    url = '/game/attack';
                }
                
                const response = await fetch(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `moveId=${encodeURIComponent(moveId)}`
                });
                const data = await response.json();
                
                if (data.battleLog) {
                    const logDiv = document.getElementById('battleLog');
                    logDiv.innerHTML = '';
                    data.battleLog.forEach(log => {
                        const div = document.createElement('div');
                        div.textContent = log;
                        if (log.includes('回合')) div.setAttribute('data-turn', 'true');
                        else if (log.includes('【玩家】')) div.setAttribute('data-player', 'true');
                        else if (log.includes('【敌方】')) div.setAttribute('data-enemy', 'true');
                        else if (log.includes('★') || log.includes('🎉')) div.setAttribute('data-special', 'true');
                        logDiv.appendChild(div);
                    });
                    logDiv.scrollTop = logDiv.scrollHeight;
                }
                
                if (data.playerHp !== undefined) {
                    document.getElementById('playerHpText').textContent = `HP: ${data.playerHp}/${data.playerMaxHp || 200}`;
                    document.getElementById('playerHpBar').style.width = `${(data.playerHp / (data.playerMaxHp || 200)) * 100}%`;
                }
                
                updateBattleDisplay(data);
                if (!isTrainerBattle && !isDungeonBattle) {
                    updateBattleRates();
                }
                
                // 刷新技能按钮状态（MP可能变化）
                if (data.playerMoves) {
                    const playerMp = data.playerMp !== undefined ? data.playerMp : data.playerPokemon?.currentMp;
                    renderMoves(data.playerMoves, playerMp, playerMp);
                }
                
                // 更新训练师对战信息
                if (data.isTrainerBattle && data.enemyRemaining !== undefined) {
                    showTrainerBattleInfo(data.enemyRemaining, 6 - data.playerDefeated, data.trainerDifficultyLevel, data.trainerRewardGold);
                }
                
                // 更新副本挑战信息
                if (data.isDungeonBattle && data.enemyRemaining !== undefined) {
                    showDungeonBattleInfo(data.enemyRemaining, data.enemyDefeated);
                }
                
                if (data.forceSwitch) {
                    // 先检查是否有可更换的精灵
                    const checkResponse = await fetch('/game/battle/switchable');
                    const switchablePokemons = await checkResponse.json();
                    
                    if (!switchablePokemons || switchablePokemons.length === 0) {
                        // 没有可更换精灵，显示战斗失败
                        setTimeout(() => {
                            const modal = document.getElementById('resultModal');
                            document.getElementById('resultTitle').textContent = '💀 战斗失败';
                            document.getElementById('resultTitle').className = 'lose';
                            document.getElementById('resultMessage').textContent = '你所有的精灵都已失去战斗能力！';
                            modal.style.display = 'flex';
                        }, 500);
                    } else {
                        setTimeout(() => {
                            alert('当前精灵已失去战斗能力，请选择下一只出战精灵！');
                            showSwitchPokemonModal(true);
                        }, 500);
                    }
                } else if (data.battleOver) {
                    if (data.isTrainerBattle) {
                        const isWin = isPlayerWinner(data.winner, data.playerPokemon?.name || document.getElementById('playerName').textContent);
                        setTimeout(() => showTrainerResult(isWin, data), 1000);
                    } else if (data.isDungeonBattle) {
                        const isWin = isPlayerWinner(data.winner, data.playerPokemon?.name || document.getElementById('playerName').textContent);
                        setTimeout(() => showDungeonResult(isWin, data), 1000);
                    } else {
                        const currentPlayerName = document.getElementById('playerName').textContent;
                        const isPlayerWin = isPlayerWinner(data.winner, currentPlayerName);
                        setTimeout(() => showResult(isPlayerWin, data), 1000);
                    }
                }
            } catch (error) {
                console.error('攻击失败:', error);
            }
        }
        
        function showResult(isWin, data) {
            const modal = document.getElementById('resultModal');
            const title = document.getElementById('resultTitle');
            const message = document.getElementById('resultMessage');
            
            if (isWin) {
                title.textContent = '🎉 胜利！';
                title.className = 'win';
                let msg = '恭喜你赢得了对战！';
                if (data.reward && data.reward > 0) {
                    msg += `<br><br>💰 获得 ${data.reward} 金币奖励！`;
                }
                message.innerHTML = msg + buildGrowthSummary(data);
            } else {
                title.textContent = '💔 战败...';
                title.className = 'lose';
                message.innerHTML = '不要气馁，再来一局吧！' + buildGrowthSummary(data);
            }
            
            modal.style.display = 'flex';
            // 更新金币显示
            updateInventoryStatus();
        }
        
        function getTypeName(type) {
            const typeNames = { 'NORMAL': '普通', 'FIRE': '火', 'WATER': '水', 'GRASS': '草' };
            return typeNames[type] || type;
        }
        
        /**
         * 获取稀有度星级显示
         * @param {number} rarity - 稀有度 1-5
         * @returns {string} 星级HTML字符串
         */
        function formatExpText(pokemon) {
            if (!pokemon) return '经验: --';
            if ((pokemon.level || 1) >= (pokemon.maxLevel || 60)) {
                return '经验: 已满级';
            }
            const progress = pokemon.expProgress || 0;
            const required = pokemon.expRequired || 0;
            const toNext = pokemon.expToNext || 0;
            return `经验: ${progress}/${required} | 距下级 ${toNext}`;
        }

        function buildGrowthSummary(data) {
            if (!data) return '';
            const parts = [];
            if (data.lastExpGained && data.lastExpGained > 0) {
                parts.push(`✨ 本次获得经验: ${data.lastExpGained}`);
            }
            if (data.totalExpGained && data.totalExpGained > 0 && data.totalExpGained !== data.lastExpGained) {
                parts.push(`📈 本场累计经验: ${data.totalExpGained}`);
            }
            if (data.playerPokemon && data.playerPokemon.level) {
                parts.push(`🎓 当前等级: Lv.${data.playerPokemon.level}`);
            }
            if (data.lastLevelUp && data.lastLevelAfter) {
                parts.push(`🌟 已升级至 Lv.${data.lastLevelAfter}`);
            } else if (data.playerExpToNext !== undefined && data.playerExpToNext > 0) {
                parts.push(`⏳ 距离下一级还需 ${data.playerExpToNext} 经验`);
            }
            if (parts.length === 0) return '';
            return `<br><br>${parts.join('<br>')}`;
        }
        function getRarityStars(rarity) {
            const r = rarity || 1;
            const colors = {
                1: '#95a5a6',  // 普通 - 灰色
                2: '#3498db',  // 稀有 - 蓝色
                3: '#9b59b6',  // 精良 - 紫色
                4: '#f39c12',  // 史诗 - 橙色
                5: '#e74c3c'   // 传说 - 红色
            };
            const color = colors[r] || colors[1];
            return '<span style="color:' + color + ';">' + '★'.repeat(r) + '☆'.repeat(5 - r) + '</span>';
        }
        
        function getTalentText(talentNames, templatePreview = false) {
            if (templatePreview) {
                return '获取后随机生成 0-2 个天赋';
            }
            if (!talentNames || talentNames.length === 0) {
                return '无';
            }
            return talentNames.map(talent => typeof talent === 'string' ? talent : (talent?.name || '')).filter(Boolean).join('、');
        }

        async function showPokemonDetailById(pokemonId, templatePreview = false) {
            const modal = document.getElementById('pokemonDetailModal');
            const content = document.getElementById('pokemonDetailContent');
            modal.style.display = 'flex';
            content.innerHTML = '<p>加载中...</p>';

            try {
                const response = await fetch(`/game/pokemon/detail?pokemonId=${pokemonId}`);
                const data = await response.json();
                if (!data.success || !data.pokemon) {
                    content.innerHTML = `<p style="color: #e74c3c;">${data.message || '加载失败'}</p>`;
                    return;
                }

                const pokemon = data.pokemon;
                const imagePath = `/images/pokemon/${pokemon.name}.png`;
                const moves = pokemon.moves || [];
                const talents = pokemon.talents || [];
                const moveHtml = moves.length > 0
                    ? moves.map(move => `
                        <div style="border: 1px solid #e8e8e8; border-radius: 10px; padding: 10px 12px; background: #fafafa; margin-bottom: 8px; text-align: left;">
                            <div style="font-weight: bold; color: ${getTypeColor(move.type)}; margin-bottom: 4px;">${move.name} <span style="font-size: 12px; color: #666;">[${getTypeName(move.type)}]</span></div>
                            <div style="font-size: 12px; color: #666;">威力: ${move.power} | 命中: ${move.accuracy}% | MP消耗: ${move.mpCost}</div>
                        </div>`).join('')
                    : '<div style="color: #999;">暂无主动技能</div>';
                const talentHtml = templatePreview
                    ? '<div style="color: #f39c12;">这只精灵在真正获得时，会随机生成 0-2 个天赋。</div>'
                    : talents.length > 0
                        ? talents.map(talent => `
                            <div style="border: 1px solid #e8e8e8; border-radius: 10px; padding: 10px 12px; background: #f6fffb; margin-bottom: 8px; text-align: left;">
                                <div style="font-weight: bold; color: ${talent.color || '#16a085'}; margin-bottom: 4px;">${talent.name} <span style="font-size: 12px; color: #666;">[${talent.rarity || '普通'}]</span></div>
                                <div style="font-size: 12px; color: #666;">${talent.description}</div>
                            </div>`).join('')
                        : '<div style="color: #999;">暂无天赋</div>';

                content.innerHTML = `
                    <div style="display:grid; grid-template-columns: minmax(220px, 260px) 1fr; gap: 18px; align-items: start;">
                        <div style="background: linear-gradient(145deg, #f9f9f9, #f0f0f0); border-radius: 16px; padding: 18px; text-align: center;">
                            <div style="width: 110px; height: 110px; margin: 0 auto 12px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 10px;">
                                <img src="${imagePath}" alt="${pokemon.name}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                            </div>
                            <div style="font-size: 22px; font-weight: bold; color: ${getTypeColor(pokemon.type)}; margin-bottom: 6px;">${pokemon.name}</div>
                            <div style="margin-bottom: 8px;">${getRarityStars(pokemon.rarity)}</div>
                            <div style="font-size: 13px; color: #666; margin-bottom: 10px;">${getTypeName(pokemon.type)} | Lv.${pokemon.level}</div>
                            <div style="text-align: left; background: white; border-radius: 12px; padding: 12px; font-size: 13px; color: #444; line-height: 1.7;">
                                <div>HP: ${pokemon.currentHp}/${pokemon.maxHp}</div>
                                <div>MP: ${pokemon.currentMp}/${pokemon.maxMp}</div>
                                <div>攻击: ${pokemon.attack}</div>
                                <div>防御: ${pokemon.defense}</div>
                                <div>速度: ${pokemon.speed}</div>
                                <div>经验: ${(pokemon.level || 1) >= (pokemon.maxLevel || 60) ? '已满级' : `${pokemon.expProgress || 0}/${pokemon.expRequired || 0}`}</div>
                            </div>
                        </div>
                        <div>
                            <div style="background: #fff; border-radius: 14px; padding: 16px; margin-bottom: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.06);">
                                <h3 style="margin: 0 0 12px; color: #2c3e50;">⚔️ 主动技能</h3>
                                ${moveHtml}
                            </div>
                            <div style="background: #fff; border-radius: 14px; padding: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.06);">
                                <h3 style="margin: 0 0 12px; color: #2c3e50;">✨ 被动天赋</h3>
                                ${talentHtml}
                            </div>
                        </div>
                    </div>`;
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }

        function closePokemonDetailModal() {
            document.getElementById('pokemonDetailModal').style.display = 'none';
        }

        function formatEffectiveness(value) {
            if (value === 2.0) return '*2.0';
            if (value === 0.5) return '*0.5';
            if (value === 1.0) return '*1.0';
            return '*' + value.toFixed(1);
        }

        function isPlayerWinner(winner, playerName) {
            if (!winner) return false;
            const normalizedWinner = String(winner).trim().toLowerCase();
            const normalizedPlayerName = String(playerName || '').trim().toLowerCase();
            return normalizedWinner === 'player'
                || normalizedWinner === '玩家'
                || normalizedWinner === normalizedPlayerName;
        }
        
        function getEffectivenessColor(value) {
            if (value > 1.0) return '#27ae60';
            if (value < 1.0) return '#e74c3c';
            return '#7f8c8d';
        }
        
        async function showBackpack() {
            const modal = document.getElementById('backpackModal');
            const content = document.getElementById('backpackContent');
            modal.style.display = 'flex';
            
            try {
                const response = await fetch('/game/backpack');
                const items = await response.json();
                
                if (items.length === 0) {
                    content.innerHTML = '<p style="text-align: center; color: #7f8c8d;">背包是空的，快去捕获精灵吧！</p>';
                } else {
                    let html = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">';
                    items.forEach(item => {
                        const typeColor = getTypeColor(item.pokemonType);
                        const imagePath = `/images/pokemon/${item.pokemonName}.png`;
                        const currentHp = parseInt(item.currentHp) || 0;
                        const maxHp = parseInt(item.maxHp) || 1;
                        const currentMp = parseInt(item.currentMp) || 0;
                        const maxMp = parseInt(item.maxMp) || 1;
                        html += `
                            <div style="border: 1px solid #ddd; border-radius: 12px; padding: 15px; background: #f9f9f9; text-align: center;">
                                <div style="width: 80px; height: 80px; margin: 0 auto 10px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 8px;">
                                    <img src="${imagePath}" alt="${item.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; margin-bottom: 5px;">${item.pokemonName} ${item.used ? '<span style="color: #27ae60;">[已出战]</span>' : ''}</div>
                                <div style="font-size: 12px; color: #666; margin-bottom: 5px;">等级: Lv.${item.level} | 属性: ${getTypeName(item.pokemonType)}</div>
                                <div style="font-size: 11px; color: #999; margin-bottom: 5px;">❤️ HP: ${currentHp}/${maxHp} | 💧 MP: ${currentMp}/${maxMp}</div>
                                <div style="font-size: 10px; color: #bbb;">捕获时间: ${new Date(item.caughtTime).toLocaleString()}</div>
                            </div>`;
                    });
                    html += '</div>';
                    html += `<p style="text-align: center; margin-top: 15px; color: #7f8c8d;">共 ${items.length} 只精灵</p>`;
                    content.innerHTML = html;
                }
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }
        
        function closeBackpack() {
            document.getElementById('backpackModal').style.display = 'none';
        }
        
        // 查看背包精灵弹窗
        async function showBackpackPokemonsModal() {
            const modal = document.getElementById('backpackPokemonsModal');
            const content = document.getElementById('backpackPokemonsContent');
            modal.style.display = 'flex';
            
            try {
                const response = await fetch('/game/backpack');
                const items = await response.json();
                
                if (items.length === 0) {
                    content.innerHTML = '<p style="text-align: center; color: #7f8c8d;">背包中没有精灵</p>';
                } else {
                    let html = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px;">';
                    items.forEach(item => {
                        const typeColor = getTypeColor(item.pokemonType);
                        const imagePath = `/images/pokemon/${item.pokemonName}.png`;
                        const rarity = item.rarity || 1;
                        const currentHp = parseInt(item.currentHp) || 0;
                        const maxHp = parseInt(item.maxHp) || 1;
                        const currentMp = parseInt(item.currentMp) || 0;
                        const maxMp = parseInt(item.maxMp) || 1;
                        const hpPercent = Math.min(100, Math.max(0, (currentHp / maxHp) * 100));
                        const mpPercent = Math.min(100, Math.max(0, (currentMp / maxMp) * 100));
                        html += `
                            <div style="border: 2px solid #ddd; border-radius: 12px; padding: 15px; background: linear-gradient(145deg, #f9f9f9, #f0f0f0); text-align: center;">
                                <div style="width: 70px; height: 70px; margin: 0 auto 10px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 8px;">
                                    <img src="${imagePath}" alt="${item.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; margin-bottom: 3px;">${item.pokemonName} ${item.used ? '<span style="color: #27ae60;">[出战]</span>' : ''}</div>
                                <div style="font-size: 11px; margin-bottom: 5px;">${getRarityStars(rarity)}</div>
                                <div style="font-size: 12px; color: #666; margin-bottom: 8px;">${getTypeName(item.pokemonType)} | Lv.${item.level}</div>
                                <div style="background: white; border-radius: 8px; padding: 8px; margin-bottom: 5px;">
                                    <div style="font-size: 11px; color: #e74c3c; margin-bottom: 3px;">❤️ HP: ${currentHp}/${maxHp}</div>
                                    <div style="width: 100%; height: 6px; background: #eee; border-radius: 3px; overflow: hidden;">
                                        <div style="width: ${hpPercent}%; height: 100%; background: linear-gradient(90deg, #e74c3c, #c0392b);"></div>
                                    </div>
                                </div>
                                <div style="background: white; border-radius: 8px; padding: 8px; margin-bottom: 8px;">
                                    <div style="font-size: 11px; color: #3498db; margin-bottom: 3px;">💧 MP: ${currentMp}/${maxMp}</div>
                                    <div style="width: 100%; height: 6px; background: #eee; border-radius: 3px; overflow: hidden;">
                                        <div style="width: ${mpPercent}%; height: 100%; background: linear-gradient(90deg, #3498db, #2980b9);"></div>
                                    </div>
                                </div>
                                <div style="font-size: 12px; color: #666; margin-bottom: 8px;">天赋: ${getTalentText(item.talents || item.talentNames)}</div>
                                <button class="restart-btn" onclick="showPokemonDetailById(${item.pokemonId});" style="margin-top: 0; padding: 8px 18px; font-size: 12px; background: linear-gradient(135deg, #16a085, #1abc9c);">技能详情</button>
                            </div>`;
                    });
                    html += '</div>';
                    html += `<p style="text-align: center; margin-top: 15px; color: #7f8c8d;">共 ${items.length} 只精灵</p>`;
                    content.innerHTML = html;
                }
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }
        
        function closeBackpackPokemonsModal() {
            document.getElementById('backpackPokemonsModal').style.display = 'none';
        }
        
        // 查看背包道具弹窗
        async function showBackpackItemsModal() {
            const modal = document.getElementById('backpackItemsModal');
            const content = document.getElementById('backpackItemsContent');
            modal.style.display = 'flex';
            
            try {
                const response = await fetch('/game/inventory');
                const data = await response.json();
                
                const pokeBalls = data.pokeBalls || {};
                const healingItems = data.healingItems || {};
                const gold = data.gold || 0;
                
                let html = `
                    <div style="background: linear-gradient(135deg, #fff9e6, #ffe4b5); border-radius: 12px; padding: 20px; margin-bottom: 15px; text-align: center;">
                        <div style="font-size: 24px; margin-bottom: 5px;">💰</div>
                        <div style="font-size: 14px; color: #666;">当前金币</div>
                        <div style="font-size: 28px; font-weight: bold; color: #f39c12;">${gold}</div>
                    </div>
                    
                    <h3 style="color: #333; margin: 20px 0 15px; text-align: center;">🔮 精灵球</h3>
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-bottom: 20px;">
                        <div style="background: linear-gradient(135deg, #ffebee, #ffcdd2); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🔴</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">初级精灵球</div>
                            <div style="font-size: 24px; font-weight: bold; color: #e74c3c;">${pokeBalls.BASIC || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #e3f2fd, #bbdefb); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🔵</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">中级精灵球</div>
                            <div style="font-size: 24px; font-weight: bold; color: #3498db;">${pokeBalls.MEDIUM || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #f3e5f5, #e1bee7); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🟣</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">高级精灵球</div>
                            <div style="font-size: 24px; font-weight: bold; color: #9b59b6;">${pokeBalls.ADVANCED || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #fff8e1, #ffecb3); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🟡</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">大师球</div>
                            <div style="font-size: 24px; font-weight: bold; color: #f1c40f;">${pokeBalls.MASTER || 0}</div>
                        </div>
                    </div>
                    
                    <h3 style="color: #333; margin: 20px 0 15px; text-align: center;">🧪 背包道具</h3>
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px;">
                        <div style="background: linear-gradient(135deg, #ffebee, #ffcdd2); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🍎</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">生命果</div>
                            <div style="font-size: 12px; color: #999;">恢复50%HP</div>
                            <div style="font-size: 24px; font-weight: bold; color: #e74c3c;">${healingItems.LIFE_FRUIT || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #e8f5e9, #c8e6c9); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🍏</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">大生命果</div>
                            <div style="font-size: 12px; color: #999;">恢复100%HP</div>
                            <div style="font-size: 24px; font-weight: bold; color: #27ae60;">${healingItems.BIG_LIFE_FRUIT || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #e3f2fd, #bbdefb); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">💎</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">能量石</div>
                            <div style="font-size: 12px; color: #999;">恢复50%MP</div>
                            <div style="font-size: 24px; font-weight: bold; color: #3498db;">${healingItems.ENERGY_STONE || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #f3e5f5, #e1bee7); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">💠</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">大能量石</div>
                            <div style="font-size: 12px; color: #999;">恢复100%MP</div>
                            <div style="font-size: 24px; font-weight: bold; color: #9b59b6;">${healingItems.BIG_ENERGY_STONE || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #e0f2f1, #b2dfdb); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🌿</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">精华草</div>
                            <div style="font-size: 12px; color: #999;">恢复50%HP和50%MP</div>
                            <div style="font-size: 24px; font-weight: bold; color: #00b894;">${healingItems.ESSENCE_GRASS || 0}</div>
                        </div>
                        <div style="background: linear-gradient(135deg, #fff8e1, #ffecb3); border-radius: 12px; padding: 15px; text-align: center;">
                            <div style="font-size: 30px; margin-bottom: 5px;">🌱</div>
                            <div style="font-size: 14px; color: #666; margin-bottom: 5px;">神仙草</div>
                            <div style="font-size: 12px; color: #999;">恢复100%HP和100%MP</div>
                            <div style="font-size: 24px; font-weight: bold; color: #f1c40f;">${healingItems.IMMORTAL_GRASS || 0}</div>
                        </div>
                        ${renderBackpackItemCard('SMALL_EXP_FRUIT', '🍊', '小经验果', '直接提升1级', '#f39c12', healingItems.SMALL_EXP_FRUIT || 0, true)}
                        ${renderBackpackItemCard('MEDIUM_EXP_FRUIT', '🍑', '中经验果', '直接提升3级', '#e67e22', healingItems.MEDIUM_EXP_FRUIT || 0, true)}
                        ${renderBackpackItemCard('LARGE_EXP_FRUIT', '🍍', '大经验果', '直接提升5级', '#d35400', healingItems.LARGE_EXP_FRUIT || 0, true)}
                    </div>
                    <div id="backpackItemActionArea" style="margin-top: 12px;"></div>
                `;
                content.innerHTML = html;
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }

        let selectedBackpackItemTargets = [];
        let currentBatchItemType = '';

        function renderBackpackItemCard(itemType, icon, name, desc, color, count, allowUse = false) {
            const useButton = allowUse && count > 0
                ? `<button class="restart-btn" onclick="showBackpackItemTargets('${itemType}', '${name}')" style="margin-top: 10px; padding: 6px 14px; font-size: 12px; background: linear-gradient(135deg, ${color}, ${adjustColor(color, -20)});">选择精灵使用</button>`
                : '';
            return `
                <div style="background: linear-gradient(135deg, #fffaf0, #fdf2d6); border-radius: 12px; padding: 15px; text-align: center; border: 1px solid ${color};">
                    <div style="font-size: 30px; margin-bottom: 5px;">${icon}</div>
                    <div style="font-size: 14px; color: #666; margin-bottom: 5px;">${name}</div>
                    <div style="font-size: 12px; color: #999;">${desc}</div>
                    <div style="font-size: 24px; font-weight: bold; color: ${color};">${count}</div>
                    ${useButton}
                </div>`;
        }

        function toggleBackpackItemTarget(backpackId) {
            const index = selectedBackpackItemTargets.indexOf(backpackId);
            const card = document.getElementById(`backpack-item-target-${backpackId}`);
            if (index >= 0) {
                selectedBackpackItemTargets.splice(index, 1);
                if (card) {
                    card.style.borderColor = '#eee';
                    card.style.boxShadow = 'none';
                }
            } else {
                selectedBackpackItemTargets.push(backpackId);
                if (card) {
                    card.style.borderColor = '#f39c12';
                    card.style.boxShadow = '0 0 0 2px rgba(243, 156, 18, 0.18)';
                }
            }
            const countEl = document.getElementById('backpackBatchCount');
            if (countEl) {
                countEl.textContent = selectedBackpackItemTargets.length;
            }
        }

        async function showBackpackItemTargets(itemType, itemName) {
            const actionArea = document.getElementById('backpackItemActionArea');
            if (!actionArea) return;

            currentBatchItemType = itemType;
            selectedBackpackItemTargets = [];
            const isExpFruit = itemType.includes('EXP_FRUIT');
            actionArea.innerHTML = '<p style="text-align:center; color:#666;">加载可使用的精灵中...</p>';
            try {
                const response = await fetch('/game/backpack');
                const pokemons = await response.json();
                if (!Array.isArray(pokemons) || pokemons.length === 0) {
                    actionArea.innerHTML = '<p style="text-align:center; color:#e74c3c;">背包中没有可使用的精灵</p>';
                    return;
                }

                let html = `<div style="padding: 12px; background: white; border-radius: 12px; border: 1px solid #f0c36d;">`;
                html += `<div style="font-size: 14px; font-weight: bold; color: #8a6d3b; margin-bottom: 10px; text-align: center;">为 ${itemName} 选择目标精灵</div>`;
                if (isExpFruit) {
                    html += `<div style="display:flex; flex-direction:column; gap:10px; margin-bottom: 10px; font-size: 12px; color:#666;"><span>\u53ef\u5bf9\u5355\u53ea\u7cbe\u7075\u4e00\u6b21\u4f7f\u7528\u591a\u4e2a\u7ecf\u9a8c\u679c</span><div style="display:flex; gap:10px; align-items:center; justify-content:center; flex-wrap:wrap;"><span>\u6570\u91cf</span><input id="backpackBatchQuantity" type="number" min="1" value="1" style="width:90px; padding:6px 8px; border:1px solid #ddd; border-radius:8px;"><button class="restart-btn" onclick="useBackpackItemBatch()" style="margin-top:0; padding: 8px 14px; font-size:12px; background: linear-gradient(135deg, #f39c12, #e67e22);">\u5bf9\u6240\u9009\u7cbe\u7075\u6279\u91cf\u4f7f\u7528</button></div><div style="text-align:center;">\u5f53\u524d\u9009\u62e9: <strong id="backpackBatchCount">0</strong> \u53ea\u7cbe\u7075</div></div>`;
                }
                html += '<div style="display:grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px;">';
                pokemons.forEach(pokemon => {
                    const actionBtn = isExpFruit
                        ? `<button class="restart-btn" onclick="event.stopPropagation(); useBackpackItem('${itemType}', ${pokemon.id})" style="padding: 6px 14px; font-size: 12px;">单独使用</button>`
                        : `<button class="restart-btn" onclick="useBackpackItem('${itemType}', ${pokemon.id})" style="padding: 6px 14px; font-size: 12px;">使用</button>`;
                    const clickAttr = isExpFruit ? `onclick="toggleBackpackItemTarget(${pokemon.id})"` : '';
                    const cursor = isExpFruit ? 'cursor:pointer;' : '';
                    html += `
                        <div id="backpack-item-target-${pokemon.id}" ${clickAttr} style="border: 1px solid #eee; border-radius: 10px; padding: 10px; text-align: center; background: #fafafa; ${cursor}">
                            <div style="font-weight: bold; color: ${getTypeColor(pokemon.pokemonType)}; margin-bottom: 4px;">${pokemon.pokemonName}</div>
                            <div style="font-size: 12px; color: #666; margin-bottom: 4px;">Lv.${pokemon.level} | ${getTypeName(pokemon.pokemonType)}</div>
                            <div style="font-size: 11px; color: #888; margin-bottom: 8px;">HP ${pokemon.currentHp}/${pokemon.maxHp} | MP ${pokemon.currentMp}/${pokemon.maxMp}</div>
                            ${actionBtn}
                        </div>`;
                });
                html += '</div></div>';
                actionArea.innerHTML = html;
            } catch (error) {
                actionArea.innerHTML = '<p style="text-align:center; color:#e74c3c;">加载目标精灵失败</p>';
            }
        }

        async function useBackpackItem(itemType, backpackId) {
            try {
                const response = await fetch(`/game/backpack/use-item?itemType=${itemType}&backpackId=${backpackId}`, {
                    method: 'POST'
                });
                const data = await response.json();
                if (data.success) {
                    alert(data.message);
                    await showBackpackItemsModal();
                    await loadPokemons();
                    updateInventoryStatus();
                } else {
                    alert(data.message);
                }
            } catch (error) {
                alert('使用道具失败，请重试');
            }
        }

        async function useBackpackItemBatch() {
            if (!currentBatchItemType) {
                alert('请先选择经验果');
                return;
            }
            if (selectedBackpackItemTargets.length === 0) {
                alert('请先选择要批量使用的精灵');
                return;
            }
            try {
                if (selectedBackpackItemTargets.length !== 1) {
                    alert('\u6279\u91cf\u7ecf\u9a8c\u679c\u4e00\u6b21\u53ea\u80fd\u9009\u62e9\u4e00\u53ea\u7cbe\u7075');
                    return;
                }
                const quantityInput = document.getElementById('backpackBatchQuantity');
                const quantity = Math.max(1, parseInt(quantityInput?.value || '1', 10) || 1);
                const response = await fetch('/game/backpack/use-item-batch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ itemType: currentBatchItemType, backpackId: selectedBackpackItemTargets[0], quantity })
                });
                const data = await response.json();
                if (data.success) {
                    alert(`${data.message}\n总共提升 ${data.totalLevelGain || 0} 级`);
                    await showBackpackItemsModal();
                    await loadPokemons();
                    updateInventoryStatus();
                } else {
                    alert(data.message);
                }
            } catch (error) {
                alert('批量使用失败，请重试');
            }
        }
        
        function closeBackpackItemsModal() {
            document.getElementById('backpackItemsModal').style.display = 'none';
        }
        
        async function showPokedex() {
            const modal = document.getElementById('pokedexModal');
            const content = document.getElementById('pokedexContent');
            modal.style.display = 'flex';
            
            // 总精灵种类数（包含战神驼在内共17种）
            const TOTAL_POKEMON_COUNT = 20;
            
            try {
                const response = await fetch('/game/pokedex');
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const entries = await response.json();
                
                if (!Array.isArray(entries)) {
                    throw new Error('返回数据格式错误');
                }
                
                if (entries.length === 0) {
                    content.innerHTML = '<p style="text-align: center; color: #7f8c8d;">图鉴是空的，快去遇到精灵吧！</p>';
                } else {
                    let html = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px;">';
                    entries.forEach(entry => {
                        const typeColor = getTypeColor(entry.pokemonType);
                        const caughtMark = entry.caught ? '✅' : '❓';
                        const imagePath = `/images/pokemon/${entry.pokemonName}.png`;
                        // 战神驼特殊标识（已获得时才显示）
                        const isGodCamel = entry.pokemonName === '战神驼';
                        const specialBadge = (isGodCamel && entry.caught) ? '<span style="background: linear-gradient(135deg, #fd79a8, #e84393); color: white; padding: 2px 8px; border-radius: 10px; font-size: 10px; margin-left: 5px;">特等奖</span>' : '';
                        
                        if (entry.caught) {
                            // 已获得：显示完整信息
                            const lotteryNum = entry.lotteryCount || 0;
                            const rarity = entry.rarity || 1;
                            html += `
                            <div style="border: 1px solid #ddd; border-radius: 12px; padding: 15px; background: #e8f5e9; text-align: center;">
                                <div style="width: 80px; height: 80px; margin: 0 auto 10px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 8px;">
                                    <img src="${imagePath}" alt="${entry.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; margin-bottom: 3px;">
                                    ${caughtMark} ${entry.pokemonName}${specialBadge}
                                </div>
                                <div style="font-size: 11px; margin-bottom: 5px;">${getRarityStars(rarity)}</div>
                                <div style="font-size: 12px; color: #666; margin-bottom: 5px;">${getTypeName(entry.pokemonType)}</div>
                                <div style="font-size: 10px; color: #999; line-height: 1.6;">
                                    <div>遇到: ${entry.encounterCount} | 捕获: ${entry.catchCount} | 抽奖: ${lotteryNum}</div>
                                </div>
                            </div>`;
                        } else {
                            // 未获得：只显示问号和属性，隐藏图片和详细数据
                            html += `
                            <div style="border: 1px solid #ddd; border-radius: 12px; padding: 15px; background: #f9f9f9; text-align: center; opacity: 0.8;">
                                <div style="width: 80px; height: 80px; margin: 0 auto 10px; background: linear-gradient(145deg, #d5d5d5, #b0b0b0); border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                                    <span style="font-size: 40px; color: #888;">?</span>
                                </div>
                                <div style="font-weight: bold; color: #888; margin-bottom: 5px;">❓ ???</div>
                                <div style="font-size: 12px; color: #aaa; margin-bottom: 5px;">${getTypeName(entry.pokemonType)}</div>
                                <div style="font-size: 11px; color: #bbb;">未发现</div>
                            </div>`;
                        }
                    });
                    html += '</div>';
                    const caughtCount = entries.filter(e => e.caught).length;
                    html += `<p style="text-align: center; margin-top: 20px; padding: 15px; background: linear-gradient(135deg, #f8f9fa, #e9ecef); border-radius: 10px; color: #495057; font-weight: bold;">
                        图鉴进度: 已捕获 ${caughtCount} / 总共 ${TOTAL_POKEMON_COUNT} 种精灵
                    </p>`;
                    content.innerHTML = html;
                }
            } catch (error) {
                console.error('图鉴加载失败:', error);
                content.innerHTML = `<p style="color: #e74c3c; text-align: center;">加载失败: ${error.message}</p>`;
            }
        }
        
        function closePokedex() {
            document.getElementById('pokedexModal').style.display = 'none';
        }
        
        function showTypeChart() {
            document.getElementById('typeChartModal').style.display = 'flex';
        }
        
        function closeTypeChart() {
            document.getElementById('typeChartModal').style.display = 'none';
        }
        
        function getTypeColor(type) {
            const colors = { 'FIRE': '#e74c3c', 'WATER': '#3498db', 'GRASS': '#27ae60', 'NORMAL': '#95a5a6' };
            return colors[type] || '#7f8c8d';
        }
        
        async function catchPokemon() {
            // 训练师对战中不能使用捕获功能
            const isTrainerBattle = document.getElementById('battleScreen').getAttribute('data-trainer-battle') === 'true';
            if (isTrainerBattle) {
                alert('训练师对战中不能使用捕获功能！');
                return;
            }
            
            // 副本挑战中不能使用捕获功能
            const isDungeonBattle = document.getElementById('battleScreen').getAttribute('data-dungeon-battle') === 'true';
            if (isDungeonBattle) {
                alert('副本挑战中不能使用捕获功能！');
                return;
            }
            
            try {
                const response = await fetch(`/game/catch?ballType=${selectedPokeBall}`, { method: 'POST' });
                const data = await response.json();
                
                if (!data.success && data.message && data.message.includes('数量不足')) {
                    alert(data.message);
                    return;
                }
                
                // 刷新精灵球数量
                await refreshBattleBallCounts();
                
                if (data.battleLog) {
                    const logDiv = document.getElementById('battleLog');
                    logDiv.innerHTML = '';
                    data.battleLog.forEach(log => {
                        const div = document.createElement('div');
                        div.textContent = log;
                        logDiv.appendChild(div);
                    });
                    logDiv.scrollTop = logDiv.scrollHeight;
                }
                
                // 更新HP和MP显示
                if (data.playerHp !== undefined) {
                    document.getElementById('playerHpText').textContent = `HP: ${data.playerHp}/${data.playerMaxHp}`;
                    document.getElementById('playerHpBar').style.width = `${(data.playerHp / data.playerMaxHp) * 100}%`;
                }
                if (data.playerMp !== undefined) {
                    document.getElementById('playerMpText').textContent = `MP: ${data.playerMp}/${data.playerMaxMp}`;
                    document.getElementById('playerMpBar').style.width = `${(data.playerMp / data.playerMaxMp) * 100}%`;
                }
                if (data.enemyHp !== undefined) {
                    document.getElementById('enemyHpText').textContent = `HP: ${data.enemyHp}/${data.enemyMaxHp}`;
                    document.getElementById('enemyHpBar').style.width = `${(data.enemyHp / data.enemyMaxHp) * 100}%`;
                }
                if (data.enemyMp !== undefined) {
                    document.getElementById('enemyMpText').textContent = `MP: ${data.enemyMp}/${data.enemyMaxMp}`;
                    document.getElementById('enemyMpBar').style.width = `${(data.enemyMp / data.enemyMaxMp) * 100}%`;
                }
                
                // 更新技能按钮状态（MP可能变化）
                if (data.playerMoves) {
                    renderMoves(data.playerMoves, data.playerMp, data.playerMp);
                }
                updateBattleRates();
                
                if (data.success) {
                    let successMessage = data.message || '捕获成功！';
                    if (data.placement === 'storage') {
                        successMessage = '捕获成功，精灵已自动存入仓库！';
                    } else if (data.placement === 'sold') {
                        successMessage = '捕获成功，但背包和仓库已满，精灵已自动折算为补偿金币！';
                    } else if (data.placement === 'backpack') {
                        successMessage = '捕获成功，精灵已放入背包！';
                    }
                    setTimeout(() => {
                        const modal = document.getElementById('resultModal');
                        document.getElementById('resultTitle').textContent = '🎉 捕获成功！';
                        document.getElementById('resultTitle').className = 'win';
                        document.getElementById('resultMessage').textContent = successMessage;
                        modal.style.display = 'flex';
                    }, 500);
                } else if (data.battleOver) {
                    // 战斗结束（所有精灵战败）
                    setTimeout(() => {
                        const modal = document.getElementById('resultModal');
                        document.getElementById('resultTitle').textContent = '💀 战斗失败';
                        document.getElementById('resultTitle').className = 'lose';
                        document.getElementById('resultMessage').textContent = '你所有的精灵都已失去战斗能力！';
                        modal.style.display = 'flex';
                    }, 500);
                } else if (data.forceSwitch) {
                    // 先检查是否有可更换的精灵
                    const checkResponse = await fetch('/game/battle/switchable');
                    const switchablePokemons = await checkResponse.json();
                    
                    if (!switchablePokemons || switchablePokemons.length === 0) {
                        // 没有可更换精灵，显示战斗失败
                        setTimeout(() => {
                            const modal = document.getElementById('resultModal');
                            document.getElementById('resultTitle').textContent = '💀 战斗失败';
                            document.getElementById('resultTitle').className = 'lose';
                            document.getElementById('resultMessage').textContent = '你所有的精灵都已失去战斗能力！';
                            modal.style.display = 'flex';
                        }, 500);
                    } else {
                        setTimeout(() => {
                            alert('当前精灵已失去战斗能力，请选择下一只出战精灵！');
                            showSwitchPokemonModal(true);
                        }, 500);
                    }
                }
            } catch (error) {
                console.error('捕获失败:', error);
            }
        }
        
        async function tryEscape() {
            // 训练师对战中不能使用逃跑功能
            const isTrainerBattle = document.getElementById('battleScreen').getAttribute('data-trainer-battle') === 'true';
            if (isTrainerBattle) {
                alert('训练师对战中不能使用逃跑功能！');
                return;
            }
            
            // 副本挑战中不能使用逃跑功能
            const isDungeonBattle = document.getElementById('battleScreen').getAttribute('data-dungeon-battle') === 'true';
            if (isDungeonBattle) {
                alert('副本挑战中不能逃跑！');
                return;
            }
            
            try {
                const response = await fetch('/game/escape', { method: 'POST' });
                const data = await response.json();
                
                if (data.battleLog) {
                    const logDiv = document.getElementById('battleLog');
                    logDiv.innerHTML = '';
                    data.battleLog.forEach(log => {
                        const div = document.createElement('div');
                        div.textContent = log;
                        logDiv.appendChild(div);
                    });
                    logDiv.scrollTop = logDiv.scrollHeight;
                }
                
                // 更新HP和MP显示
                if (data.playerHp !== undefined) {
                    document.getElementById('playerHpText').textContent = `HP: ${data.playerHp}/${data.playerMaxHp}`;
                    document.getElementById('playerHpBar').style.width = `${(data.playerHp / data.playerMaxHp) * 100}%`;
                }
                if (data.playerMp !== undefined) {
                    document.getElementById('playerMpText').textContent = `MP: ${data.playerMp}/${data.playerMaxMp}`;
                    document.getElementById('playerMpBar').style.width = `${(data.playerMp / data.playerMaxMp) * 100}%`;
                }
                if (data.enemyHp !== undefined) {
                    document.getElementById('enemyHpText').textContent = `HP: ${data.enemyHp}/${data.enemyMaxHp}`;
                    document.getElementById('enemyHpBar').style.width = `${(data.enemyHp / data.enemyMaxHp) * 100}%`;
                }
                if (data.enemyMp !== undefined) {
                    document.getElementById('enemyMpText').textContent = `MP: ${data.enemyMp}/${data.enemyMaxMp}`;
                    document.getElementById('enemyMpBar').style.width = `${(data.enemyMp / data.enemyMaxMp) * 100}%`;
                }
                
                // 更新技能按钮状态（MP可能变化）
                if (data.playerMoves) {
                    renderMoves(data.playerMoves, data.playerMp, data.playerMp);
                }
                updateBattleRates();
                
                if (data.success) {
                    setTimeout(() => {
                        const modal = document.getElementById('resultModal');
                        document.getElementById('resultTitle').textContent = '🏃 逃跑成功！';
                        document.getElementById('resultTitle').className = 'win';
                        document.getElementById('resultMessage').textContent = '你成功逃离了战斗！';
                        modal.style.display = 'flex';
                    }, 500);
                } else if (data.battleOver) {
                    // 战斗结束（所有精灵战败）
                    setTimeout(() => {
                        const modal = document.getElementById('resultModal');
                        document.getElementById('resultTitle').textContent = '💀 战斗失败';
                        document.getElementById('resultTitle').className = 'lose';
                        document.getElementById('resultMessage').textContent = '你所有的精灵都已失去战斗能力！';
                        modal.style.display = 'flex';
                    }, 500);
                } else if (data.forceSwitch) {
                    // 先检查是否有可更换的精灵
                    const checkResponse = await fetch('/game/battle/switchable');
                    const switchablePokemons = await checkResponse.json();
                    
                    if (!switchablePokemons || switchablePokemons.length === 0) {
                        // 没有可更换精灵，显示战斗失败
                        setTimeout(() => {
                            const modal = document.getElementById('resultModal');
                            document.getElementById('resultTitle').textContent = '💀 战斗失败';
                            document.getElementById('resultTitle').className = 'lose';
                            document.getElementById('resultMessage').textContent = '你所有的精灵都已失去战斗能力！';
                            modal.style.display = 'flex';
                        }, 500);
                    } else {
                        setTimeout(() => {
                            alert('当前精灵已失去战斗能力，请选择下一只出战精灵！');
                            showSwitchPokemonModal(true);
                        }, 500);
                    }
                }
            } catch (error) {
                console.error('逃跑失败:', error);
            }
        }
        
        async function showSwitchPokemonModal(isForceSwitch = false) {
            const modal = document.getElementById('switchPokemonModal');
            const content = document.getElementById('switchPokemonContent');
            // 设置强制更换标记
            modal.setAttribute('data-force-switch', isForceSwitch ? 'true' : 'false');
            modal.style.display = 'flex';
            
            try {
                const response = await fetch('/game/battle/switchable');
                const pokemons = await response.json();
                
                if (pokemons.error || !Array.isArray(pokemons)) {
                    content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
                    return;
                }
                
                // 检查是否战斗已结束（所有精灵都战败）
                const isBattleOver = !document.getElementById('battleScreen').style.display || document.getElementById('battleScreen').style.display === 'none';
                
                if (pokemons.length === 0) {
                    const isTrainerBattle = document.getElementById('battleScreen').getAttribute('data-trainer-battle') === 'true';
                    // 检查是否处于强制更换状态（精灵战败）
                    const isForceSwitch = document.getElementById('switchPokemonModal').getAttribute('data-force-switch') === 'true';
                    
                    if (isForceSwitch) {
                        // 强制更换状态下没有可更换精灵 = 战斗失败
                        closeSwitchPokemonModal();
                        setTimeout(() => {
                            const modal = document.getElementById('resultModal');
                            document.getElementById('resultTitle').textContent = '💀 战斗失败';
                            document.getElementById('resultTitle').className = 'lose';
                            document.getElementById('resultMessage').textContent = '你所有的精灵都已失去战斗能力！';
                            modal.style.display = 'flex';
                        }, 300);
                    } else {
                        // 主动更换但没有其他精灵
                        const message = isTrainerBattle 
                            ? '<p style="text-align: center; color: #e74c3c; font-size: 16px; font-weight: bold;">⚠️ 当前没有可替换的精灵！</p><p style="text-align: center; color: #7f8c8d; margin-top: 10px;">你所有的精灵都已失去战斗能力...</p>'
                            : '<p style="text-align: center; color: #7f8c8d;">背包中没有其他可更换的精灵</p>';
                        content.innerHTML = message;
                    }
                } else {
                    let html = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">';
                    pokemons.forEach(pokemon => {
                        const typeColor = getTypeColor(pokemon.type);
                        const imagePath = `/images/pokemon/${pokemon.name}.png`;
                        html += `
                            <div style="border: 1px solid #ddd; border-radius: 12px; padding: 15px; background: #f9f9f9; text-align: center;">
                                <div style="width: 70px; height: 70px; margin: 0 auto 10px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 8px;">
                                    <img src="${imagePath}" alt="${pokemon.name}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; font-size: 16px; margin-bottom: 3px;">${pokemon.name}</div>
                                <div style="font-size: 11px; margin-bottom: 5px;">${getRarityStars(pokemon.rarity)}</div>
                                <div style="font-size: 12px; color: #666; margin-bottom: 5px;">属性: ${getTypeName(pokemon.type)} | 等级: Lv.${pokemon.level}</div>
                                <div style="font-size: 11px; color: #999; margin-bottom: 8px;">生命值: ${pokemon.currentHp}/${pokemon.maxHp} | 法力值: ${pokemon.currentMp}/${pokemon.maxMp}</div>
                                <button class="restart-btn" onclick="switchPokemon(${pokemon.backpackId})" style="padding: 8px 20px; font-size: 14px;">更换</button>
                            </div>`;
                    });
                    html += '</div>';
                    content.innerHTML = html;
                }
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }
        
        function closeSwitchPokemonModal() {
            document.getElementById('switchPokemonModal').style.display = 'none';
        }
        
        async function switchPokemon(backpackId) {
            try {
                const response = await fetch('/game/battle/switch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `backpackId=${backpackId}`
                });
                const data = await response.json();
                
                if (data.success) {
                    closeSwitchPokemonModal();
                    updateBattleDisplay(data);
                    if (data.playerMoves) renderMoves(data.playerMoves, data.playerMp, data.playerMp);
                    updateBattleRates();
                    
                    // 如果是副本挑战，更新副本挑战信息
                    if (data.isDungeonBattle && data.enemyRemaining !== undefined) {
                        showDungeonBattleInfo(data.enemyRemaining, data.enemyDefeated);
                    }
                }
            } catch (error) {
                console.error('更换精灵失败:', error);
            }
        }
        
        // ========== 战斗中使用道具功能 ==========
        
        /**
         * 显示战斗中使用道具弹窗
         */
        async function showBattleItemModal() {
            const modal = document.getElementById('battleItemModal');
            const content = document.getElementById('battleItemContent');
            modal.style.display = 'flex';
            
            try {
                // 获取恢复道具库存
                const response = await fetch('/game/inventory');
                const data = await response.json();
                
                if (!data.healingItems) {
                    content.innerHTML = '<p style="text-align: center; color: #e74c3c;">加载道具失败</p>';
                    return;
                }
                
                const healingItems = data.healingItems;
                const itemConfigs = {
                    'LIFE_FRUIT': { name: '生命果', icon: '🍎', desc: '恢复50%HP', color: '#e74c3c' },
                    'BIG_LIFE_FRUIT': { name: '大生命果', icon: '🍏', desc: '恢复100%HP', color: '#27ae60' },
                    'ENERGY_STONE': { name: '能量石', icon: '💎', desc: '恢复50%MP', color: '#3498db' },
                    'BIG_ENERGY_STONE': { name: '大能量石', icon: '💠', desc: '恢复100%MP', color: '#9b59b6' },
                    'ESSENCE_GRASS': { name: '精华草', icon: '🌿', desc: '恢复50%HP和50%MP', color: '#00b894' },
                    'IMMORTAL_GRASS': { name: '神仙草', icon: '🌱', desc: '恢复100%HP和100%MP', color: '#f1c40f' },

                };
                
                let html = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 12px;">';
                
                let hasItems = false;
                for (const [itemType, count] of Object.entries(healingItems)) {
                    if (count > 0 && itemConfigs[itemType]) {
                        hasItems = true;
                        const config = itemConfigs[itemType];
                        html += `
                            <div style="border: 2px solid ${config.color}; border-radius: 12px; padding: 15px; background: linear-gradient(135deg, #f8f9fa, #e9ecef); text-align: center;">
                                <div style="font-size: 40px; margin-bottom: 8px;">${config.icon}</div>
                                <div style="font-weight: bold; font-size: 16px; color: ${config.color}; margin-bottom: 5px;">${config.name}</div>
                                <div style="font-size: 12px; color: #666; margin-bottom: 8px;">${config.desc}</div>
                                <div style="font-size: 14px; color: #333; margin-bottom: 10px;">持有: <strong>${count}</strong>个</div>
                                <button class="restart-btn" onclick="useItemInBattle('${itemType}')" style="padding: 8px 20px; font-size: 14px; background: linear-gradient(135deg, ${config.color}, ${adjustColor(config.color, -20)});">使用</button>
                            </div>`;
                    }
                }
                
                html += '</div>';
                
                if (!hasItems) {
                    html = '<p style="text-align: center; color: #7f8c8d; padding: 40px;">你没有可在战斗中使用的恢复道具，请前往商店购买</p>';
                }
                
                content.innerHTML = html;
            } catch (error) {
                content.innerHTML = '<p style="text-align: center; color: #e74c3c;">加载失败</p>';
            }
        }
        
        /**
         * 辅助函数：调整颜色亮度
         */
        function adjustColor(color, amount) {
            const hex = color.replace('#', '');
            const r = Math.max(0, Math.min(255, parseInt(hex.substr(0, 2), 16) + amount));
            const g = Math.max(0, Math.min(255, parseInt(hex.substr(2, 2), 16) + amount));
            const b = Math.max(0, Math.min(255, parseInt(hex.substr(4, 2), 16) + amount));
            return `rgb(${r}, ${g}, ${b})`;
        }
        
        /**
         * 关闭战斗中使用道具弹窗
         */
        function closeBattleItemModal() {
            document.getElementById('battleItemModal').style.display = 'none';
        }
        
        /**
         * 在战斗中使用道具
         */
        async function useItemInBattle(itemType) {
            try {
                const response = await fetch(`/game/battle/use-item?itemType=${itemType}`, {
                    method: 'POST'
                });
                const data = await response.json();
                
                if (data.success) {
                    closeBattleItemModal();
                    
                    // 更新战斗显示
                    if (data.isCurrentPokemon) {
                        document.getElementById('playerHpText').textContent = `HP: ${data.newHp}/${data.maxHp}`;
                        document.getElementById('playerHpBar').style.width = `${(data.newHp / data.maxHp) * 100}%`;
                        document.getElementById('playerMpText').textContent = `MP: ${data.newMp}/${data.maxMp}`;
                        document.getElementById('playerMpBar').style.width = `${(data.newMp / data.maxMp) * 100}%`;
                        
                        if (data.battleLog) {
                            const battleLogEl = document.getElementById('battleLog');
                            battleLogEl.innerHTML = data.battleLog.map(log => `<p>${log}</p>`).join('');
                            battleLogEl.scrollTop = battleLogEl.scrollHeight;
                        }
                        
                        if (battleData.playerPokemon) {
                            battleData.playerPokemon.currentHp = data.newHp;
                            battleData.playerPokemon.currentMp = data.newMp;
                        }
                        battleData.playerMp = data.newMp;
                        battleData.playerMaxMp = data.maxMp;
                        renderMoves(battleData.playerMoves || [], data.newMp, data.newMp);
                        updateBattleRates();
                    }
                    
                    alert(data.message);
                } else {
                    alert(data.message);
                }
            } catch (error) {
                alert('使用道具失败，请重试');
            }
        }
        
        /**
         * 根据当前MP刷新技能按钮状态
         */
        function refreshMovesByCurrentMp(currentMp, maxMp) {
            const container = document.getElementById('movesContainer');
            const buttons = container.querySelectorAll('.move-btn');
            
            buttons.forEach(btn => {
                // 从按钮文本中提取MP消耗
                const mpMatch = btn.innerHTML.match(/MP:(\d+)/);
                if (mpMatch) {
                    const mpCost = parseInt(mpMatch[1]);
                    const canUse = currentMp >= mpCost;
                    btn.disabled = !canUse;
                    
                    // 更新按钮样式
                    if (canUse) {
                        btn.style.opacity = '1';
                        btn.style.cursor = 'pointer';
                    } else {
                        btn.style.opacity = '0.5';
                        btn.style.cursor = 'not-allowed';
                    }
                }
            });
        }
        
        async function updateInventoryStatus() {
            try {
                const response = await fetch('/game/inventory/status');
                const data = await response.json();
                document.getElementById('backpackCount').textContent = data.backpackCount;
                document.getElementById('storageCount').textContent = data.storageCount;
                document.getElementById('goldCount').textContent = data.gold !== undefined ? data.gold : 0;
                checkTrainerBattleEligibility();
            } catch (error) {
                console.error('获取容量信息失败:', error);
            }
        }
        
        // 存储选中的精灵ID
        let selectedBackpackIds = [];
        let selectedStorageIds = [];
        
        async function showStorage() {
            const modal = document.getElementById('storageModal');
            const content = document.getElementById('storageContent');
            const actions = document.getElementById('storageActions');
            modal.style.display = 'flex';
            selectedBackpackIds = [];
            selectedStorageIds = [];
            
            try {
                const [backpackRes, storageRes, statusRes] = await Promise.all([
                    fetch('/game/backpack'),
                    fetch('/game/storage'),
                    fetch('/game/inventory/status')
                ]);
                
                const backpack = await backpackRes.json();
                const storageData = await storageRes.json();
                const status = await statusRes.json();
                
                // 显示操作按钮
                actions.style.display = 'block';
                
                // 仓库扩充按钮（如果未达上限）
                const canExpand = status.storageMax < 100;
                const expandBtn = canExpand 
                    ? `<button onclick="expandStorage()" style="margin-left: 15px; padding: 5px 12px; border: none; border-radius: 15px; background: linear-gradient(135deg, #f1c40f, #f39c12); color: white; cursor: pointer; font-size: 12px;">扩充仓库 (1000金币+10格)</button>`
                    : `<span style="margin-left: 15px; color: #27ae60; font-size: 12px;">✅ 仓库已达上限</span>`;
                
                let html = `<div style="background: #f0f0f0; padding: 10px; border-radius: 8px; margin-bottom: 20px; text-align: center;">
                    <span style="margin-right: 20px;">🎒 背包: ${status.backpackCount}/${status.backpackMax}</span>
                    <span>🏠 仓库: ${status.storageCount}/${status.storageMax}</span>
                    ${expandBtn}
                </div>`;
                
                html += '<h3 style="color: #667eea; margin-bottom: 10px;">🎒 背包中的精灵（点击选择）</h3>';
                if (backpack.length === 0) {
                    html += '<p style="color: #7f8c8d; margin-bottom: 20px;">背包是空的</p>';
                } else {
                    html += '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; margin-bottom: 20px;">';
                    backpack.forEach(item => {
                        const typeColor = getTypeColor(item.pokemonType);
                        const canDeposit = backpack.length > 1;
                        const imagePath = `/images/pokemon/${item.pokemonName}.png`;
                        html += `
                            <div id="backpack-item-${item.id}" onclick="toggleBackpackSelection(${item.id}, ${canDeposit})" style="border: 2px solid #ddd; border-radius: 12px; padding: 15px; background: #f9f9f9; text-align: center; cursor: pointer; transition: all 0.3s;">
                                <div style="width: 60px; height: 60px; margin: 0 auto 10px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 6px;">
                                    <img src="${imagePath}" alt="${item.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; margin-bottom: 3px;">${item.pokemonName}</div>
                                <div style="font-size: 10px; margin-bottom: 5px;">${getRarityStars(item.rarity)}</div>
                                <div style="font-size: 11px; color: #666; margin-bottom: 8px;">等级: Lv.${item.level} | 属性: ${getTypeName(item.pokemonType)}</div>
                                ${!canDeposit ? '<div style="font-size: 10px; color: #e74c3c;">最后一个不能移走</div>' : ''}
                            </div>`;
                    });
                    html += '</div>';
                }
                
                html += '<h3 style="color: #667eea; margin-bottom: 10px;">🏠 仓库中的精灵（点击选择）</h3>';
                if (storageData.storage.length === 0) {
                    html += '<p style="color: #7f8c8d;">仓库是空的</p>';
                } else {
                    html += '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px;">';
                    storageData.storage.forEach(item => {
                        const typeColor = getTypeColor(item.pokemonType);
                        const canWithdraw = status.backpackCount < status.backpackMax;
                        const imagePath = `/images/pokemon/${item.pokemonName}.png`;
                        html += `
                            <div id="storage-item-${item.id}" onclick="toggleStorageSelection(${item.id}, ${canWithdraw})" style="border: 2px solid #ddd; border-radius: 12px; padding: 15px; background: #e8f5e9; text-align: center; cursor: pointer; transition: all 0.3s;">
                                <div style="width: 60px; height: 60px; margin: 0 auto 10px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 6px;">
                                    <img src="${imagePath}" alt="${item.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; margin-bottom: 3px;">${item.pokemonName}</div>
                                <div style="font-size: 10px; margin-bottom: 5px;">${getRarityStars(item.rarity)}</div>
                                <div style="font-size: 11px; color: #666; margin-bottom: 8px;">等级: Lv.${item.level} | 属性: ${getTypeName(item.pokemonType)}</div>
                                ${!canWithdraw ? '<div style="font-size: 10px; color: #e74c3c;">背包已满</div>' : ''}
                            </div>`;
                    });
                    html += '</div>';
                }
                
                content.innerHTML = html;
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }
        
        function toggleBackpackSelection(backpackId, canDeposit) {
            if (!canDeposit) return;
            
            const index = selectedBackpackIds.indexOf(backpackId);
            const element = document.getElementById(`backpack-item-${backpackId}`);
            
            if (index > -1) {
                selectedBackpackIds.splice(index, 1);
                element.style.border = '2px solid #ddd';
                element.style.background = '#f9f9f9';
            } else {
                selectedBackpackIds.push(backpackId);
                element.style.border = '3px solid #e74c3c';
                element.style.background = '#ffeaea';
            }
        }
        
        function toggleStorageSelection(storageId, canWithdraw) {
            if (!canWithdraw) return;
            
            const index = selectedStorageIds.indexOf(storageId);
            const element = document.getElementById(`storage-item-${storageId}`);
            
            if (index > -1) {
                selectedStorageIds.splice(index, 1);
                element.style.border = '2px solid #ddd';
                element.style.background = '#e8f5e9';
            } else {
                selectedStorageIds.push(storageId);
                element.style.border = '3px solid #27ae60';
                element.style.background = '#d4edda';
            }
        }
        
        async function executeBatchDeposit() {
            if (selectedBackpackIds.length === 0) {
                alert('请先选择要移入仓库的精灵！');
                return;
            }
            
            try {
                const response = await fetch('/game/storage/deposit-batch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pokemonIds: selectedBackpackIds })
                });
                const data = await response.json();
                
                alert(data.message);
                if (data.success) {
                    showStorage();
                    loadPokemons();
                    updateInventoryStatus();
                    checkTrainerBattleEligibility();
                }
            } catch (error) {
                console.error('批量移入仓库失败:', error);
            }
        }
        
        async function executeBatchWithdraw() {
            if (selectedStorageIds.length === 0) {
                alert('请先选择要移入背包的精灵！');
                return;
            }
            
            try {
                const response = await fetch('/game/storage/withdraw-batch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pokemonIds: selectedStorageIds })
                });
                const data = await response.json();
                
                alert(data.message);
                if (data.success) {
                    showStorage();
                    loadPokemons();
                    updateInventoryStatus();
                    checkTrainerBattleEligibility();
                }
            } catch (error) {
                console.error('批量移入背包失败:', error);
            }
        }
        
        function closeStorageModal() {
            document.getElementById('storageModal').style.display = 'none';
        }

        /**
         * 扩充仓库容量
         */
        async function expandStorage() {
            if (!confirm('确定花费1000金币扩充仓库吗？将增加10个格子。')) {
                return;
            }
            try {
                const response = await fetch('/game/storage/expand', { method: 'POST' });
                const data = await response.json();
                if (data.success) {
                    alert(data.message);
                    // 刷新仓库界面
                    showStorage();
                    updateInventoryStatus();
                } else {
                    alert(data.message);
                }
            } catch (error) {
                console.error('扩充仓库失败:', error);
                alert('扩充失败，请重试');
            }
        }
        
        async function depositToStorage(backpackId) {
            try {
                const response = await fetch('/game/storage/deposit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `backpackId=${backpackId}`
                });
                const data = await response.json();
                if (data.success) {
                    alert(data.message);
                    showStorage();
                    updateInventoryStatus();
                    loadPokemons();
                }
            } catch (error) {
                console.error('移入仓库失败:', error);
            }
        }
        
        async function withdrawFromStorage(storageId) {
            try {
                const response = await fetch('/game/storage/withdraw', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `storageId=${storageId}`
                });
                const data = await response.json();
                if (data.success) {
                    alert(data.message);
                    showStorage();
                    updateInventoryStatus();
                    loadPokemons();
                }
            } catch (error) {
                console.error('移入背包失败:', error);
            }
        }
        
        async function showHospital() {
            const modal = document.getElementById('hospitalModal');
            const content = document.getElementById('hospitalContent');
            modal.style.display = 'flex';
            await loadHospitalContent();
        }
        
        async function loadHospitalContent() {
            const content = document.getElementById('hospitalContent');
            try {
                const [backpackRes, goldRes] = await Promise.all([
                    fetch('/game/backpack/status'),
                    fetch('/game/gold')
                ]);
                const pokemons = await backpackRes.json();
                const goldData = await goldRes.json();
                const currentGold = goldData.gold || 0;
                
                if (pokemons.length === 0) {
                    content.innerHTML = '<p style="text-align: center; color: #7f8c8d;">背包中没有精灵</p>';
                } else {
                    // 显示金币和治疗费用信息
                    let html = `<div style="background: linear-gradient(135deg, #f39c12, #e67e22); color: white; padding: 15px; border-radius: 10px; margin-bottom: 15px; text-align: center;">
                        <div style="font-size: 18px; font-weight: bold;">💰 当前金币: ${currentGold}</div>
                        <div style="font-size: 12px; margin-top: 5px;">治疗费用: 50金币/只精灵</div>
                    </div>`;
                    html += '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">';
                    pokemons.forEach(pokemon => {
                        const typeColor = getTypeColor(pokemon.type);
                        const hpPercent = (pokemon.currentHp / pokemon.maxHp) * 100;
                        const mpPercent = (pokemon.currentMp / pokemon.maxMp) * 100;
                        let hpColor = '#27ae60';
                        if (hpPercent < 30) hpColor = '#e74c3c';
                        else if (hpPercent < 60) hpColor = '#f39c12';
                        const needsHeal = hpPercent < 100 || mpPercent < 100;
                        
                        html += `
                            <div style="border: 1px solid #ddd; border-radius: 12px; padding: 15px; background: ${needsHeal ? '#fff9e6' : '#f9f9f9'}; text-align: center; cursor: pointer;" onclick="togglePokemonSelection(${pokemon.id})" id="hospital-pokemon-${pokemon.id}">
                                <div style="display: flex; justify-content: center; align-items: center; margin-bottom: 10px;">
                                    <input type="checkbox" id="checkbox-${pokemon.id}" style="margin-right: 8px; cursor: pointer;" onclick="event.stopPropagation();" ${needsHeal ? '' : 'disabled'}>
                                    <span style="font-weight: bold; color: ${typeColor}; font-size: 14px;">${pokemon.name}</span>
                                </div>
                                <div style="font-size: 11px; color: #666; margin-bottom: 8px;">Lv.${pokemon.level} ${getTypeName(pokemon.type)}</div>
                                <div style="margin-bottom: 8px;">
                                    <div style="display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 3px;"><span>HP</span><span>${pokemon.currentHp}/${pokemon.maxHp}</span></div>
                                    <div style="width: 100%; height: 8px; background: #e0e0e0; border-radius: 4px; overflow: hidden;">
                                        <div style="width: ${hpPercent}%; height: 100%; background: ${hpColor}; transition: width 0.3s;"></div>
                                    </div>
                                </div>
                                <div>
                                    <div style="display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 3px;"><span>MP</span><span>${pokemon.currentMp}/${pokemon.maxMp}</span></div>
                                    <div style="width: 100%; height: 6px; background: #e0e0e0; border-radius: 3px; overflow: hidden;">
                                        <div style="width: ${mpPercent}%; height: 100%; background: #3498db; transition: width 0.3s;"></div>
                                    </div>
                                </div>
                                <div style="margin-top: 8px; font-size: 10px; color: ${needsHeal ? '#e74c3c' : '#27ae60'};">${needsHeal ? '⚠️ 需要治疗' : '✅ 状态良好'}</div>
                            </div>`;
                    });
                    html += '</div>';
                    content.innerHTML = html;
                }
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }
        
        function togglePokemonSelection(pokemonId) {
            const checkbox = document.getElementById(`checkbox-${pokemonId}`);
            if (checkbox && !checkbox.disabled) {
                checkbox.checked = !checkbox.checked;
                const card = document.getElementById(`hospital-pokemon-${pokemonId}`);
                if (checkbox.checked) {
                    card.style.borderColor = '#27ae60';
                    card.style.boxShadow = '0 0 10px rgba(39, 174, 96, 0.3)';
                } else {
                    card.style.borderColor = '#ddd';
                    card.style.boxShadow = 'none';
                }
            }
        }
        
        async function healSelectedPokemons() {
            const checkboxes = document.querySelectorAll('[id^="checkbox-"]:checked');
            if (checkboxes.length === 0) {
                alert('请先选择需要治疗的精灵！');
                return;
            }
            
            const selectedIds = Array.from(checkboxes).map(cb => cb.id.replace('checkbox-', ''));
            
            try {
                const response = await fetch('/game/hospital/heal-selected', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pokemonIds: selectedIds })
                });
                const data = await response.json();
                alert(data.message);
                if (data.success) {
                    loadHospitalContent();
                    updateInventoryStatus();
                }
            } catch (error) {
                console.error('治疗失败:', error);
            }
        }
        
        function closeHospitalModal() {
            document.getElementById('hospitalModal').style.display = 'none';
        }
        
        // 显示金币帮助弹窗
        async function toggleMysteryGoldModal(forceOpen = true) {
            const modal = document.getElementById('mysteryGoldModal');
            if (forceOpen === false) {
                modal.style.display = 'none';
                return;
            }
            modal.style.display = 'flex';
            await refreshMysteryGoldDisplay();
        }

        async function refreshMysteryGoldDisplay() {
            try {
                const response = await fetch('/game/gold');
                const data = await response.json();
                document.getElementById('mysteryGoldCurrent').textContent = data.gold ?? 0;
            } catch (error) {
                document.getElementById('mysteryGoldCurrent').textContent = '--';
            }
        }

        async function adjustMysteryGold(amount) {
            try {
                const response = await fetch(`/game/gold/adjust?amount=${encodeURIComponent(amount)}`, {
                    method: 'POST'
                });
                const data = await response.json();
                if (data.success) {
                    await refreshMysteryGoldDisplay();
                    updateInventoryStatus();
                } else {
                    alert(data.message || '金币调整失败');
                }
            } catch (error) {
                alert('金币调整失败，请重试');
            }
        }

        async function applyCustomMysteryGold() {
            const input = document.getElementById('mysteryGoldAmount');
            const amount = parseInt(input.value, 10);
            if (Number.isNaN(amount)) {
                alert('请输入有效数字');
                return;
            }
            await adjustMysteryGold(amount);
        }

        function showGoldHelp() {
            document.getElementById('goldHelpModal').style.display = 'flex';
        }
        
        // 关闭金币帮助弹窗
        function closeGoldHelp() {
            document.getElementById('goldHelpModal').style.display = 'none';
        }
        
        async function healAllPokemons() {
            try {
                const response = await fetch('/game/hospital/heal', { method: 'POST' });
                const data = await response.json();
                alert(data.message);
                if (data.success) {
                    loadHospitalContent();
                    updateInventoryStatus();
                }
            } catch (error) {
                console.error('治疗失败:', error);
            }
        }
        
        async function updateBattleRates() {
            try {
                const response = await fetch(`/game/battle/rates?ballType=${selectedPokeBall}`);
                const data = await response.json();
                document.getElementById('catchRate').textContent = data.catchRate.toFixed(1);
                document.getElementById('escapeRate').textContent = data.escapeRate.toFixed(1);
            } catch (error) {
                console.error('获取概率失败:', error);
            }
        }

        // 当前选中的精灵球类型
        let selectedPokeBall = 'BASIC';

        // ========== 商店系统 JS ==========

        /**
         * 打开商店弹窗
         */
        async function showShop() {
            document.getElementById('shopModal').style.display = 'flex';
            await refreshShopDisplay();
        }

        /**
         * 关闭商店弹窗
         */
        function closeShop() {
            document.getElementById('shopModal').style.display = 'none';
            updateInventoryStatus();
        }

        /**
         * 刷新商店显示
         */
        async function refreshShopDisplay() {
            try {
                const resp = await fetch('/game/inventory');
                const data = await resp.json();
                
                // 更新金币显示
                document.getElementById('shopGoldDisplay').textContent = data.gold;
                
                // 更新精灵球数量
                const pokeBalls = data.pokeBalls || {};
                document.getElementById('shopBasicCount').textContent = (pokeBalls.BASIC || 0) + '个';
                document.getElementById('shopMediumCount').textContent = (pokeBalls.MEDIUM || 0) + '个';
                document.getElementById('shopAdvancedCount').textContent = (pokeBalls.ADVANCED || 0) + '个';
                document.getElementById('shopMasterCount').textContent = (pokeBalls.MASTER || 0) + '个';
                
                // 更新恢复道具数量
                const healingItems = data.healingItems || {};
                const healingTypes = ['LIFE_FRUIT', 'BIG_LIFE_FRUIT', 'ENERGY_STONE', 'BIG_ENERGY_STONE', 'ESSENCE_GRASS', 'IMMORTAL_GRASS', 'SMALL_EXP_FRUIT', 'MEDIUM_EXP_FRUIT', 'LARGE_EXP_FRUIT'];
                healingTypes.forEach(type => {
                    const countEl = document.getElementById(`shopHealingCount-${type}`);
                    if (countEl) {
                        countEl.textContent = healingItems[type] || 0;
                    }
                });
            } catch (e) {
                console.error('刷新商店显示失败:', e);
            }
        }

        /**
         * 购买精灵球
         */
        async function buyPokeBall(ballType, quantity) {
            try {
                const resp = await fetch(`/game/shop/buy?ballType=${ballType}&quantity=${quantity}&category=POKEBALL`, {
                    method: 'POST'
                });
                const data = await resp.json();
                
                if (data.success) {
                    alert(data.message);
                    await refreshShopDisplay();
                    updateInventoryStatus();
                } else {
                    alert(data.message);
                }
            } catch (e) {
                alert('购买失败，请重试');
            }
        }
        
        /**
         * 根据输入框数量购买精灵球
         */
        async function buyPokeBallWithInput(ballType) {
            const input = document.getElementById(`buyQty-${ballType}`);
            const quantity = parseInt(input.value);
            
            if (isNaN(quantity) || quantity < 1) {
                alert('请输入有效的购买数量！');
                return;
            }
            
            if (quantity > 99) {
                alert('单次购买数量不能超过99个！');
                return;
            }
            
            await buyPokeBall(ballType, quantity);
        }
        
        /**
         * 购买恢复道具
         */
        async function buyHealingItem(itemType) {
            const input = document.getElementById(`buyQty-${itemType}`);
            const quantity = parseInt(input.value);
            
            if (isNaN(quantity) || quantity < 1) {
                alert('请输入有效的购买数量！');
                return;
            }
            
            if (quantity > 99) {
                alert('单次购买数量不能超过99个！');
                return;
            }
            
            try {
                const resp = await fetch(`/game/shop/buy?itemType=${itemType}&quantity=${quantity}&category=HEALING`, {
                    method: 'POST'
                });
                const data = await resp.json();
                
                if (data.success) {
                    alert(data.message);
                    await refreshShopDisplay();
                    updateInventoryStatus();
                } else {
                    alert(data.message);
                }
            } catch (e) {
                alert('购买失败，请重试');
            }
        }
        
        // 批量贩卖精灵相关变量
        let selectedSellPokemons = [];
        
        function toggleSellSelection(source, id, name, rarity, price) {
            const key = `${source}-${id}`;
            const index = selectedSellPokemons.findIndex(p => p.key === key);
            
            if (index > -1) {
                // 取消选择
                selectedSellPokemons.splice(index, 1);
                document.getElementById(`sell-checkbox-${source}-${id}`).style.background = '#fff';
                document.getElementById(`sell-checkbox-${source}-${id}`).style.borderColor = '#ddd';
            } else {
                // 选择
                selectedSellPokemons.push({ key, source, id, name, rarity, price });
                document.getElementById(`sell-checkbox-${source}-${id}`).style.background = '#27ae60';
                document.getElementById(`sell-checkbox-${source}-${id}`).style.borderColor = '#27ae60';
            }
            
            updateBatchSellButton();
        }
        
        function updateBatchSellButton() {
            const btn = document.getElementById('batchSellButton');
            if (selectedSellPokemons.length === 0) {
                btn.style.display = 'none';
            } else {
                btn.style.display = 'block';
                const totalPrice = selectedSellPokemons.reduce((sum, p) => sum + p.price, 0);
                btn.innerHTML = `💰 批量贩卖 (${selectedSellPokemons.length}只) - 共${totalPrice}金币`;
            }
        }
        
        async function sellPokemonBatch() {
            if (selectedSellPokemons.length === 0) return;
            
            // 检查是否选择了背包中的所有精灵
            const backpackSelectedCount = selectedSellPokemons.filter(p => p.source === 'backpack').length;
            const totalBackpackCount = window.currentBackpackCount || 0;
            
            if (backpackSelectedCount > 0 && backpackSelectedCount >= totalBackpackCount) {
                alert('❌ 无法交易！至少需要保留一只精灵在背包中！');
                return;
            }
            
            const totalPrice = selectedSellPokemons.reduce((sum, p) => sum + p.price, 0);
            const names = selectedSellPokemons.map(p => p.name).join('、');
            
            if (!confirm(`确定要贩卖以下 ${selectedSellPokemons.length} 只精灵吗？\n${names}\n\n将获得 ${totalPrice} 金币！`)) {
                return;
            }
            
            let successCount = 0;
            let failCount = 0;
            
            for (const pokemon of selectedSellPokemons) {
                try {
                    const resp = await fetch(`/game/shop/sell?source=${pokemon.source}&id=${pokemon.id}`, {
                        method: 'POST'
                    });
                    const data = await resp.json();
                    
                    if (data.success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (e) {
                    failCount++;
                }
            }
            
            selectedSellPokemons = [];
            alert(`✅ 成功贩卖 ${successCount} 只精灵，失败 ${failCount} 只\n共获得 ${totalPrice} 金币！`);
            
            // 刷新显示
            showSellPokemonModal();
            await refreshShopDisplay();
            updateInventoryStatus();
            loadPokemons();
            // 刷新训练师对战和副本挑战按钮状态
            checkTrainerBattleEligibility();
        }

        // ========== 贩卖精灵 JS ==========
        
        const SELL_PRICES = {
            1: 50,
            2: 100,
            3: 150,
            4: 200,
            5: 250
        };
        
        async function showSellPokemonModal() {
            const modal = document.getElementById('sellPokemonModal');
            const content = document.getElementById('sellPokemonContent');
            modal.style.display = 'flex';
            
            // 清空之前的选择
            selectedSellPokemons = [];
            
            try {
                // 获取金币
                const goldResp = await fetch('/game/inventory');
                const goldData = await goldResp.json();
                document.getElementById('sellGoldDisplay').textContent = goldData.gold || 0;
                
                // 获取背包和仓库精灵
                const [backpackResp, storageResp] = await Promise.all([
                    fetch('/game/backpack'),
                    fetch('/game/storage')
                ]);
                
                const backpackItems = await backpackResp.json();
                const storageData = await storageResp.json();
                const storageItems = storageData.storage || [];
                
                // 记录背包精灵总数，用于验证至少保留一只
                window.currentBackpackCount = backpackItems.length;
                
                let html = '';
                
                // 批量贩卖按钮
                html += '<div id="batchSellButton" style="display: none; position: sticky; top: 0; z-index: 10; background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; padding: 15px; border-radius: 10px; text-align: center; margin-bottom: 15px; cursor: pointer; font-weight: bold; box-shadow: 0 4px 6px rgba(0,0,0,0.1);" onclick="sellPokemonBatch()">💰 批量贩卖</div>';
                
                // 背包精灵
                if (backpackItems.length > 0) {
                    html += '<h3 style="color: #667eea; margin: 15px 0 10px;">🎒 背包精灵 (点击选择)</h3>';
                    html += '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">';
                    backpackItems.forEach(item => {
                        const typeColor = getTypeColor(item.pokemonType);
                        const imagePath = `/images/pokemon/${item.pokemonName}.png`;
                        const rarity = item.rarity || 1;
                        const price = SELL_PRICES[rarity] || 50;
                        html += `
                            <div style="border: 2px solid #ddd; border-radius: 12px; padding: 12px; background: linear-gradient(145deg, #f9f9f9, #f0f0f0); text-align: center; cursor: pointer; position: relative;" onclick="toggleSellSelection('backpack', ${item.id}, '${item.pokemonName}', ${rarity}, ${price})">
                                <div id="sell-checkbox-backpack-${item.id}" style="position: absolute; top: 8px; left: 8px; width: 24px; height: 24px; border: 2px solid #ddd; border-radius: 50%; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; color: white; transition: all 0.3s;">✓</div>
                                <div style="width: 60px; height: 60px; margin: 0 auto 8px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 6px;">
                                    <img src="${imagePath}" alt="${item.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; font-size: 14px; margin-bottom: 3px;">${item.pokemonName} ${item.used ? '<span style="color: #27ae60;">[出战]</span>' : ''}</div>
                                <div style="font-size: 10px; margin-bottom: 5px;">${getRarityStars(rarity)}</div>
                                <div style="font-size: 11px; color: #666; margin-bottom: 8px;">${getTypeName(item.pokemonType)} | Lv.${item.level}</div>
                                <div style="background: linear-gradient(135deg, #fff9e6, #ffe4b5); border-radius: 8px; padding: 6px; margin-bottom: 8px;">
                                    <div style="font-size: 12px; color: #f39c12; font-weight: bold;">💰 ${price} 金币</div>
                                </div>
                                <button class="restart-btn" onclick="event.stopPropagation(); sellPokemon('backpack', ${item.id}, '${item.pokemonName}', ${price})" style="padding: 6px 15px; font-size: 12px; background: linear-gradient(135deg, #e74c3c, #c0392b);">💰 单独贩卖</button>
                            </div>`;
                    });
                    html += '</div>';
                }
                
                // 仓库精灵
                if (storageItems.length > 0) {
                    html += '<h3 style="color: #f093fb; margin: 20px 0 10px;">🏠 仓库精灵 (点击选择)</h3>';
                    html += '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">';
                    storageItems.forEach(item => {
                        const typeColor = getTypeColor(item.pokemonType);
                        const imagePath = `/images/pokemon/${item.pokemonName}.png`;
                        const rarity = item.rarity || 1;
                        const price = SELL_PRICES[rarity] || 50;
                        html += `
                            <div style="border: 2px solid #ddd; border-radius: 12px; padding: 12px; background: linear-gradient(145deg, #f9f9f9, #f0f0f0); text-align: center; cursor: pointer; position: relative;" onclick="toggleSellSelection('storage', ${item.id}, '${item.pokemonName}', ${rarity}, ${price})">
                                <div id="sell-checkbox-storage-${item.id}" style="position: absolute; top: 8px; left: 8px; width: 24px; height: 24px; border: 2px solid #ddd; border-radius: 50%; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; color: white; transition: all 0.3s;">✓</div>
                                <div style="width: 60px; height: 60px; margin: 0 auto 8px; background: linear-gradient(145deg, #f0f0f0, #e0e0e0); border-radius: 50%; padding: 6px;">
                                    <img src="${imagePath}" alt="${item.pokemonName}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none';">
                                </div>
                                <div style="font-weight: bold; color: ${typeColor}; font-size: 14px; margin-bottom: 3px;">${item.pokemonName}</div>
                                <div style="font-size: 10px; margin-bottom: 5px;">${getRarityStars(rarity)}</div>
                                <div style="font-size: 11px; color: #666; margin-bottom: 8px;">${getTypeName(item.pokemonType)} | Lv.${item.level}</div>
                                <div style="background: linear-gradient(135deg, #fff9e6, #ffe4b5); border-radius: 8px; padding: 6px; margin-bottom: 8px;">
                                    <div style="font-size: 12px; color: #f39c12; font-weight: bold;">💰 ${price} 金币</div>
                                </div>
                                <button class="restart-btn" onclick="event.stopPropagation(); sellPokemon('storage', ${item.id}, '${item.pokemonName}', ${price})" style="padding: 6px 15px; font-size: 12px; background: linear-gradient(135deg, #e74c3c, #c0392b);">💰 单独贩卖</button>
                            </div>`;
                    });
                    html += '</div>';
                }
                
                if (backpackItems.length === 0 && storageItems.length === 0) {
                    html = '<p style="text-align: center; color: #7f8c8d; padding: 40px;">背包和仓库中都没有精灵可贩卖</p>';
                }
                
                content.innerHTML = html;
            } catch (error) {
                content.innerHTML = '<p style="color: #e74c3c;">加载失败</p>';
            }
        }
        
        function closeSellPokemonModal() {
            document.getElementById('sellPokemonModal').style.display = 'none';
        }
        
        async function sellPokemon(source, id, name, price) {
            // 检查是否贩卖背包中最后一只精灵
            if (source === 'backpack' && window.currentBackpackCount <= 1) {
                alert('❌ 无法交易！至少需要保留一只精灵在背包中！');
                return;
            }
            
            if (!confirm(`确定要贩卖 ${name} 吗？\n将获得 ${price} 金币！`)) {
                return;
            }
            
            try {
                const resp = await fetch(`/game/shop/sell?source=${source}&id=${id}`, {
                    method: 'POST'
                });
                const data = await resp.json();
                
                if (data.success) {
                    alert(`✅ ${data.message}`);
                    // 刷新显示
                    showSellPokemonModal();
                    // 更新商店金币显示
                    await refreshShopDisplay();
                    updateInventoryStatus();
                    // 刷新主界面背包精灵
                    loadPokemons();
                    // 刷新训练师对战和副本挑战按钮状态
                    checkTrainerBattleEligibility();
                } else {
                    alert(`❌ ${data.message}`);
                }
            } catch (e) {
                alert('贩卖失败，请重试');
            }
        }

        // ========== 精灵球选择 JS ==========

        /**
         * 选择精灵球
         */
        function selectPokeBall(ballType) {
            selectedPokeBall = ballType;
            
            // 更新按钮样式
            document.querySelectorAll('.pokeball-btn').forEach(btn => {
                const type = btn.getAttribute('data-type');
                if (type === ballType) {
                    btn.style.border = '2px solid ' + getBallColor(type);
                    btn.style.background = '#fff9e6';
                } else {
                    btn.style.border = '2px solid #ddd';
                    btn.style.background = 'white';
                }
            });
            
            // 更新捕获率显示
            updateBattleRates();
        }

        /**
         * 获取精灵球颜色
         */
        function getBallColor(type) {
            const colors = {
                'BASIC': '#e74c3c',
                'MEDIUM': '#3498db',
                'ADVANCED': '#9b59b6',
                'MASTER': '#f1c40f'
            };
            return colors[type] || '#ddd';
        }

        /**
         * 刷新战斗界面精灵球数量显示
         */
        async function refreshBattleBallCounts() {
            try {
                const resp = await fetch('/game/inventory');
                const data = await resp.json();
                const pokeBalls = data.pokeBalls || {};
                
                document.getElementById('battleBasicCount').textContent = (pokeBalls.BASIC || 0) + '个';
                document.getElementById('battleMediumCount').textContent = (pokeBalls.MEDIUM || 0) + '个';
                document.getElementById('battleAdvancedCount').textContent = (pokeBalls.ADVANCED || 0) + '个';
                document.getElementById('battleMasterCount').textContent = (pokeBalls.MASTER || 0) + '个';
            } catch (e) {
                console.error('刷新精灵球数量失败:', e);
            }
        }

        // ========== 抽奖系统 JS ==========

        /**
         * 打开抽奖弹窗
         * 刷新当前金币显示，重置结果区和按钮状态
         */
        async function showLottery() {
            // 显示弹窗
            document.getElementById('lotteryModal').style.display = 'flex';
            // 重置结果区为默认提示状态
            resetLotteryResult();
            // 刷新金币显示
            await refreshLotteryGold();
        }

        /**
         * 关闭抽奖弹窗
         * 关闭后刷新主界面金币显示
         */
        function closeLotteryModal() {
            document.getElementById('lotteryModal').style.display = 'none';
            // 刷新主界面金币显示
            updateInventoryStatus();
        }

        /**
         * 刷新抽奖弹窗内的金币显示
         */
        async function refreshLotteryGold() {
            try {
                const resp = await fetch('/game/inventory/status');
                const data = await resp.json();
                // 更新弹窗内金币显示
                document.getElementById('lotteryGoldDisplay').textContent = data.gold;
            } catch (e) {
                console.error('获取金币失败', e);
            }
        }

        /**
         * 重置抽奖结果区为默认状态
         */
        function resetLotteryResult() {
            document.getElementById('lotteryDefaultHint').style.display = 'block';
            document.getElementById('lotteryResultContent').style.display = 'none';
            // 隐藏精灵奖励展示区
            document.getElementById('lotteryResultPokemon').style.display = 'none';
            document.getElementById('lotteryResultReward').style.display = 'block';
            // 恢复结果区背景
            const area = document.getElementById('lotteryResultArea');
            area.style.background = 'linear-gradient(135deg, #2d3436, #636e72)';
        }
        
        // ========== 成就系统 JS ==========
        
        /**
         * 显示成就弹窗
         */
        async function showAchievements() {
            const modal = document.getElementById('achievementModal');
            modal.style.display = 'flex';
            await loadAchievements();
        }
        
        /**
         * 关闭成就弹窗
         */
        function closeAchievementModal() {
            document.getElementById('achievementModal').style.display = 'none';
        }
        
        /**
         * 加载成就列表
         */
        async function loadAchievements() {
            try {
                const response = await fetch('/game/achievements');
                const data = await response.json();
                
                // 更新统计信息
                const stats = data.stats;
                document.getElementById('achievementCompleted').textContent = stats.completed;
                document.getElementById('achievementTotal').textContent = stats.total;
                document.getElementById('achievementProgressBar').style.width = stats.progress + '%';
                
                // 更新未领取奖励提示
                const unclaimedEl = document.getElementById('achievementUnclaimed');
                if (stats.unclaimed > 0) {
                    unclaimedEl.style.display = 'block';
                    document.getElementById('unclaimedCount').textContent = stats.unclaimed;
                } else {
                    unclaimedEl.style.display = 'none';
                }
                
                // 更新徽章
                updateAchievementBadge(stats.unclaimed);
                
                // 渲染成就列表
                renderAchievementList(data.achievements);
            } catch (error) {
                console.error('加载成就失败:', error);
            }
        }
        
        /**
         * 渲染成就列表
         */
        function renderAchievementList(achievements) {
            const listEl = document.getElementById('achievementList');
            listEl.innerHTML = '';
            
            achievements.forEach(achievement => {
                const isCompleted = achievement.completed;
                const isClaimed = achievement.rewardClaimed;
                
                let bgStyle, statusText, statusColor, buttonHtml;
                
                if (!isCompleted) {
                    // 未完成
                    bgStyle = 'background: linear-gradient(135deg, #f8f9fa, #e9ecef); opacity: 0.7;';
                    statusText = '未完成';
                    statusColor = '#95a5a6';
                    buttonHtml = '';
                } else if (isClaimed) {
                    // 已完成且已领取
                    bgStyle = 'background: linear-gradient(135deg, #d4edda, #c3e6cb);';
                    statusText = '✅ 已完成';
                    statusColor = '#27ae60';
                    buttonHtml = '<span style="color: #27ae60; font-weight: bold;">已领取</span>';
                } else {
                    // 已完成但未领取
                    bgStyle = 'background: linear-gradient(135deg, #fff3cd, #ffeeba); border: 2px solid #f1c40f;';
                    statusText = '🎁 可领取';
                    statusColor = '#f1c40f';
                    buttonHtml = `<button class="restart-btn" onclick="claimAchievementReward(${achievement.id})" style="padding: 8px 20px; font-size: 14px; background: linear-gradient(135deg, #f1c40f, #f39c12);">领取奖励</button>`;
                }
                
                // 构建奖励显示文本
                let rewardText = '';
                if (achievement.goldReward > 0) {
                    rewardText += `${achievement.goldReward}金币 `;
                }
                if (achievement.masterBallReward > 0) {
                    rewardText += `${achievement.masterBallReward}个大师球`;
                }
                
                const html = `
                    <div style="${bgStyle} border-radius: 12px; padding: 15px; display: flex; align-items: center; gap: 15px;">
                        <div style="font-size: 40px; flex-shrink: 0;">${achievement.icon}</div>
                        <div style="flex: 1;">
                            <div style="font-weight: bold; font-size: 16px; margin-bottom: 3px;">${achievement.name}</div>
                            <div style="font-size: 12px; color: #666; margin-bottom: 5px;">${achievement.description}</div>
                            <div style="font-size: 13px; color: #f39c12; font-weight: bold;">奖励: ${rewardText || '无'}</div>
                            <div style="font-size: 11px; color: ${statusColor}; margin-top: 3px;">${statusText}</div>
                        </div>
                        <div style="flex-shrink: 0;">
                            ${buttonHtml}
                        </div>
                    </div>
                `;
                
                listEl.innerHTML += html;
            });
        }
        
        /**
         * 领取成就奖励
         */
        async function claimAchievementReward(achievementId) {
            try {
                const response = await fetch(`/game/achievements/claim?achievementId=${achievementId}`, {
                    method: 'POST'
                });
                const data = await response.json();
                
                if (data.success) {
                    let message = `恭喜获得 ${data.achievementName} 奖励！`;
                    if (data.goldReward > 0) {
                        message += `\n💰 ${data.goldReward}金币`;
                    }
                    if (data.masterBallReward > 0) {
                        message += `\n🟡 ${data.masterBallReward}个大师球`;
                    }
                    alert(message);
                    
                    // 刷新成就列表
                    await loadAchievements();
                    // 刷新金币显示
                    updateInventoryStatus();
                } else {
                    alert(data.message);
                }
            } catch (error) {
                alert('领取奖励失败，请重试');
            }
        }
        
        /**
         * 更新成就徽章
         */
        function updateAchievementBadge(unclaimedCount) {
            const badge = document.getElementById('achievementBadge');
            if (unclaimedCount > 0) {
                badge.textContent = unclaimedCount;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        }
        
        /**
         * 检查新成就（可在战斗胜利、捕获成功等时机调用）
         */
        async function checkNewAchievements() {
            try {
                const response = await fetch('/game/achievements/check');
                const data = await response.json();
                
                if (data.count > 0) {
                    // 有新成就完成，显示提示
                    const names = data.newAchievements.map(a => a.name).join('、');
                    setTimeout(() => {
                        alert(`🎉 恭喜完成成就！\n${names}\n\n请前往成就系统领取奖励！`);
                    }, 500);
                    
                    // 更新徽章
                    updateAchievementBadge(data.count);
                }
            } catch (error) {
                console.error('检查成就失败:', error);
            }
        }

        /**
         * 执行抽奖逻辑
         * 1. 禁用按钮防止重复点击
         * 2. 调用后端 /game/lottery接口
         * 3. 根据奖项等级展示不同颜色的结果：
         *    - 金币奖励：显示获得金币数
         *    - 精灵奖励（二等/特等）：显示精灵名称和属性
         * 4. 更新弹窗内金币显示
         */
        async function doLottery() {
            const btn = document.getElementById('lotteryDrawBtn');
            // 禁用按钮，防止抽奖期间重复点击
            btn.disabled = true;
            btn.textContent = '抽奖中...';

            try {
                // 调用抽奖后端接口
                const resp = await fetch('/game/lottery', { method: 'POST' });
                const data = await resp.json();

                if (!data.success) {
                    // 金币不足等失败情况提示
                    alert(data.message);
                    btn.disabled = false;
                    btn.textContent = '🎰 抽奖（-100 金币）';
                    return;
                }

                // ====== 展示抽奖结果 ======
                const area   = document.getElementById('lotteryResultArea');
                const hint   = document.getElementById('lotteryDefaultHint');
                const result = document.getElementById('lotteryResultContent');
                const rewardEl  = document.getElementById('lotteryResultReward');
                const pokemonEl = document.getElementById('lotteryResultPokemon');

                // 根据奖项等级设置结果区背景颜色
                // -1=特等 0=安慰 1=一等 2=二等 3=三等 4=普通
                const gradients = {
                    '-1': 'linear-gradient(135deg, #fd79a8, #e84393)',   // 特等奖：红粉色
                      0 : 'linear-gradient(135deg, #636e72, #b2bec3)',   // 安慰奖：灰色
                      1 : 'linear-gradient(135deg, #f39c12, #f1c40f)',   // 一等奖：金色
                      2 : 'linear-gradient(135deg, #6c5ce7, #a29bfe)',   // 二等奖：紫色
                      3 : 'linear-gradient(135deg, #cd7f32, #e17055)',   // 三等奖：铜色
                      4 : 'linear-gradient(135deg, #7f8c8d, #95a5a6)'    // 普通奖：银色
                };
                area.style.background = gradients[data.prizeLevel] ?? gradients[0];

                // 填充结果内容
                document.getElementById('lotteryResultEmoji').textContent = data.emoji;
                document.getElementById('lotteryResultTitle').textContent = data.prizeTitle;

                if (data.prizePokemon) {
                    // 精灵奖励（二等奖或特等奖）：隐藏金币区，显示精灵信息
                    rewardEl.style.display  = 'none';
                    pokemonEl.style.display = 'block';
                    pokemonEl.textContent   = '🐾 获得精灵「' + data.prizePokemon + '」（' + (data.prizePokemonType || '') + '系）';
                } else {
                    // 金币奖励：隐藏精灵区，显示金币金额
                    pokemonEl.style.display = 'none';
                    rewardEl.style.display  = 'block';
                    rewardEl.textContent    = '获得 ' + data.reward + ' 金币';
                }

                // 隐藏默认提示，显示结果
                hint.style.display   = 'none';
                result.style.display = 'block';

                // 更新弹窗内金币显示
                document.getElementById('lotteryGoldDisplay').textContent = data.gold;

                // 抽中精灵奖励时刷新主界面背包/仓库计数
                if (data.prizePokemon) {
                    updateInventoryStatus();
                    // 同时刷新主界面的精灵选择区（如果当前在主界面）
                    loadPokemons();
                }

            } catch (e) {
                console.error('抽奖请求失败:', e);
                alert('抽奖失败，请重试');
            } finally {
                // 无论成功失败，恢复按钮
                btn.disabled = false;
                btn.textContent = '🎰 抽奖（-100 金币）';
            }
        }
    