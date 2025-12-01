package com.eventara.rule.service;

import com.eventara.rule.dto.request.CreateRuleRequest;
import com.eventara.rule.dto.request.UpdateRuleRequest;
import com.eventara.rule.dto.request.TestRuleRequest;
import com.eventara.rule.dto.response.RuleResponse;
import com.eventara.rule.dto.response.RuleTestResult;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import com.eventara.rule.exception.InvalidRuleException;
import com.eventara.rule.exception.RuleNotFoundException;
import com.eventara.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final RuleValidationService validationService;
    private final DrlGeneratorService drlGeneratorService;
    private final RuleCompilerService compilerService;

    @Override
    public RuleResponse createRule(CreateRuleRequest request) {
        log.info("Creating new rule: {}", request.getName());

        // Validate rule doesn't exist
        if (ruleRepository.existsByName(request.getName())) {
            throw new InvalidRuleException("Rule with name '" + request.getName() + "' already exists");
        }

        // Validate rule configuration
        validationService.validateRuleConfig(request);

        // Generate DRL
        String drl = drlGeneratorService.generateDrl(request);

        // Compile and validate DRL
        compilerService.compileDrl(drl);

        // Calculate DRL hash
        String drlHash = calculateHash(drl);

        // Build entity
        AlertRule rule = AlertRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ruleType(request.getRuleType())
                .status(RuleStatus.DRAFT) // Start as draft
                .ruleConfig(request.getRuleConfig())
                .generatedDrl(drl)
                .drlHash(drlHash)
                .severity(request.getSeverity())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .notificationChannels(request.getNotificationChannels())
                .notificationConfig(request.getNotificationConfig())
                .suppressionWindowMinutes(request.getSuppressionWindowMinutes() != null
                        ? request.getSuppressionWindowMinutes() : 30)
                .maxAlertsPerHour(request.getMaxAlertsPerHour() != null
                        ? request.getMaxAlertsPerHour() : 10)
                .createdBy(request.getCreatedBy())
                .build();

        AlertRule saved = ruleRepository.save(rule);
        log.info("Rule created successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public RuleResponse updateRule(Long id, UpdateRuleRequest request) {
        log.info("Updating rule with ID: {}", id);

        AlertRule rule = getAlertRuleEntity(id);

        // Check if name is being changed and if it conflicts
        if (request.getName() != null && !request.getName().equals(rule.getName())) {
            if (ruleRepository.existsByName(request.getName())) {
                throw new InvalidRuleException("Rule with name '" + request.getName() + "' already exists");
            }
            rule.setName(request.getName());
        }

        // Update fields
        if (request.getDescription() != null) {
            rule.setDescription(request.getDescription());
        }

        if (request.getRuleConfig() != null) {
            validationService.validateRuleConfig(request);
            rule.setRuleConfig(request.getRuleConfig());

            // Regenerate DRL
            String newDrl = drlGeneratorService.generateDrl(request);
            compilerService.compileDrl(newDrl);

            rule.setGeneratedDrl(newDrl);
            rule.setDrlHash(calculateHash(newDrl));
            rule.setVersion(rule.getVersion() + 1);
        }

        if (request.getSeverity() != null) {
            rule.setSeverity(request.getSeverity());
        }

        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }

        if (request.getNotificationChannels() != null) {
            rule.setNotificationChannels(request.getNotificationChannels());
        }

        if (request.getNotificationConfig() != null) {
            rule.setNotificationConfig(request.getNotificationConfig());
        }

        AlertRule updated = ruleRepository.save(rule);
        log.info("Rule updated successfully: {}", id);

        return mapToResponse(updated);
    }

    @Override
    public void deleteRule(Long id) {
        log.info("Deleting rule with ID: {}", id);

        AlertRule rule = getAlertRuleEntity(id);
        ruleRepository.delete(rule);

        log.info("Rule deleted successfully: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RuleResponse getRuleById(Long id) {
        return mapToResponse(getAlertRuleEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RuleResponse enableRule(Long id) {
        log.info("Enabling rule with ID: {}", id);

        AlertRule rule = getAlertRuleEntity(id);
        rule.setStatus(RuleStatus.ACTIVE);

        AlertRule updated = ruleRepository.save(rule);
        log.info("Rule enabled successfully: {}", id);

        return mapToResponse(updated);
    }

    @Override
    public RuleResponse disableRule(Long id) {
        log.info("Disabling rule with ID: {}", id);

        AlertRule rule = getAlertRuleEntity(id);
        rule.setStatus(RuleStatus.INACTIVE);

        AlertRule updated = ruleRepository.save(rule);
        log.info("Rule disabled successfully: {}", id);

        return mapToResponse(updated);
    }

    @Override
    public RuleResponse archiveRule(Long id) {
        log.info("Archiving rule with ID: {}", id);

        AlertRule rule = getAlertRuleEntity(id);
        rule.setStatus(RuleStatus.ARCHIVED);

        AlertRule updated = ruleRepository.save(rule);
        log.info("Rule archived successfully: {}", id);

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getActiveRules() {
        return ruleRepository.findAllActiveRules().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getRulesByType(RuleType type) {
        return ruleRepository.findByRuleType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getRulesByStatus(RuleStatus status) {
        return ruleRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> searchRules(String searchTerm) {
        return ruleRepository.searchRules(searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RuleTestResult testRule(TestRuleRequest request) {
        log.info("Testing rule configuration");

        // Validate config
        validationService.validateRuleConfig(request);

        // Generate DRL
        String drl = drlGeneratorService.generateDrl(request);

        // Try to compile
        try {
            compilerService.compileDrl(drl);

            return RuleTestResult.builder()
                    .success(true)
                    .message("Rule configuration is valid")
                    .generatedDrl(drl)
                    .build();

        } catch (Exception e) {
            return RuleTestResult.builder()
                    .success(false)
                    .message("Rule compilation failed: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RuleTestResult testRuleById(Long id) {
        AlertRule rule = getAlertRuleEntity(id);

        try {
            compilerService.compileDrl(rule.getGeneratedDrl());

            return RuleTestResult.builder()
                    .success(true)
                    .message("Rule is valid and ready to execute")
                    .generatedDrl(rule.getGeneratedDrl())
                    .build();

        } catch (Exception e) {
            return RuleTestResult.builder()
                    .success(false)
                    .message("Rule validation failed: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalRulesCount() {
        return ruleRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveRulesCount() {
        return ruleRepository.countByStatus(RuleStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleResponse> getMostTriggeredRules() {
        return ruleRepository.findMostTriggeredRules().stream()
                .limit(10)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertRule getAlertRuleEntity(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRule> getAllActiveRuleEntities() {
        return ruleRepository.findAllActiveRules();
    }

    @Override
    public void updateLastTriggered(Long ruleId) {
        AlertRule rule = getAlertRuleEntity(ruleId);
        rule.setLastTriggeredAt(LocalDateTime.now());
        ruleRepository.save(rule);
    }

    @Override
    public void incrementTriggerCount(Long ruleId) {
        AlertRule rule = getAlertRuleEntity(ruleId);
        rule.setTriggerCount(rule.getTriggerCount() + 1);
        rule.setLastTriggeredAt(LocalDateTime.now());
        ruleRepository.save(rule);
    }

    // Helper methods
    private RuleResponse mapToResponse(AlertRule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .ruleType(rule.getRuleType())
                .status(rule.getStatus())
                .ruleConfig(rule.getRuleConfig())
                .severity(rule.getSeverity())
                .priority(rule.getPriority())
                .notificationChannels(rule.getNotificationChannels())
                .notificationConfig(rule.getNotificationConfig())
                .suppressionWindowMinutes(rule.getSuppressionWindowMinutes())
                .maxAlertsPerHour(rule.getMaxAlertsPerHour())
                .createdBy(rule.getCreatedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .lastTriggeredAt(rule.getLastTriggeredAt())
                .triggerCount(rule.getTriggerCount())
                .version(rule.getVersion())
                .build();
    }

    private String calculateHash(String drl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(drl.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating hash", e);
            return null;
        }
    }
}
