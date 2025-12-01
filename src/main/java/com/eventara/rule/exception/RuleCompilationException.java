package com.eventara.rule.exception;

import com.eventara.common.exception.EventaraException;

public class RuleCompilationException extends EventaraException {

    public RuleCompilationException(String message) {
        super(message);
    }

    public RuleCompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
