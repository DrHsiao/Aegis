package com.aegis;

import lombok.Data;
import io.netty.handler.codec.http.FullHttpRequest;

@Data
public class TrafficEvent {
    private String clientIp;
    private String method;
    private String uri;
    private String payload;
    private long timestamp;

    public void clear() {
        clientIp = null;
        method = null;
        uri = null;
        payload = null;
        timestamp = 0;
    }
}
