package com.eventara.rule.entity;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;


@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Convert(attributeName = "jsonb", converter = JsonBinaryType.class)
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RuleStatus status = RuleStatus.ACTIVE;

    @Type(JsonBinaryType.class)
    @Column(name = "rule_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> ruleConfig;

    @Column(name = "generated_drl", columnDefinition = "TEXT", nullable = false)
    private String generatedDrl;

    @Column(name = "drl_hash")
    private String drlHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "notification_channels")
    private String[] notificationChannels;

    @Type(JsonBinaryType.class)
    @Column(name = "notification_config", columnDefinition = "jsonb")
    private Map<String, Object> notificationConfig;

    @Column(name = "suppression_window_minutes")
    @Builder.Default
    private Integer suppressionWindowMinutes = 30;

    @Column(name = "max_alerts_per_hour")
    @Builder.Default
    private Integer maxAlertsPerHour = 10;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    @Builder.Default
    private Integer triggerCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
