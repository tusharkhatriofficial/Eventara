package com.eventara.rule.exception;

import com.eventara.common.exception.EventaraException;

public class InvalidRuleException extends EventaraException {

    public InvalidRuleException(String message) {
        super(message);
    }

    public InvalidRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
