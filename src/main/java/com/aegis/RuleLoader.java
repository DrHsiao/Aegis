package com.aegis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RuleLoader {

    private final RuleManager ruleManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public RuleLoader(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    public void loadFromFile(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("Rule file not found: {}", resourcePath);
                return;
            }

            JsonNode root = mapper.readTree(is);

            // Load Keywords
            if (root.has("keywords")) {
                List<String> keywords = new ArrayList<>();
                root.get("keywords").forEach(node -> keywords.add(node.asText()));
                ruleManager.setKeywords(keywords);
                log.info("Loaded {} keywords from {}", keywords.size(), resourcePath);
            }

            // Load Regexes
            if (root.has("regexes")) {
                ruleManager.clearRegexes();
                root.get("regexes").forEach(node -> ruleManager.addRegex(node.asText()));
                log.info("Loaded regexes from {}", resourcePath);
            }

        } catch (Exception e) {
            log.error("Failed to load rules from {}", resourcePath, e);
        }
    }
}
