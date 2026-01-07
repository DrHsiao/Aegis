# Aegis WAF (神盾)

Aegis 是一款基於 Java Netty 開發的高性能 Agentic WAF (Web Application Firewall) 代理伺服器。它旨在為企業級應用（如 IIS, Tomcat）提供透明、高效且具備 AI 偵測能力的資安防護屏障。

## 🌟 核心特色

- **高性能異步架構**: 基於 **Netty** 與 **LMAX Disruptor**，實現 I/O 轉發與資安分析的完全解耦，確保防護邏輯絕不阻塞流量。
- **AI 智能偵測**: 整合 **Microsoft ONNX Runtime**，支援載入特徵工程後的深度學習模型進行即時威脅判定。
- **多維度過濾**:
    - **Aho-Corasick 高速匹配**: 針對 OWASP Top 10 關鍵字進行 O(n) 時間複雜度過濾。
    - **Regex 正則支援**: 精準識別複雜的攻擊模式。
- **協議深度兼容**:
    - 支援 **NTLM / Negotiate** 認證透傳（Connection Pinning）。
    - 支援 **WebSocket / SignalR** 自動升級與全透傳模式。
    - **HTTP/2** 支援與惡意 Stream 精準 RST 通訊。
- **自動化防禦**: 整合 **Guava Cache** 實現 IP 黑名單機制，當 AI 分數過高時自動封鎖惡意客端 5 分鐘。
- **高可用性方案**: 內建 `/health/live` 健康評鑑，並提供 Windows (PowerShell) 與 Linux (Bash) 的自動故障轉移 (Failover) 腳本。

## 🚀 快速開始

### 1. 編譯專案
使用 Maven 進行封裝：
```bash
mvn clean package
```

### 2. 啟動伺服器
```bash
java -jar target/aegis-proxy-1.0-SNAPSHOT.jar
```

### 3. 設定規則
編輯 `src/main/resources/rules.json` 以新增關鍵字或正則規則。

## 🛡️ 故障轉移 (Failover)

若 Aegis 服務意外中斷，可執行配套的 Watchdog 腳本：
- **Windows**: `watchdog.ps1` (使用 netsh portproxy)
- **Linux**: `watchdog.sh` (使用 iptables DNAT)

## 🛠️ 實作里程碑 (Implementation Milestones)

本專案從零開始，經歷了以下關鍵開發階段，最終形成完整的高性能防禦體系：

1.  **高效能代理核心與協議兼容**:
    - 建立基於 **Netty** 的雙向代理通道（Frontend & Backend）。
    - 實作 **NTLM/Negotiate 連線釘選 (Connection Pinning)**，確保企業級 Windows 認證在代理環境下依然穩定。
    - 支援 **HTTP/2** 流量處理與特定 Stream 的攻擊阻斷。
    - 實作 **WebSocket/SignalR** 的協議升級偵測與自動全透傳切換。

2.  **多維度安全規則引擎**:
    - 實作關鍵字過濾的 **Aho-Corasick** 演算法，實現單次掃描多模式識別。
    - 整合正規表達式 (Regex) 偵測，支援 Case-Insensitive 的複雜模式攻擊過濾。
    - 支援 `rules.json` 動態配置規則載入。

3.  **異步 AI 分析與效能優化**:
    - 引入 **LMAX Disruptor** 環形隊列，將流量數據採集與資安分析完全異步化，實現 Netty 執行緒零阻塞。
    - 實作 **Microsoft ONNX Runtime** 服務，將 Payload 特徵化後送入深度學習模型進行惡意行為判定。

4.  **自動化攔截與懲罰機制**:
    - 實作整合 **Guava Cache** 的 IP 黑名單管理器，支援 5 分鐘自動過期懲罰。
    - 建立 `SecurityHandler` 作為 Pipeline 的第一道防線，當 AI 分數超過閾值即自動封鎖發起源。

5.  **運維與部署自動化**:
    - 實作標準的 `/health/live` 健康評鑑端點。
    - 撰寫 Windows 專用安裝輔助腳本 (`install_aux.ps1`)，自動遷移 IIS 佔用的 Port 80。
    - 實作跨平台的 **Watchdog 監控腳本**：
        - Windows 版：利用 `netsh interface portproxy` 達成内核級流量撤回。
        - Linux 版：利用 `iptables DNAT` 實現故障即時跳線。
    - 提供 **jpackage** 打包指令，生成內含精簡 JRE 的原生 `Aegis-WAF.exe`。

## 📦 打包為原生執行檔
專案支援使用 `jpackage` 打包為免安裝的 `.exe` 檔，並內含精簡 JRE：
```powershell
# 參考指令見文件或開發記錄
jpackage --name "Aegis-WAF" ...
```

---
**Author**: John (Aegis Project Lead)
