package com.eventara.drools.service;


import com.eventara.alert.service.AlertTriggerHandler;
import com.eventara.analytics.service.MetricsCalculator;
import com.eventara.drools.fact.MetricsFact;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
@RequiredArgsConstructor
public class RuleExecutionService {


    private final RuleRepository ruleRepository;
    private final AlertTriggerHandler alertTriggerHandler;
    private final MetricsCalculator metricsCalculator;


    private KieServices kieServices;
    private final ConcurrentHashMap<String, String> loadedRuleHashes = new ConcurrentHashMap<>();
    private KieContainer kieContainer;


    @PostConstruct
    public void init() {
        log.info("Initializing Rule Execution Service (real-time evaluation only, no scheduled execution)");
        this.kieServices = KieServices.Factory.get();
        loadAllActiveRules();
    }


    @PreDestroy
    public void shutdown() {
        if (kieContainer != null) {
            kieContainer.dispose();
        }
    }


    /**
     * Load all active rules from database into KIE container.
     * Used for rule testing functionality only.
     * 
     * NOTE: Scheduled time-window based evaluation has been REMOVED.
     * Threshold rules are now evaluated in REAL-TIME on every event
     * by RealTimeRuleEvaluator for immediate alerting.
     */
    public void loadAllActiveRules() {
        log.info("Loading all active rules for testing purposes");

        List<AlertRule> activeRules = ruleRepository.findByStatus(RuleStatus.ACTIVE);

        if (activeRules.isEmpty()) {
            log.warn("No active rules found");
            return;
        }

        // Update loaded rule hashes
        loadedRuleHashes.clear();
        for (AlertRule rule : activeRules) {
            loadedRuleHashes.put(rule.getName(), rule.getDrlHash());
        }

        // Dispose old KieContainer
        if (kieContainer != null) {
            kieContainer.dispose();
        }

        // Create unique release ID
        ReleaseId releaseId = kieServices.newReleaseId(
            "com.eventara", 
            "rules", 
            String.valueOf(System.currentTimeMillis())
        );

        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.generateAndWritePomXML(releaseId);

        for (AlertRule rule : activeRules) {
            if (rule.getGeneratedDrl() != null) {
                String resourcePath = "src/main/resources/rules/" + rule.getName() + ".drl";
                kfs.write(resourcePath, rule.getGeneratedDrl());
                log.debug("Loaded rule: {}", rule.getName());
            }
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            log.error("Error building rules: {}", kieBuilder.getResults().getMessages());
            throw new RuntimeException("Failed to build rules: " + kieBuilder.getResults().getMessages());
        }

        kieContainer = kieServices.newKieContainer(releaseId);
        log.info("Successfully loaded {} active rules", activeRules.size());
    }


    /**
     * Reload a single rule (when updated)
     */
    public void reloadRule(AlertRule rule) {
        log.info("Reloading rule: {}", rule.getName());


        // Check if rule hash changed
        String existingHash = loadedRuleHashes.get(rule.getName());
        if (rule.getDrlHash() != null && rule.getDrlHash().equals(existingHash)) {
            log.debug("Rule {} unchanged, skipping reload", rule.getName());
            return;
        }


        // Reload all rules
        loadAllActiveRules();
    }

    /**
     * Test a rule with sample metrics
     */
    public int testRule(String drl, MetricsFact testMetrics) {
        log.info("Testing rule with DRL");


        // Create unique release ID for test
        ReleaseId testReleaseId = kieServices.newReleaseId(
            "com.eventara", 
            "test-rule", 
            String.valueOf(System.currentTimeMillis())
        );


        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.generateAndWritePomXML(testReleaseId);
        kfs.write("src/main/resources/rules/test.drl", drl);


        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();


        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            log.error("Error compiling test rule: {}", kieBuilder.getResults().getMessages());
            throw new RuntimeException("Failed to compile test rule: " + kieBuilder.getResults().getMessages());
        }


        KieContainer testContainer = kieServices.newKieContainer(testReleaseId);
        KieSession kieSession = null;


        try {
            kieSession = testContainer.newKieSession();
            kieSession.setGlobal("alertHandler", alertTriggerHandler);
            kieSession.insert(testMetrics);
            kieSession.insert(alertTriggerHandler);


            int rulesFired = kieSession.fireAllRules();
            log.info("Test rule fired {} times", rulesFired);
            return rulesFired;


        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
            if (testContainer != null) {
                testContainer.dispose();
            }
        }
    }


    /**
     * Unload a rule (when deleted or deactivated)
     */
    public void unloadRule(String ruleName) {
        log.info("Unloading rule: {}", ruleName);
        loadedRuleHashes.remove(ruleName);
        loadAllActiveRules();
    }
}
