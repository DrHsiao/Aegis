# Aegis WAF 未來發展規劃 (TODO)

為了提升 Aegis 從核心代理走向企業級防火牆，以下是後續的開發重點方向：

## 1. 流量管理與限流 (Rate Limiting)
- [ ] 實作 **Token Bucket** 或 **Leaky Bucket** 演算法。
- [ ] 支援針對特定 IP、URI 或全局流量的請求限制（Return 429）。

## 2. 防資訊外洩機制 (DLP)
- [ ] 實作 **Response Content Inspection**。
- [ ] 針對敏感資訊（如信用卡號、身分證字號）進行正則偵測並自動遮罩 (Masking)。

## 3. 即時監控儀表板 (Dashboard)
- [ ] 整合 Netty 實作輕量級管理介面。
- [ ] 視覺化呈現 RPS, 延遲, 攔截率以及黑名單狀態。

## 4. 自動化證書管理 (ACME)
- [ ] 整合 **Let's Encrypt** ACME 客戶端。
- [ ] 支援 HTTPS 證書的自動申請與更新。

## 5. 企業級日誌集成 (SIEM Integration)
- [ ] 支援標準 **CEF (Common Event Format)** 格式。
- [ ] 支援將資安事件透過 Syslog 或 Webhook 拋轉至外部監控系統。

## 6. AI 特徵工程優化
- [ ] 實作更精細的 Payload 特徵化邏輯（如 TF-IDF 或 N-gram 轉換）。
- [ ] 支援動態更新 ONNX 模型檔案而無需重啟服務。
