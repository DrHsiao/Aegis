package com.aegis;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DisruptorManager {

    private final Disruptor<TrafficEvent> disruptor;
    private final RingBuffer<TrafficEvent> ringBuffer;

    public DisruptorManager(RuleManager ruleManager, AIInferenceService aiInferenceService,
            IPBlacklistManager blacklistManager) {
        TrafficEventFactory factory = new TrafficEventFactory();
        int bufferSize = 1024 * 8; // 必須是 2 的次方

        this.disruptor = new Disruptor<>(
                factory,
                bufferSize,
                new DefaultThreadFactory("ai-disruptor"),
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        this.disruptor.handleEventsWith(new TrafficEventHandler(ruleManager, aiInferenceService, blacklistManager));
        this.ringBuffer = disruptor.start();
    }

    public void publishEvent(String clientIp, String method, String uri, String payload) {
        long sequence = ringBuffer.next();
        try {
            TrafficEvent event = ringBuffer.get(sequence);
            event.setClientIp(clientIp);
            event.setMethod(method);
            event.setUri(uri);
            event.setPayload(payload);
            event.setTimestamp(System.currentTimeMillis());
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}
