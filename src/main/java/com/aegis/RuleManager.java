package com.aegis;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class RuleManager {

    private final Set<String> keywords = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> regexStrings = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<java.util.regex.Pattern> regexPatterns = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final AtomicReference<AhoCorasickMatcher> matcherRef = new AtomicReference<>();

    public RuleManager() {
        rebuild();
    }

    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            keywords.add(keyword);
            rebuild();
            log.info("Keyword added: '{}'. Total rules: {}", keyword, keywords.size());
        }
    }

    public void removeKeyword(String keyword) {
        if (keywords.remove(keyword)) {
            rebuild();
            log.info("Keyword removed: '{}'. Total rules: {}", keyword, keywords.size());
        }
    }

    public void setKeywords(Collection<String> newKeywords) {
        keywords.clear();
        if (newKeywords != null) {
            keywords.addAll(newKeywords);
        }
        rebuild();
        log.info("Keywords reset. Total rules: {}", keywords.size());
    }

    public void addRegex(String regex) {
        if (regex != null && !regex.isEmpty()) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex,
                        java.util.regex.Pattern.CASE_INSENSITIVE);
                if (regexStrings.add(regex)) {
                    regexPatterns.add(pattern);
                    log.info("Regex added: '{}'. Total regexes: {}", regex, regexPatterns.size());
                }
            } catch (Exception e) {
                log.error("Invalid regex: {}", regex, e);
            }
        }
    }

    public void clearRegexes() {
        regexStrings.clear();
        regexPatterns.clear();
    }

    private synchronized void rebuild() {
        matcherRef.set(new AhoCorasickMatcher(new HashSet<>(keywords)));
    }

    public List<AhoCorasickMatcher.MatchResult> match(String text) {
        AhoCorasickMatcher matcher = matcherRef.get();
        if (matcher == null || text == null) {
            return Collections.emptyList();
        }
        return matcher.match(text);
    }

    public boolean containsAny(String text) {
        if (text == null)
            return false;

        // Check keywords using AC
        if (!match(text).isEmpty())
            return true;

        // Check regexes
        for (java.util.regex.Pattern pattern : regexPatterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getKeywords() {
        return Collections.unmodifiableSet(keywords);
    }
}
