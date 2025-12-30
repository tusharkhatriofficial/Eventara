package com.eventara.drools.service;

import com.eventara.alert.service.AlertTriggerHandler;
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
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleExecutionService {

    private final RuleRepository ruleRepository;
    private final AlertTriggerHandler alertTriggerHandler;

    private KieServices kieServices;
    private KieContainer kieContainer;
    private final ConcurrentHashMap<String, String> loadedRuleHashes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing Rule Execution Service");
        this.kieServices = KieServices.Factory.get();
        loadAllActiveRules();
    }

    /**
     * Load all active rules from database into KIE container
     */
    public void loadAllActiveRules() {
        log.info("Loading all active rules");

        List<AlertRule> activeRules = ruleRepository.findByStatus(RuleStatus.ACTIVE);

        if (activeRules.isEmpty()) {
            log.warn("No active rules found");
            return;
        }

        KieFileSystem kfs = kieServices.newKieFileSystem();

        for (AlertRule rule : activeRules) {
            String resourcePath = "src/main/resources/rules/" + rule.getName() + ".drl";
            kfs.write(resourcePath, rule.getGeneratedDrl());
            loadedRuleHashes.put(rule.getName(), rule.getDrlHash());
            log.debug("Loaded rule: {}", rule.getName());
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            log.error("Error building rules: {}", kieBuilder.getResults().getMessages());
            throw new RuntimeException("Failed to build rules: " + kieBuilder.getResults().getMessages());
        }

        kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        log.info("Successfully loaded {} active rules", activeRules.size());
    }

    /**
     * Reload a single rule (when updated)
     */
    public void reloadRule(AlertRule rule) {
        log.info("Reloading rule: {}", rule.getName());

        // Check if rule hash changed
        String existingHash = loadedRuleHashes.get(rule.getName());
        if (rule.getDrlHash().equals(existingHash)) {
            log.debug("Rule {} unchanged, skipping reload", rule.getName());
            return;
        }

        // Reload all rules (simpler for now, can optimize later)
        loadAllActiveRules();
    }

    /**
     * Execute all active rules against metrics
     */
    @Async
    public void executeRules(MetricsFact metrics) {
        if (kieContainer == null) {
            log.warn("KIE container not initialized, loading rules");
            loadAllActiveRules();
            if (kieContainer == null) {
                log.error("Failed to initialize KIE container");
                return;
            }
        }

        KieSession kieSession = null;
        try {
            kieSession = kieContainer.newKieSession();

            // Insert facts
            kieSession.setGlobal("alertHandler", alertTriggerHandler);
            kieSession.insert(metrics);
            kieSession.insert(alertTriggerHandler);

            // Fire all rules
            int rulesFired = kieSession.fireAllRules();
            log.info("Executed rules against metrics. Rules fired: {}", rulesFired);

        } catch (Exception e) {
            log.error("Error executing rules", e);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }
    }

    /**
     * Test a rule with sample metrics
     */
    public int testRule(String drl, MetricsFact testMetrics) {
        log.info("Testing rule with DRL");

        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write("src/main/resources/rules/test.drl", drl);

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            log.error("Error compiling test rule: {}", kieBuilder.getResults().getMessages());
            throw new RuntimeException("Failed to compile test rule: " + kieBuilder.getResults().getMessages());
        }

        KieContainer testContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
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
