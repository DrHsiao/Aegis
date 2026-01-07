package com.aegis;

import com.lmax.disruptor.EventFactory;

public class TrafficEventFactory implements EventFactory<TrafficEvent> {
    @Override
    public TrafficEvent newInstance() {
        return new TrafficEvent();
    }
}
