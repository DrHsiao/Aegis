package com.aegis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;

public class Main {
    private static DisruptorManager disruptorManager;
    private static AIInferenceService aiInferenceService;
    private static IPBlacklistManager blacklistManager;

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.yaml");
                return;
            }

            AppConfig config = mapper.readValue(input, AppConfig.class);
            System.out.println("Config loaded successfully:");
            System.out.println(config);

            RuleManager ruleManager = new RuleManager();
            RuleLoader ruleLoader = new RuleLoader(ruleManager);
            ruleLoader.loadFromFile("rules.json");

            blacklistManager = new IPBlacklistManager();
            aiInferenceService = new AIInferenceService("model.onnx");
            disruptorManager = new DisruptorManager(ruleManager, aiInferenceService, blacklistManager);

            AegisServer server = new AegisServer(config, ruleManager, disruptorManager, blacklistManager);
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
