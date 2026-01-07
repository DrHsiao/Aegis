package com.aegis;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

public class SslContextFactory {

    public static SslContext createSslContext(AppConfig.SslConfig sslConfig) throws Exception {
        if (sslConfig == null || !sslConfig.isEnabled()) {
            return null;
        }

        KeyStore ks = KeyStore.getInstance(sslConfig.getCertType());
        try (InputStream is = new FileInputStream(sslConfig.getCertPath())) {
            ks.load(is, sslConfig.getCertPassword().toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, sslConfig.getCertPassword().toCharArray());

        return SslContextBuilder.forServer(kmf)
                .sslProvider(SslProvider.OPENSSL) // Uses Netty-tcnative (BoringSSL)
                .ciphers(null, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        // NO_ADVERTISE: selector does not advertise protocols
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT: listener accepts connection even if no protocol match
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
    }
}
