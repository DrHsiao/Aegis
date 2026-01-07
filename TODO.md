# Aegis WAF 未來發展規劃 (TODO)

為了提升 Aegis 從核心代理走向企業級防火牆，以下是後續的開發重點方向：

## 1. 流量管理與限流 (Rate Limiting)
- [ ] 實作 **Token Bucket** 或 **Leaky Bucket** 演算法。
- [ ] 支援針對特定 IP、URI 或全局流量的請求限制（Return 429）。

## 2. 防資訊外洩機制 (DLP)
- [ ] 實作 **Response Content Inspection**。
- [ ] 針對敏感資訊（如信用卡號、身分證字號）進行正則偵測並自動遮罩 (Masking)。

## 3. Web 管理面板與視覺化儀表板 (Web UI / Dashboard)
這將是提升用戶體驗的核心項目，計畫採用現代化的單頁應用 (SPA) 架構：
- [ ] **即時狀態監控**:
    - [ ] 實作 **WebSocket 推播系統**，將最新的流量指標 (RPS, Latency) 即時推送至前端。
    - [ ] 使用 **ECharts** 或 **Chart.js** 繪製攻擊趨勢圖表。
- [ ] **威脅地圖 (Threat Map)**: 透過 IP 地理位置解析，視覺化呈現攻擊來源地。
- [ ] **線上規則編輯器**:
    - [ ] 支援在 Web UI 上直接新增/修改 `rules.json` 中的關鍵字與正則規則，並實作 **Hot-Reload** 無感更新。
- [ ] **黑名單管理介面**:
    - [ ] 提供一鍵式解封 (Unban) 功能，並顯示 IP 封鎖剩餘時間。
- [ ] **日誌檢索系統**:
    - [ ] 整合簡易的搜尋功能，可過濾特定時間、IP 或攻擊類型的日誌記錄。

## 4. 自動化證書管理 (ACME)
- [ ] 整合 **Let's Encrypt** ACME 客戶端。
- [ ] 支援 HTTPS 證書的自動申請與更新。

## 5. 企業級日誌集成 (SIEM Integration)
- [ ] 支援標準 **CEF (Common Event Format)** 格式。
- [ ] 支援將資安事件透過 Syslog 或 Webhook 拋轉至外部監控系統。

## 6. AI 特徵工程優化
- [ ] 實作更精細的 Payload 特徵化邏輯（如 TF-IDF 或 N-gram 轉換）。
- [ ] 支援動態更新 ONNX 模型檔案而無需重啟服務。
