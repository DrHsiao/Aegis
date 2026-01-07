package com.aegis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class IPBlacklistManager {

    private final Cache<String, Boolean> blacklist;

    public IPBlacklistManager() {
        // 建立一個 5 分鐘後自動過期的快取
        this.blacklist = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    public void blacklistIP(String ip) {
        if (ip != null) {
            blacklist.put(ip, Boolean.TRUE);
            log.warn("IP {} has been blacklisted for 5 minutes.", ip);
        }
    }

    public boolean isBlacklisted(String ip) {
        if (ip == null)
            return false;
        return blacklist.getIfPresent(ip) != null;
    }

    public void remove(String ip) {
        blacklist.invalidate(ip);
    }

    public long size() {
        return blacklist.size();
    }
}
