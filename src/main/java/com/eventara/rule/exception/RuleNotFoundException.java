package com.eventara.rule.exception;

import com.eventara.common.exception.EventaraException;

public class RuleNotFoundException extends EventaraException {

    public RuleNotFoundException(String message) {
        super(message);
    }

    public RuleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
