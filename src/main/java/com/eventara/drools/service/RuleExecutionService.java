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
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleExecutionService {

    private final RuleRepository ruleRepository;
    private final AlertTriggerHandler alertTriggerHandler;
    private final MetricsCalculator metricsCalculator;

    private KieServices kieServices;
    private KieContainer kieContainer;
    private final ConcurrentHashMap<String, String> loadedRuleHashes = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private final Map<Integer, List<AlertRule>> ruleGroups = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing Rule Execution Service");
        this.kieServices = KieServices.Factory.get();
        this.scheduler = Executors.newScheduledThreadPool(10);
        loadAllActiveRules();
        scheduleRuleGroups();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
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

        // Group rules by timeWindowMinutes
        ruleGroups.clear();
        for (AlertRule rule : activeRules) {
            int twm = getTimeWindowMinutes(rule);
            ruleGroups.computeIfAbsent(twm, k -> new ArrayList<>()).add(rule);
        }
    }

    private int getTimeWindowMinutes(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config != null && config.containsKey("timeWindowMinutes")) {
            try {
                return Integer.parseInt(config.get("timeWindowMinutes").toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid timeWindowMinutes for rule {}, defaulting to 5", rule.getName());
            }
        }
        return 5; // Default to 5 minutes
    }

    private void scheduleRuleGroups() {
        log.info("Scheduling rule evaluation groups");

        // Shutdown existing scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        // Create new scheduler
        scheduler = Executors.newScheduledThreadPool(Math.max(1, ruleGroups.size()));

        long currentTimeMillis = System.currentTimeMillis();
        long currentMinute = currentTimeMillis / 60000;

        for (Map.Entry<Integer, List<AlertRule>> entry : ruleGroups.entrySet()) {
            int twm = entry.getKey();
            List<AlertRule> rules = entry.getValue();

            // Calculate initial delay to align with clock
            long delayMinutes = twm - (currentMinute % twm);
            if (delayMinutes == twm) delayMinutes = 0;

            log.info("Scheduling group for timeWindowMinutes={} with {} rules, initial delay={} minutes",
                    twm, rules.size(), delayMinutes);

            scheduler.scheduleAtFixedRate(() -> evaluateRuleGroup(rules, twm),
                    delayMinutes, twm, TimeUnit.MINUTES);
        }
    }

    private void evaluateRuleGroup(List<AlertRule> rules, int twm) {
        log.debug("Evaluating rule group for timeWindowMinutes={}", twm);

        try {
            // Calculate metrics
            MetricsFact metrics = metricsCalculator.calculateCurrentMetrics();

            // Create KieSession and evaluate
            KieSession kieSession = null;
            try {
                kieSession = kieContainer.newKieSession();
                kieSession.setGlobal("alertHandler", alertTriggerHandler);
                kieSession.insert(metrics);
                kieSession.insert(alertTriggerHandler);

                int rulesFired = kieSession.fireAllRules();
                log.info("Evaluated {} rules for timeWindowMinutes={}, rules fired: {}", rules.size(), twm, rulesFired);

            } finally {
                if (kieSession != null) {
                    kieSession.dispose();
                }
            }

        } catch (Exception e) {
            log.error("Error evaluating rule group for timeWindowMinutes={}", twm, e);
        }
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
        scheduleRuleGroups();
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
        scheduleRuleGroups();
    }
}
