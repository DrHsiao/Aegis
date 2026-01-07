package com.aegis;

import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrafficEventHandler implements EventHandler<TrafficEvent> {

    private final RuleManager ruleManager;
    private final AIInferenceService aiInferenceService;
    private final IPBlacklistManager blacklistManager;

    public TrafficEventHandler(RuleManager ruleManager, AIInferenceService aiInferenceService,
            IPBlacklistManager blacklistManager) {
        this.ruleManager = ruleManager;
        this.aiInferenceService = aiInferenceService;
        this.blacklistManager = blacklistManager;
    }

    @Override
    public void onEvent(TrafficEvent event, long sequence, boolean endOfBatch) {
        try {
            // 模擬 AI Thread 處理分析 (不阻塞 Netty)
            if (event.getPayload() != null && ruleManager.containsAny(event.getPayload())) {
                log.warn("[AI-THREAD] Potential threat detected by RULES from {}: {} {}",
                        event.getClientIp(), event.getMethod(), event.getUri());
            }

            // AI 異步推論
            if (aiInferenceService != null && aiInferenceService.isEnabled() && event.getPayload() != null) {
                // 模擬特徵化：在實際場景中需將 Payload 轉換為 Float Array
                float[] mockFeatures = new float[128]; // 假設模型輸入長度
                float score = aiInferenceService.infer(mockFeatures);
                if (score > 0.85f) {
                    log.warn("[AI-THREAD] Potential threat detected by AI (Score: {}) from {}", score,
                            event.getClientIp());
                    blacklistManager.blacklistIP(event.getClientIp());
                }
            }

            // 可以在此執行異步日誌、資料庫寫入或 AI 模型推論

        } catch (Exception e) {
            log.error("Error processing traffic event", e);
        } finally {
            event.clear(); // 清理核心物件以利 RingBuffer 復用
        }
    }
}
