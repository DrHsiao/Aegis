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

## 📦 打包為原生執行檔
專案支援使用 `jpackage` 打包為免安裝的 `.exe` 檔，並內含精簡 JRE：
```powershell
# 參考指令見文件或開發記錄
jpackage --name "Aegis-WAF" ...
```

---
**Author**: John (Aegis Project Lead)
