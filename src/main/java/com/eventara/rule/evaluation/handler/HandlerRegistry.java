package com.eventara.rule.evaluation.handler;

import com.eventara.rule.entity.AlertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registry that discovers and routes rules to appropriate handlers.
 * Handlers are auto-discovered via Spring's component scanning.
 */
@Component
@Slf4j
public class HandlerRegistry {

    private final List<RuleHandler> handlers;

    /**
     * Spring injects all RuleHandler implementations, sorted by priority.
     */
    public HandlerRegistry(List<RuleHandler> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(RuleHandler::getPriority))
                .toList();

        log.info("HandlerRegistry initialized with {} handlers: {}",
                handlers.size(),
                handlers.stream().map(h -> h.getClass().getSimpleName()).toList());
    }

    /**
     * Find the appropriate handler for a rule.
     * Returns the first handler (by priority) that can handle the rule.
     * 
     * @param rule The rule to find a handler for
     * @return Optional containing the handler, or empty if none can handle it
     */
    public Optional<RuleHandler> findHandler(AlertRule rule) {
        return handlers.stream()
                .filter(h -> h.canHandle(rule))
                .findFirst();
    }

    /**
     * Check if any handler can process the given rule.
     */
    public boolean hasHandler(AlertRule rule) {
        return handlers.stream().anyMatch(h -> h.canHandle(rule));
    }

    /**
     * Get the number of registered handlers.
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
