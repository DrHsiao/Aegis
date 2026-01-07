package com.aegis;

import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class AIInferenceService {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final boolean enabled;

    public AIInferenceService(String modelPath) {
        this.env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();

        OrtSession tempSession = null;
        boolean isEnabled = false;
        try {
            tempSession = env.createSession(modelPath, options);
            isEnabled = true;
            log.info("AI Model loaded successfully from {}", modelPath);
        } catch (OrtException e) {
            log.error("Failed to load AI model from {}. AI inference will be disabled.", modelPath, e);
        }

        this.session = tempSession;
        this.enabled = isEnabled;
    }

    /**
     * 執行推論。此處假設模型輸入為固定長度的浮點數向量（例如特徵化後的 Payload）。
     * 在實際應用中，需根據模型的輸入層定義（Input Metadata）進行調整。
     */
    public float infer(float[] inputData) {
        if (!enabled || session == null) {
            return 0.0f;
        }

        try {
            // 建立輸入 Tensor (假設名為 "float_input")
            // 這裡假設模型輸入維度為 [1, inputData.length]
            long[] shape = new long[] { 1, inputData.length };
            OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape);

            try (OrtSession.Result results = session
                    .run(Collections.singletonMap(session.getInputNames().iterator().next(), tensor))) {
                // 獲取第一個輸出值 (假設是攻擊機率 0.0 ~ 1.0)
                float[][] output = (float[][]) results.get(0).getValue();
                return output[0][0];
            } finally {
                tensor.close();
            }
        } catch (OrtException e) {
            log.error("AI inference error", e);
            return 0.0f;
        }
    }

    public void close() {
        try {
            if (session != null)
                session.close();
            if (env != null)
                env.close();
        } catch (OrtException e) {
            log.error("Error closing ONNX runtime", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
