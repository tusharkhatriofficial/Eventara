package com.eventara.rule.service;

import com.eventara.rule.exception.RuleCompilationException;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RuleCompilerService {

    private final KieServices kieServices;

    public RuleCompilerService() {
        this.kieServices = KieServices.Factory.get();
    }

    public void compileDrl(String drl) {
        log.debug("Compiling DRL: {}", drl);

        try {
            KieFileSystem kfs = kieServices.newKieFileSystem();
            kfs.write("src/main/resources/rules/temp.drl", drl);

            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                StringBuilder errors = new StringBuilder("DRL compilation errors:\n");
                for (Message message : kieBuilder.getResults().getMessages(Message.Level.ERROR)) {
                    errors.append("- ").append(message.getText()).append("\n");
                }
                throw new RuleCompilationException(errors.toString());
            }

            log.debug("DRL compiled successfully");

        } catch (RuleCompilationException e) {
            throw e;
        } catch (Exception e) {
            log.error("DRL compilation failed", e);
            throw new RuleCompilationException("Failed to compile DRL: " + e.getMessage(), e);
        }
    }
}
