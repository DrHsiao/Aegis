package com.aegis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AppConfig {

    @JsonProperty("server")
    private ServerConfig server = new ServerConfig();

    @JsonProperty("upstream")
    private UpstreamConfig upstream = new UpstreamConfig();

    @JsonProperty("dns")
    private DnsConfig dns = new DnsConfig();

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("max_content_length")
    private String maxContentLength = "2MB";

    @Data
    @NoArgsConstructor
    public static class ServerConfig {
        @JsonProperty("port")
        private int port;

        @JsonProperty("ssl")
        private SslConfig ssl;
    }

    @Data
    @NoArgsConstructor
    public static class SslConfig {
        @JsonProperty("enabled")
        private boolean enabled;
        @JsonProperty("cert_path")
        private String certPath;
        @JsonProperty("cert_password")
        private String certPassword;
        @JsonProperty("cert_type")
        private String certType = "PKCS12";
    }

    @Data
    @NoArgsConstructor
    public static class DnsConfig {
        @JsonProperty("enabled")
        private boolean enabled;
        @JsonProperty("port")
        private int port = 53;
        @JsonProperty("records")
        private java.util.Map<String, String> records = new java.util.HashMap<>();
    }

    @Data
    @NoArgsConstructor
    public static class UpstreamConfig {
        @JsonProperty("host")
        private String host = "127.0.0.1";

        @JsonProperty("port")
        private int port;
    }
}
